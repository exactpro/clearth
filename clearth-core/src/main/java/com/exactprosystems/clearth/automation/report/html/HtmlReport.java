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

package com.exactprosystems.clearth.automation.report.html;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.report.ActionReportWriter;
import com.exactprosystems.clearth.automation.report.ReportException;
import com.exactprosystems.clearth.automation.report.ReportStatus;
import com.exactprosystems.clearth.automation.report.html.template.*;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.utils.DateTimeUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import freemarker.template.TemplateException;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class HtmlReport
{
	private static final Logger log = LoggerFactory.getLogger(HtmlReport.class);
	
	protected static final String pathToResourceFiles;
	protected static final String[] resourceFiles = {"logo.gif", "show.gif", "hide.gif", "right.gif", "close.gif"};
	protected static final String APP_LOGO = "app_logo.gif";
	protected static final String APP_LOGO_NY = "app_logo_ny.gif";

	protected String pathToFiles;
	protected String reportFilePath;

	protected String userName, scriptName, execStart, execTime;
	protected boolean testPassed;
	protected Matrix matrix;
	protected HandledTestExecutionId matrixExecutionId;
	protected String testHandlerName;
	protected ReportTemplatesProcessor templatesProcessor;
	
	static
	{
		pathToResourceFiles = ClearThCore.getInstance().getOriginalReportsFilePath();
	}
	
	public HtmlReport(){}
	
	public HtmlReport(Matrix matrix, String pathToReport, String userName, String reportName, Date startTime,
			Date endTime, String testHandlerName, HandledTestExecutionId matrixExecutionId) throws IOException
	{
		this(matrix, pathToReport, userName, reportName, startTime, endTime, testHandlerName, matrixExecutionId,
				ClearThCore.getInstance().getReportTemplatesProcessor());
	}
	public HtmlReport(Matrix matrix, String pathToReport, String userName, String reportName, Date startTime,
			Date endTime, String testHandlerName, HandledTestExecutionId matrixExecutionId,
			ReportTemplatesProcessor templatesProcessor) throws IOException
	{
		this.matrix = matrix;

		this.userName = userName;
		this.scriptName = matrix.getName();
		this.testPassed = matrix.isSuccessful();
		this.matrixExecutionId = matrixExecutionId;
		this.testHandlerName = testHandlerName;
		this.templatesProcessor = templatesProcessor;
		
		if (startTime!=null)
			execStart = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(startTime);
		else
			execStart = "";
		
		if ((endTime!=null) && (startTime!=null))
			execTime = Double.toString((endTime.getTime()-startTime.getTime())/1000.0)+" sec";
		else
			execTime = "";
		
		File report = new File(pathToReport, matrix.getShortFileName());
		report.mkdirs();
		pathToFiles = report.getCanonicalPath()+"/";
		reportFilePath = pathToFiles + reportName + ActionReportWriter.HTML_SUFFIX;
	}
	
	protected String getRevisionData()
	{
		return ClearThCore.getInstance().getVersion().getBuildNumber();
	}

	protected boolean isStepStatusExpanded(ReportStatus status)
	{
		return false;
	}

	protected Logger getLogger()
	{
		return log;
	}

	public void writeReport(List<Step> allSteps, List<String> matrixSteps, File actionsReportsDir, boolean onlyFailed) throws IOException, ReportException
	{
		copyReportFiles();
		Map<String, Object> parameters = initTemplateParameters(allSteps, matrixSteps, actionsReportsDir, onlyFailed);

		try (Writer writer = new BufferedWriter(new FileWriter(reportFilePath)))
		{
			templatesProcessor.processTemplate(writer, parameters, ReportTemplateFiles.REPORT);
		}
		catch (TemplateException e)
		{
			String errMsg = "An error occurred while processing the template of the report.";
			getLogger().error(errMsg, e);
			throw new ReportException(errMsg + " Please check logs for details");
		}

		writeAdditionalInfo(parameters);
	}

	protected void writeAdditionalInfo(Map<String, Object> parameters)	throws IOException, ReportException
	{
		// Override to write addition information
	}

	protected void copyReportFiles() throws IOException
	{
		String logoFile = getAppLogoFile();

		for (String file : getResourceFilesNames())
		{
			FileOperationUtils.copyFile(file, pathToResourceFiles, pathToFiles);
		}

		FileOperationUtils.copyFile(pathToResourceFiles + logoFile, pathToFiles + APP_LOGO);
	}

	protected String getAppLogoFile()
	{
		return (DateTimeUtils.isNewYear() && newYearLogoExists()) ? APP_LOGO_NY : APP_LOGO;
	}
	
	private boolean newYearLogoExists()
	{
		return new File(pathToResourceFiles + APP_LOGO_NY).exists();
	}
	
	protected Map<String, Object> initTemplateParameters(List<Step> allSteps, List<String> matrixSteps, File actionsReportsDir, boolean onlyFailed)
	{
		Map<String, Object> parameters = new HashMap<String, Object>();

		parameters.put("userName", userName);
		parameters.put("scriptName", scriptName);
		parameters.put("execStart", execStart);
		parameters.put("execTime", execTime);
		parameters.put("testPassed", testPassed);
		parameters.put("handledTestExecutionId", matrixExecutionId);
		parameters.put("handlerName", testHandlerName);
		parameters.put("description", matrix.getDescription());
		parameters.put("constants", matrix.getConstants());
		parameters.put("formulas", matrix.getFormulas());

		parameters.put("pathToStyles", pathToResourceFiles + "style.css");
		parameters.put("pathToJS", pathToResourceFiles + "script.js");

		parameters.put("revision", getRevisionData());

		parameters.put("host", Utils.host());

		parameters.put("stepsData", createStepsData(allSteps, matrixSteps, actionsReportsDir, onlyFailed));
		
		Map<String, Object> additionalParams = ClearThCore.getInstance().getAdditionalTemplateParams(); 
		if (additionalParams != null && !additionalParams.isEmpty())
			parameters.putAll(additionalParams);
		
		addExtraTemplateParameters(parameters);
		
		return parameters;
	}

	protected void addExtraTemplateParameters(Map<String, Object> parameters)
	{
	}

	protected List<StepData> createStepsData(List<Step> allSteps, List<String> matrixSteps, File actionsReportsDir, boolean onlyFailed)
	{
		if (matrixSteps == null || matrixSteps.isEmpty())
		{
			return Collections.emptyList();
		}
		else
		{
			List<StepData> stepsData = new ArrayList<StepData>(matrixSteps.size());
			for (Step step : allSteps)
			{
				if (!step.isExecute() || step.getFinished() == null && step.getExecutionProgress().getDone() == 0)
					continue;
				String stepName = step.getName();
				String stepFileName = !onlyFailed ? step.getSafeName() : (step.getSafeName() + "_failed");
				if (matrixSteps.contains(stepFileName))
				{
					if (!onlyFailed || step.isFailedDueToError() || !matrix.isStepSuccessful(stepName))
					{
						File stepActionsFile = new File(actionsReportsDir, matrix.getShortFileName() + File.separator + stepFileName);
						ReportStatus status = createStepStatus(step, matrix);
						stepsData.add(createStepData(step, status, stepActionsFile));
					}
				}
			}
			return stepsData;
		}
	}
	
	protected StepData createStepData(Step step, ReportStatus status, File stepActionsFile)
	{
		return new StepData(step, status, stepActionsFile.exists() ? stepActionsFile.getAbsolutePath() : null, 
				isStepStatusExpanded(status), htmlValidName(step.getName()));
	}
	
	/**
	 * Making name correct displayed on html-page
	 * @param name
	 * @return
	 */
	public String htmlValidName(String name)
	{
		return StringEscapeUtils.escapeHtml(name);
	}

	protected ReportStatus createStepStatus(Step step, Matrix matrix)
	{
		boolean success = !step.isFailedDueToError() && matrix.isStepSuccessful(step.getName());
		List<String> comments = new ArrayList<String>(),
				matrixComments = matrix.getStepStatusComments(step.getName());
		if (matrixComments!=null)
			comments.addAll(matrixComments);
		if (step.getStatusComment()!=null)
			comments.add(step.getStatusComment());

		ReportStatus stepStatus = new ReportStatus(success, comments, step.getError());
		stepStatus.setStarted(step.getStarted());
		stepStatus.setFinished(step.getFinished());
		return stepStatus;
	}

	protected String[] getResourceFilesNames()
	{
		return resourceFiles;
	}
}
