/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
 * https://www.exactpro.com
 * Build Software to Test Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.exactprosystems.clearth.automation.report.json;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.report.AutomationReport;
import com.exactprosystems.clearth.automation.report.StepReport;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.utils.JsonMarshaller;
import com.exactprosystems.clearth.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.automation.report.ActionReportWriter.JSON_SUFFIX;
import static com.exactprosystems.clearth.utils.FileOperationUtils.newBlockingBufferedReader;
import static com.exactprosystems.clearth.utils.FileOperationUtils.newBlockingBufferedWriter;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.repeat;

public class JsonReport
{
	private static final Logger logger = LoggerFactory.getLogger(JsonReport.class);

	private static final Pattern STEP_REPORTS_PATTERN = Pattern.compile("\"stepReports\"\\s*:\\s*\\[");
	private static final Pattern ACTION_REPORTS_PATTERN = Pattern.compile("\"actionReports\"\\s*:\\s*(\\[\\s*])");
	private static final int STEP_REPORTS_INDENT_SIZE = 2;
	private static final String ACTION_REPORTS_INDENT = repeat(' ', 4);
	
	protected final Matrix matrix;
	protected final Collection<Step> allSteps;
	protected final Set<String> matrixStepNames;
	
	protected final String userName;
	protected final Date startTime;
	protected final Date endTime;
	protected final HandledTestExecutionId matrixExecutionId;
	
	public JsonReport(Matrix matrix, Collection<Step> allSteps, Set<String> matrixStepNames,
	                  String userName, Date startTime, Date endTime, HandledTestExecutionId matrixExecutionId)
	{
		this.matrix = matrix;
		this.allSteps = allSteps;
		this.matrixStepNames = matrixStepNames;
		this.userName = userName;
		this.startTime = startTime;
		this.endTime = endTime;
		this.matrixExecutionId = matrixExecutionId;
	}


	protected AutomationReport createAutomationReport()
	{
		return new AutomationReport();
	}

	/* Override this method to add project-specific fields to automation JSON report. */
	protected void setAutomationReportCustomFields(AutomationReport report, Matrix matrix)
	{ /*Nothing to set by default*/ }

	protected StepReport createStepReport(Step step, Matrix matrix)
	{
		return new StepReport(step, matrix);
	}


	public void writeReport(Path reportPath, Path actionsReportsDir) throws IOException
	{
		logger.debug("Writing JSON report for matrix '{}'...", matrix.getName());

		AutomationReport automationReport = createAutomationReport();
		setAutomationReportFields(automationReport, matrix, userName, startTime, endTime, matrixExecutionId);
		setAutomationReportCustomFields(automationReport, matrix);
		String automationReportJson = toJson(automationReport);

		try (BufferedWriter reportWriter = newBlockingBufferedWriter(reportPath))
		{
			if (isEmpty(matrixStepNames))
				reportWriter.write(automationReportJson);
			else
			{
				Matcher m = STEP_REPORTS_PATTERN.matcher(automationReportJson);
				if (m.find())
				{
					reportWriter.write(automationReportJson.substring(0, m.end()));
					buildAndWriteStepJsonReports(reportWriter, actionsReportsDir);
					reportWriter.write(automationReportJson.substring(m.end()));
				}
				else
				{
					logger.error("Unable to add steps to JSON report for matrix '{}': pattern '{}' not found.",
							matrix.getName(), STEP_REPORTS_PATTERN.pattern());
					reportWriter.write(automationReportJson);
				}
			}
		}
	}
	
	private void setAutomationReportFields(AutomationReport report, Matrix matrix, String userName,
	                                       Date startTime, Date endTime, HandledTestExecutionId matrixExecutionId)
	{
		report.setReportName(matrix.getName());
		report.setMatrixName(matrix.getName());
		report.setUserName(userName);
		report.setHost(Utils.host());
		report.setVersion(ClearThCore.getInstance().getVersion().getBuildNumber());
		report.setExecutionStart(startTime);
		report.setExecutionEnd(endTime);
		report.setExecutionTime(startTime, endTime);
		report.setResult(matrix.isSuccessful());
		report.setDescription(matrix.getDescription());
		report.setConstants(matrix.getConstants());
		report.setHandledTestExecutionId(getStringMatrixExecutionId(matrixExecutionId));
	}
	
	private void buildAndWriteStepJsonReports(BufferedWriter reportWriter, Path actionsReportsDir) throws IOException
	{
		boolean firstStep = true;
		for (Step step : allSteps)
		{
			if (matrixStepNames.contains(step.getSafeName() + ".json"))
			{
				if (firstStep)
					firstStep = false;
				else
					reportWriter.write(',');

				buildAndWriteStepJsonReport(reportWriter, step, actionsReportsDir, matrix);
			}
		}
	}
	
	private void buildAndWriteStepJsonReport(BufferedWriter reportWriter, Step step, Path actionsReportsDir,
	                                         Matrix matrix) throws IOException
	{
		StepReport stepReport = createStepReport(step, matrix);
		String stepReportJson = toJson(stepReport);
		stepReportJson = addIndentToJson(stepReportJson, STEP_REPORTS_INDENT_SIZE);

		Matcher m = ACTION_REPORTS_PATTERN.matcher(stepReportJson);
		if (m.find())
		{
			reportWriter.write(stepReportJson.substring(0, m.start(1)));

			Path storedActionsReportPath = actionsReportsDir.resolve(step.getSafeName() + JSON_SUFFIX);
			appendActionsReport(reportWriter, storedActionsReportPath);

			reportWriter.write(stepReportJson.substring(m.end(1)));
		}
		else
		{
			logger.error("Unable to add actions to JSON report for step '{}' of matrix '{}': pattern '{}' not found.",
					step.getSafeName(), matrix.getName(), ACTION_REPORTS_PATTERN.pattern());
			reportWriter.write(stepReportJson);
		}
	}
	
	private void appendActionsReport(BufferedWriter reportWriter, Path storedActionsReportPath) throws IOException
	{
		try (BufferedReader actionsReportReader = newBlockingBufferedReader(storedActionsReportPath))
		{
			String line;
			while ((line = actionsReportReader.readLine()) != null)
			{
				reportWriter.write(ACTION_REPORTS_INDENT);
				reportWriter.write(line);
				reportWriter.newLine();
			}
		}
	}
	
	
	protected String getStringMatrixExecutionId(HandledTestExecutionId matrixExecutionId)
	{
		if (matrixExecutionId == null)
			return null;
		return matrixExecutionId.toString();
	}
	
	protected <T> String toJson(T object) throws IOException
	{
		JsonMarshaller<T> marshaller = new JsonMarshaller<>();
		return marshaller.marshal(object);
	}
	
	private String addIndentToJson(String json, int indentSize)
	{
		return json.replaceAll("(?<=\r?\n)", repeat(' ', indentSize));
	}
}
