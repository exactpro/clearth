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

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.Executor;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.report.html.HtmlReport;
import com.exactprosystems.clearth.automation.report.json.JsonReport;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static com.exactprosystems.clearth.automation.report.ActionReportWriter.JSON_REPORT_NAME;

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


	protected HtmlReport createHtmlReport(Matrix matrix, String pathToReport, String userName, String reportName, 
			Date startTime, Date endTime) throws IOException
	{
		return new HtmlReport(matrix, pathToReport, userName, reportName, startTime, endTime);
	}
	
	protected JsonReport createJsonReport(Matrix matrix, Collection<Step> allSteps, Set<String> matrixStepNames,
	                                      String userName, Date startTime, Date endTime)
	{
		return new JsonReport(matrix, allSteps, matrixStepNames, userName, startTime, endTime);
	}
	
	protected void makeHtmlReportsEndings(HtmlReport report, HtmlReport report_failed) throws ReportException
	{
	}
	
	public void buildAndWriteReports(Matrix matrix, List<String> matrixSteps, String userName) throws IOException, ReportException
	{
		Date startTime = executor.getStarted();
		Date endTime = calcReportEndTime();

		buildAndWriteHtmlReports(matrix, matrixSteps, userName, startTime, endTime);
		buildAndWriteJsonReports(matrix, matrixSteps, userName, startTime, endTime);
		copyResultDetailsDir(matrix);
	}

	private Date calcReportEndTime()
	{
		if (executor.isTerminated())
		{
			return executor.getEnded();
		}

		Date endTime = executor.getEnded();
		for (Step step : executor.getSteps())
		{
			if (!step.isExecute())
				continue;

			if (step.getStarted() == null)
				return endTime;

			for (Action action : step.getActions())
			{
				Date actionEndTime = action.getFinished();
				if (actionEndTime == null)
					break;

				if (endTime.before(actionEndTime))
					endTime = actionEndTime;
			}
		}
		return endTime;
	}
	
	protected void buildAndWriteHtmlReports(Matrix matrix, List<String> matrixSteps, String userName, 
			Date startTime, Date endTime) throws IOException, ReportException
	{
		logger.debug("Writing HTML reports for matrix '{}'...", matrix.getName());
		
		HtmlReport report = createHtmlReport(matrix, reportsPath, userName, "report", startTime, endTime);
		HtmlReport report_failed = createHtmlReport(matrix, reportsPath, userName, "report_failed", startTime, endTime);
		
		File actionsReports = new File(actionsReportsPath);
		report.writeReport(executor.getSteps(), matrixSteps, actionsReports, false);
		report_failed.writeReport(executor.getSteps(), matrixSteps, actionsReports, true);
		
		makeHtmlReportsEndings(report, report_failed);
	}
	
	
	protected void buildAndWriteJsonReports(Matrix matrix, List<String> matrixSteps, String userName,
	                                        Date startTime, Date endTime) throws IOException
	{
		JsonReport report = createJsonReport(matrix, executor.getSteps(), new HashSet<>(matrixSteps),
				userName, startTime, endTime);
		
		Path reportPath = Paths.get(rootRelative(reportsPath), matrix.getShortFileName(), JSON_REPORT_NAME);
		Path actionsReportsDir = Paths.get(rootRelative(executor.getActionsReportsDir()), matrix.getShortFileName());
		report.writeReport(reportPath, actionsReportsDir);
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
