/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Executor;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.report.html.HtmlReport;
import com.exactprosystems.clearth.utils.JsonMarshaller;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.automation.report.ActionReportWriter.JSON_SUFFIX;

public class ReportsWriter
{
	private static final Logger logger = LoggerFactory.getLogger(ReportsWriter.class);
	
	private final Executor executor;
	
	private final String reportsPath;
	private final String actionsReportsPath;
	
	public ReportsWriter(Executor executor, String reportsPath, String actionsReportsPath)
	{
		this.executor = executor;
		this.reportsPath = reportsPath;
		this.actionsReportsPath = actionsReportsPath;
	}
	
	
	public Executor getExecutor()
	{
		return executor;
	}
	
	public String getReportsPath()
	{
		return reportsPath;
	}
	
	public String getActionsReportsPath()
	{
		return actionsReportsPath;
	}
	
	
	protected AutomationReport createAutomationReport()
	{
		return new AutomationReport();
	}
	
	protected HtmlReport createHtmlReport(Matrix matrix, String pathToReport, String userName, String reportName, 
			Date startTime, Date endTime) throws IOException
	{
		return new HtmlReport(matrix, pathToReport, userName, reportName, startTime, endTime);
	}
	
	protected void makeHtmlReportsEndings(HtmlReport report, HtmlReport report_failed) throws ReportException
	{
	}
	
	public void buildAndWriteReports(Matrix matrix, List<String> matrixSteps, String userName, 
			Date startTime, Date endTime) throws IOException, ReportException
	{
		buildAndWriteHtmlReports(matrix, matrixSteps, userName, startTime, endTime);
		
		AutomationReport automationReport = buildAutomationReport(matrix, matrixSteps, userName, startTime, endTime);
		writeAutomationReport(automationReport, Paths.get(reportsPath, matrix.getShortFileName()).toString());
		
		copyResultDetailsDir(matrix);
	}
	
	protected void buildAndWriteHtmlReports(Matrix matrix, List<String> matrixSteps, String userName, 
			Date startTime, Date endTime) throws IOException, ReportException
	{
		HtmlReport report = createHtmlReport(matrix, reportsPath, userName, "report", startTime, endTime);
		HtmlReport report_failed = createHtmlReport(matrix, reportsPath, userName, "report_failed", startTime, endTime);
		
		File actionsReports = new File(actionsReportsPath);
		report.writeReport(executor.getSteps(), matrixSteps, actionsReports, false);
		report_failed.writeReport(executor.getSteps(), matrixSteps, actionsReports, true);
		
		makeHtmlReportsEndings(report, report_failed);
	}
	
	protected AutomationReport buildAutomationReport(Matrix matrix, List<String> matrixSteps, String userName, 
			Date startTime, Date endTime)
	{
		AutomationReport report = createAutomationReport();
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

		if (matrixSteps == null)
			return report;
		
		Set<String> steps = new HashSet<String>(matrixSteps);
		
		String actionsReportsDir = new File(executor.getActionsReportsDir(), matrix.getShortFileName()).getAbsolutePath();
		
		for (Step step : executor.getSteps())
		{
			if (!steps.contains(step.getSafeName()))
				continue;
			
			String storedActionsReports = Paths.get(actionsReportsDir, step.getSafeName() + ActionReportWriter.JSON_SUFFIX).toString();
			try
			{
				buildStepReport(report, step, storedActionsReports);
			}
			catch (IOException e)
			{
				logger.error("Cannot build report for step '{}'", step.getName(), e);
			}
		}
		
		return report;
	}
	
	protected void buildStepReport(AutomationReport report, Step step, String storedActionsReportsPath) throws IOException
	{
		StepReport stepReport = new StepReport(step);
		
		List<ActionReport> actionsReports = new JsonMarshaller<List<ActionReport>>().unmarshal(Paths.get(storedActionsReportsPath));

		stepReport.setActionReports(actionsReports);
		
		report.addStepReport(stepReport);
	}
	
	protected void writeAutomationReport(AutomationReport automationReport, String path) throws IOException
	{
		new JsonMarshaller<AutomationReport>().marshal(automationReport, 
				new File(ClearThCore.rootRelative(path), automationReport.getReportName() + JSON_SUFFIX).getAbsolutePath());
	}
	
	protected void copyResultDetailsDir(Matrix matrix)
	{
		File oldDetailsDir = new File(actionsReportsPath + matrix.getShortFileName(), Result.DETAILS_DIR),
				newDetailsDir = Paths.get(reportsPath, matrix.getShortFileName(), Result.DETAILS_DIR).toFile();
		if (oldDetailsDir.isDirectory())
		{
			try
			{
				FileUtils.copyDirectory(oldDetailsDir, newDetailsDir);
			}
			catch (IOException e)
			{
				logger.error("Error occurred while copying result details directory '{}' to the report files at '{}'",
						oldDetailsDir.getAbsolutePath(), newDetailsDir.getParent(), e);
			}
		}
	}
}
