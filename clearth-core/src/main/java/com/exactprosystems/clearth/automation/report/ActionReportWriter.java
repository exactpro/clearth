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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.actions.macro.MacroAction;
import com.exactprosystems.clearth.automation.report.html.HtmlActionReport;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplatesProcessor;
import com.exactprosystems.clearth.utils.JsonMarshaller;
import com.exactprosystems.clearth.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static com.exactprosystems.clearth.automation.report.ReportFormat.HTML;
import static com.exactprosystems.clearth.automation.report.ReportFormat.JSON;

public class ActionReportWriter
{
	private static final Logger logger = LoggerFactory.getLogger(ActionReportWriter.class);

	public static final String HTML_SUFFIX = ".html",
    		JSON_SUFFIX = ".json",
    		REPORT_FILENAME = "report",
    		HTML_REPORT_NAME = REPORT_FILENAME+HTML_SUFFIX,
    		JSON_REPORT_NAME = REPORT_FILENAME+JSON_SUFFIX,
    		FAILED_SUFFIX = "_failed",
    		HTML_FAILED_REPORT_NAME = REPORT_FILENAME + FAILED_SUFFIX + HTML_SUFFIX;

	private int actionIndex = 0;
	private final ReportsConfig reportsConfig;
	private final ReportTemplatesProcessor templatesProcessor;
	
	public ActionReportWriter(ReportsConfig reportsConfig, ReportTemplatesProcessor templatesProcessor)
	{
		this.reportsConfig = reportsConfig;
		this.templatesProcessor = templatesProcessor;
	}
	
	public void reset()
	{
		actionIndex = 0;
	}
	
	/**
	 * Writes HTML report to file where other action reports are stored for given matrix and given step.
	 * @param action to write report for
	 * @param actionsReportsDir path to directory with execution reports data. Action report file will be located in it
	 * @param stepFileName name of file with action reports for particular step
	 */
	public void writeReport(Action action, String actionsReportsDir, String stepFileName)
	{
		incActionIndex();
		if (action.getResult() != null)
			action.getResult().processDetails(getReportDir(actionsReportsDir, action), action);

		if (reportsConfig.isCompleteHtmlReport())
			writeHtmlActionReport(action, actionsReportsDir, stepFileName, false);
		if (reportsConfig.isFailedHtmlReport() && !action.isPassed())
			writeHtmlActionReport(action, actionsReportsDir, stepFileName, true);

		if (reportsConfig.isCompleteJsonReport())
			writeJsonActionReport(action, actionsReportsDir, stepFileName);
	}
	
	/**
	 * Updates report files to allow addition of data to them
	 * @param actionsReportsDir path to directory with execution reports data
	 * @param matrixReportsDir name of directory with action reports for particular matrix
	 * @param stepFileName name of file with action reports for particular step
	 * @throws IOException when report file update failed
	 */
	public void prepareReportsToUpdate(String actionsReportsDir, String matrixReportsDir, String stepFileName) throws IOException
	{
		if (!reportsConfig.isCompleteJsonReport())
			return;
		
		prepareJsonReportToUpdate(actionsReportsDir, matrixReportsDir, stepFileName);
	}
	
	
	protected void writeJsonActionReport(Action action, String actionsReportsDir, String actionsReportFile)
	{
		String reportFilePath = getJsonStepReportPath(actionsReportsDir, action.getMatrix().getShortFileName(), actionsReportFile);
		File reportFile = new File(reportFilePath);
		
		PrintWriter writer = null;
		try
		{
			writer = createReportWriter(reportFile);
			if (reportFile.length() == 0)
				writer.println("[");
			else
				writer.println(",");
			
			ActionReport actionReport = createActionReport(action);
			String jsonActionReport = new JsonMarshaller<ActionReport>().marshal(actionReport);
			
			if (!action.isAsync() || action.isPayloadFinished())
				writer.println(jsonActionReport);
			else
			{
				writePreReportData(writer, action, JSON);
				writer.println(jsonActionReport);
				writePostReportData(writer, action, JSON);
			}
		}
		catch (Exception e)
		{
			getLogger().error("Error occurred while writing JSON action report", e);
		}
		finally
		{
			if (writer != null)
			{
				writer.flush();
				writer.close();
			}
		}
	}

	/**
	 * Updates already written reports with actual result of asynchronous action.
	 * @param action asynchronous action to update report
	 * @param actionsReportsDir path to directory with execution reports data. Action report file is located in it
	 * @param actionsReportFile file with action reports to update
	 */
	public void updateReports(Action action, String actionsReportsDir, String actionsReportFile)
	{
		incActionIndex();
		if (action.getResult() != null)
			action.getResult().processDetails(getReportDir(actionsReportsDir, action), action);
		
		if (getLogger().isDebugEnabled())
			getLogger().debug(action.getDescForLog("Updating reports for"));
		
		updateHtmlReport(action, actionsReportsDir, actionsReportFile);
		
		updateJsonReport(action, actionsReportsDir, actionsReportFile);
	}

	protected void updateHtmlReport(Action action, String actionsReportsDir, String actionsReportFile)
	{
		String resultId = buildResultId(actionsReportFile);
		File reportDir = getReportDir(actionsReportsDir, action),
				reportFile = new File(reportDir, actionsReportFile+".swp"),
				originalReportFile = getReportFile(reportDir, actionsReportFile, false);
		if (!updateReport(originalReportFile, action, resultId, reportFile, HTML, reportDir, false))
			return;
		
		if (!originalReportFile.delete())
		{
			getLogger().error("Could not delete original report file '"+originalReportFile.getAbsolutePath()+"'");
			return;
		}
		
		if (!reportFile.renameTo(originalReportFile))
			getLogger().error("Could not rename updated report file '"+reportFile.getAbsolutePath()+"' to '"+originalReportFile.getAbsolutePath()+"'");
	}

	protected void updateJsonReport(Action action, String actionsReportsDir, String actionsReportFile)
	{
		String reportFilePath = getJsonStepReportPath(actionsReportsDir, action.getMatrix().getShortFileName(), actionsReportFile);
		File reportFile = new File(reportFilePath + ".swp"),
				originalReportFile = new File(reportFilePath);
		if (!updateReport(originalReportFile, action, "", reportFile, JSON, 
				getReportDir(actionsReportsDir, action), false))
			return;

		if (!originalReportFile.delete())
		{
			getLogger().error("Could not delete original report file '"+originalReportFile.getAbsolutePath()+"'");
			return;
		}

		if (!reportFile.renameTo(originalReportFile))
			getLogger().error("Could not rename updated report file '"+reportFile.getAbsolutePath()+"' to '"+originalReportFile.getAbsolutePath()+"'");
	}

	private String getJsonStepReportPath(String actionsReportsDir, String matrixFileName, String stepReportFile)
	{
		Path reportDir = Path.of(actionsReportsDir, matrixFileName);
		try
		{
			if (!Files.exists(reportDir))
				Files.createDirectories(reportDir);
		}
		catch (IOException e)
		{
			getLogger().error("Could not create directories", e);
		}
		
		return rootRelative(reportDir.resolve(stepReportFile + JSON_SUFFIX).toString());
	}
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	public int getActionIndex()
	{
		return actionIndex;
	}
	
	public void incActionIndex()
	{
		actionIndex++;
	}
	
	
	protected String buildResultId(String actionsReportFile)
	{
		return actionsReportFile+"_action_"+actionIndex;
	}
	
	protected File getReportDir(String actionsReportsDir, Action action)
	{
		File reportDir = new File(ClearThCore.appRootRelative(actionsReportsDir)+action.getMatrix().getShortFileName()+File.separator);
		reportDir.mkdirs();
		return reportDir;
	}
	
	protected File getReportFile(File reportDir, String actionsReportFile, boolean onlyFailed)
	{
		if (onlyFailed)
			actionsReportFile += FAILED_SUFFIX;
		return new File(reportDir, actionsReportFile);
	}
	
	protected BufferedReader createReportReader(File reportFile) throws IOException
	{
		return new BufferedReader(new FileReader(reportFile));
	}
	
	protected PrintWriter createReportWriter(File reportFile) throws IOException
	{
		return new PrintWriter(new BufferedWriter(new FileWriter(reportFile, true)));
	}
	
	protected HtmlActionReport createHtmlActionReport()
	{
		return new HtmlActionReport(templatesProcessor);
	}

	protected ActionReport createActionReport(Action action)
	{
		return !(action instanceof MacroAction) ? new ActionReport(action, this)
				: new MacroActionReport((MacroAction)action, this);
	}

	protected ActionReport createActionReport()
	{
		return new ActionReport();
	}
	
	protected String buildAsyncActionStartComment(Action action, ReportFormat reportFormat)
	{
		String label = "ASYNC action " + action.getIdInMatrix() + " start";
		return wrapCommentLabelByFormat(label, reportFormat);
	}
	
	protected String buildAsyncActionEndComment(Action action, ReportFormat reportFormat)
	{
		String label = "ASYNC action " + action.getIdInMatrix() + " end";
		return wrapCommentLabelByFormat(label, reportFormat);
	}

	protected String wrapCommentLabelByFormat(String label, ReportFormat reportFormat)
	{
		switch (reportFormat)
		{
			case HTML: return "<!-- " + label + " -->";
			case JSON: return "/* " + label + " */";
			default: return "";
		}
	}

	protected void writePreReportData(PrintWriter writer, Action action, ReportFormat reportFormat)
	{
		writer.println(buildAsyncActionStartComment(action, reportFormat));
	}

	protected void writePostReportData(PrintWriter writer, Action action, ReportFormat reportFormat)
	{
		writer.println(buildAsyncActionEndComment(action, reportFormat));
	}

	protected void writeHtmlActionReport(Action action, String actionsReportsDir, String actionsReportFile, boolean onlyFailed)
	{
		if (getLogger().isTraceEnabled())
			getLogger().trace(action.getDescForLog("Writing report for"));
		String resultId = buildResultId(actionsReportFile);
		File reportDir = getReportDir(actionsReportsDir, action),
				reportFile = getReportFile(reportDir, actionsReportFile, onlyFailed);
		PrintWriter writer = null;
		try
		{
			writer = createReportWriter(reportFile);
			HtmlActionReport report = createHtmlActionReport();

			if (!action.isAsync() || action.isPayloadFinished())
			{
				report.write(writer, action, resultId, reportDir, onlyFailed);
			}
			else
			{
				writePreReportData(writer, action, HTML);
				report.write(writer, action, resultId, reportDir, onlyFailed);
				writePostReportData(writer, action, HTML);
			}
		}
		catch (IOException e)
		{
			getLogger().error("Could not write action report", e);
		}
		finally
		{
			if (writer != null)
			{
				writer.flush();
				writer.close();
			}
		}
	}
	
	protected boolean updateReport(File originalReportFile, Action action, String resultId, File updatedReportFile,
								   ReportFormat reportFormat, File reportDir, boolean onlyFailed)
	{
		boolean startFound = false,
				endFound = false;
		BufferedReader reader = null;
		PrintWriter writer = null;
		try
		{
			reader = createReportReader(originalReportFile);
			writer = createReportWriter(updatedReportFile);
			
			//Searching for special comments to find action data.
			//All lines between them will be skipped and thus replaced with actual action data. All other lines are kept.
			ActionReport report = createActionReportForUpdate(action, reportFormat);
			if (report == null)
			{
				getLogger().warn("Unknown report format specified for action report update");
				return false;
			}

			String startToFind = buildAsyncActionStartComment(action, reportFormat),
					endToFind = buildAsyncActionEndComment(action, reportFormat),
					line;
			while ((line = reader.readLine()) != null)
			{
				if (!startFound)
				{
					if (!line.equals(startToFind))
					{
						writer.println(line);
						continue;
					}
					
					startFound = true;
					writeActionReport(report, reportFormat, writer, action, resultId, reportDir, onlyFailed);
				}
				else if (!endFound)
				{
					if (!line.equals(endToFind))
						continue;
					
					endFound = true;
				}
				else
					writer.println(line);
			}
			
			if (!startFound)
				//Storing action result anyway to restore it later, if needed
				writeActionReport(report, reportFormat, writer, action, resultId, reportDir, onlyFailed);
		}
		catch (IOException e)
		{
			logger.error("Could not write action report", e);
			return false;  //On error report can't be updated and current report should remain intact
		}
		finally
		{
			if (writer != null)
			{
				writer.flush();
				writer.close();
			}
			
			Utils.closeResource(reader);
		}
		
		if (!startFound)
		{
			logger.warn("Async action start not found. Report is not updated");
			return false;
		}
		if (!endFound)
		{
			logger.warn("Async action end not found. Report is not updated not to affect other data");
			return false;
		}
		
		return true;
	}

	protected ActionReport createActionReportForUpdate(Action action, ReportFormat format)
	{
		switch (format)
		{
			case HTML: return createHtmlActionReport();
			case JSON: return createActionReport(action);
			default: return null;
		}
	}

	protected void writeActionReport(ActionReport report, ReportFormat format, PrintWriter writer, Action action,
									 String containerId, File reportDir, boolean onlyFailed) throws IOException
	{
		switch (format)
		{
			case HTML:
				((HtmlActionReport)report).write(writer, action, containerId, reportDir, onlyFailed);
				return;

			case JSON:
				String reportStr = new JsonMarshaller<ActionReport>().marshal(report);
				writer.println(reportStr);
		}
	}

	public void makeReportsEnding(String actionsReportsDir, String stepSafeName)
	{
		File[] files = new File(rootRelative(actionsReportsDir)).listFiles();
		if (files == null || files.length == 0)
			return;

		for (File file : files)
		{
			if (!file.isDirectory())
				continue;

			completeStepJsonReport(actionsReportsDir, file.getName(), stepSafeName);
		}
	}

	protected void completeStepJsonReport(String actionsReportsDir, String matrixFileName, String stepSafeName)
	{
		String stepReportPath = getJsonStepReportPath(actionsReportsDir, matrixFileName, stepSafeName);
		File reportFile = new File(stepReportPath);
		if (!reportFile.isFile())
			return;

		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(new FileWriter(reportFile, true));
			writer.println("]");
		}
		catch (IOException e)
		{
			getLogger().error("Cannot complete step json report", e);
		}
		finally
		{
			if (writer != null)
			{
				writer.flush();
				writer.close();
			}
		}
	}
	
	
	private void prepareJsonReportToUpdate(String actionsReportsDir, String matrixReportsDir, String stepFileName) throws IOException
	{
		File reportFile = new File(getJsonStepReportPath(actionsReportsDir, matrixReportsDir, stepFileName)),
				tempFile = File.createTempFile(reportFile.getName()+"_", ".tmp", reportFile.getParentFile());
		try (BufferedReader reader = new BufferedReader(new FileReader(reportFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
		{
			String skipped = null,
					line = null;
			while ((line = reader.readLine()) != null)
			{
				if (skipped != null)
				{
					writer.write(skipped);
					writer.newLine();
					skipped = null;
				}
				
				if (line.equals("]"))
					skipped = line;
				else
				{
					writer.write(line);
					writer.newLine();
				}
			}
		}
		catch (Exception e)
		{
			Files.delete(tempFile.toPath());
			throw e;
		}
		
		Files.move(tempFile.toPath(), reportFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}
}
