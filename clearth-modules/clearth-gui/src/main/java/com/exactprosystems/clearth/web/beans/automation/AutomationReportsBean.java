/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.report.ActionReportWriter;
import com.exactprosystems.clearth.utils.LogsExtractor;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.beans.ClearThCoreApplicationBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunches;
import org.apache.commons.lang.time.DateUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.StreamedContent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.automation.async.AsyncActionsManager.DEFAULT_ASYNC_THREAD_NAME;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AutomationReportsBean extends ClearThBean
{

	protected AutomationBean automationBean;

	protected ReportsInfo selectedReportsInfo;
	protected List<XmlMatrixInfo> filteredReportsInfo;

	protected static final String LOG_TO_EXTRACT = "all.log";
	protected static final String EXTRACTED_LOG = "short_"+ LOG_TO_EXTRACT;

	protected final SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

	//Real-time reports variables
	protected List<Matrix> rtMatrices;
	protected List<Matrix> filteredRTMatrices;

	protected List<Step> rtSelectedSteps;
	protected List<Step> filteredRTSteps;

	public ReportFilters reportFilters;

	public AutomationReportsBean()
	{
		this.rtMatrices = new ArrayList<>();
		this.rtSelectedSteps = new ArrayList<>();
		this.reportFilters = createReportFilter();
		this.reportFilters.setRtSelectedSteps(rtSelectedSteps);
	}

	protected Scheduler selectedScheduler() {
		return this.automationBean.selectedScheduler;
	}

	public void setAutomationBean(AutomationBean automationBean) {
		this.automationBean = automationBean;
	}

	public ReportFilters createReportFilter() {
		return new ReportFilters();
	}

	/* Reports */

	public String getLastLaunchReportUrl()
	{
		XmlMatrixInfo lastLaunchInfo = getLastLaunch().getMatricesInfo().get(0);
		return getReportPath(getLastLaunch().getReportsPath(), lastLaunchInfo.getFileName(), ActionReportWriter.HTML_REPORT_NAME);
	}

	public String getLastLaunchJsonReportUrl()
	{
		XmlMatrixInfo lastLaunchInfo = getLastLaunch().getMatricesInfo().get(0);
		return getReportPath(getLastLaunch().getReportsPath(), lastLaunchInfo.getFileName(), ActionReportWriter.JSON_REPORT_NAME);
	}


	protected String getReportPath(String reportsPath, String matrixFileName, String reportName)
	{
		return MessageFormat.format("{0}/{1}/{2}/{3}/{4}",                  // For example:
				ClearThCoreApplicationBean.getInstance().getAppContextPath(),       // "/clearth"
				"reports",															// "reports"
				reportsPath,                                                        // "admin/20171205121833073/completed"
				automationBean.safeFileName(matrixFileName),                        // "linked_matrix_1497950729006.csv"
				reportName);
	}

	public ReportsInfo getSelectedReportsInfo()
	{
		return selectedReportsInfo;
	}

	public void setSelectedReportsInfo(ReportsInfo reportsInfo)
	{
		selectedReportsInfo = reportsInfo;
	}

	public List<XmlMatrixInfo> getFilteredReportsInfo()
	{
		return filteredReportsInfo;
	}

	public void setFilteredReportsInfo(List<XmlMatrixInfo> filteredReportsInfo)
	{
		this.filteredReportsInfo = filteredReportsInfo;
	}

	public void extractReportsInfo(XmlSchedulerLaunchInfo launchInfo)
	{
		selectedReportsInfo = new ReportsInfo();
		selectedReportsInfo.setPath(launchInfo.getReportsPath());
		selectedReportsInfo.getMatrices().addAll(launchInfo.getMatricesInfo());
		selectedReportsInfo.setStarted(launchInfo.getStarted());
		selectedReportsInfo.setFinished(launchInfo.getFinished());
	}

	public XmlSchedulerLaunches getXmlLaunches()
	{
		return selectedScheduler().getSchedulerData().getLaunches();
	}

	public List<XmlSchedulerLaunchInfo> getLaunches()
	{
		return getXmlLaunches().getLaunchesInfo();
	}

	public XmlSchedulerLaunchInfo getLastLaunch()
	{
		List<XmlSchedulerLaunchInfo> launches = getLaunches();
		if (launches.size()>0)
			return launches.get(0);
		return null;
	}

	public void makeReports()
	{
		selectedReportsInfo = selectedScheduler().makeCurrentReports(selectedScheduler().getReportsDir()
				+ "current_" + df.format(new Date()), true);
	}

	public void clearHistory(boolean cleanToday)
	{
		XmlSchedulerLaunches xmlLaunches = getXmlLaunches();

		if (cleanToday)
			xmlLaunches.clearLaunchInfoList();
		else
		{
			Date today = Calendar.getInstance().getTime();
			List<XmlSchedulerLaunchInfo> launches = getLaunches();

			launches.stream().filter(launchInfo -> !DateUtils.isSameDay(today, launchInfo.getFinished()))
				.forEach(xmlLaunches::removeLaunchInfo);
		}


		getLogger().info("cleared history of launchers in scheduler '"+selectedScheduler().getName()+"'");

		try
		{
			selectedScheduler().getSchedulerData().saveLaunches();
		}
		catch (Exception e)
		{
			getLogger().error("Clean history error: could not save launches info to file '" + selectedScheduler().getSchedulerData().getLaunchesName()+"'", e);
			MessageUtils.addWarningMessage("Could not save launches info to file", selectedScheduler().getSchedulerData().getLaunchesName());
		}
	}

	public StreamedContent getZipCurrentReports()
	{
		if (selectedScheduler().isRunning())
		{
			makeReports();
		}
		return new ReportsArchiver().setFilteredData(filteredRTMatrices, filteredReportsInfo)
				.getZipSelectedReports(true, getSelectedReportsInfo());
	}

	public StreamedContent getLogsBySelectedRun()
	{
		try
		{
			File shortLog = extractLogsBySelectedRun();
			if (shortLog == null)
			{
				String errMsg = "Could not make short log file by selected run";
				MessageUtils.addErrorMessage(errMsg, "These logs were lost");
				getLogger().debug(errMsg);
				return null;
			}
			return WebUtils.downloadFile(shortLog);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not download logs by run", e, getLogger());
			return null;
		}
	}

	private File extractLogsBySelectedRun() throws IOException
	{
		return extractLogsBySelectedRun(LOG_TO_EXTRACT, EXTRACTED_LOG);
	}

	private File extractLogsBySelectedRun(String logName, String resultName) throws IOException
	{
		File logsDir = WebUtils.getLogsDir();
		if (!logsDir.exists())
			throw new FileNotFoundException("Logs directory does not exist: "+logsDir.getPath());
		if (!logsDir.isDirectory())
			throw new NotDirectoryException("File denoted as logs directory is not a directory: "+logsDir.getPath());

		String schedulerName = selectedScheduler().getName();
		Date startTime = selectedReportsInfo.getStarted();
		Date finishTime = selectedReportsInfo.getFinished();

		LogsExtractor extractor = new LogsExtractor(logsDir, logName, schedulerName, getAsyncThreadNames(selectedScheduler()));
		return extractor.extractLogByRun(startTime, finishTime, resultName);
	}

	private File extractLogsByTime(String logName, String resultName, Date startTime, Date finishTime) throws IOException
	{
		File logsDir = WebUtils.getLogsDir();
		String schedulerName = selectedScheduler().getName();

		LogsExtractor extractor = new LogsExtractor(logsDir, logName, schedulerName, getAsyncThreadNames(selectedScheduler()));
		return extractor.extractLogByRun(startTime, finishTime, resultName);
	}

	private Set<String> getAsyncThreadNames(Scheduler scheduler)
	{
		Set<String> asyncThreadNames = new HashSet<>();
		asyncThreadNames.add(scheduler.getName());
		asyncThreadNames.add(DEFAULT_ASYNC_THREAD_NAME);

		for (Step step : scheduler.getSteps())
		{
			for (Action a : step.getActions())
				if (isNotEmpty(a.getAsyncGroup()))
					asyncThreadNames.add(a.getAsyncGroup());
		}

		return asyncThreadNames;
	}

	// real time

	public List<Matrix> getRtMatrices() {
		List<Matrix> matrices = selectedScheduler().getMatrices();

		if (rtSelectedSteps == null || rtSelectedSteps.size() == 0)
			return matrices;
		else
		{
			List<Matrix> newMatrices = new ArrayList<Matrix>();
			for (Matrix m : matrices)
			{
				for (Step s : rtSelectedSteps)
				{
					if (m.getStepSuccess().containsKey(s.getName()))
					{
						newMatrices.add(m);
						break;
					}
				}
			}
			return newMatrices;
		}
	}

	public void setRtMatrices(List<Matrix> rtMatrices) {
		this.rtMatrices = rtMatrices;
	}

	public List<Matrix> getFilteredRTMatrices() {
		return filteredRTMatrices;
	}

	public void setFilteredRTMatrices(List<Matrix> filteredRTMatrices) {
		this.filteredRTMatrices = filteredRTMatrices;
	}

	public List<Step> getRtSelectedSteps() {
		return rtSelectedSteps;
	}

	public void setRtSelectedSteps(List<Step> rtSelectedSteps) {
		this.rtSelectedSteps = rtSelectedSteps;
		this.reportFilters.setRtSelectedSteps(rtSelectedSteps);
	}

	public List<Step> getFilteredRTSteps() {
		return filteredRTSteps;
	}

	public void setFilteredRTSteps(List<Step> filteredRTSteps) {
		this.filteredRTSteps = filteredRTSteps;
	}

	public StreamedContent getZipSelectedReportsWithLogs()
	{
		try
		{
			File shortLog;
			if (selectedScheduler().isSuspended())
			{
				shortLog = extractLogsByTime(LOG_TO_EXTRACT, EXTRACTED_LOG, selectedScheduler().getStartTime(), null);
			}
			else
				shortLog = extractLogsBySelectedRun();
			if (shortLog == null)
			{
				PrimeFaces.current().executeScript("PF('lostLogsDlg').show();");
				return null;
			}

			return new ReportsArchiver().setFilteredData(filteredRTMatrices, filteredReportsInfo)
					.getZipSelectedReportsWithLogs(shortLog, getSelectedReportsInfo());
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not download reports with logs", e, getLogger());
			return null;
		}

	}

	public StreamedContent getZipSelectedReports() {
		return new ReportsArchiver().setFilteredData(filteredRTMatrices, filteredReportsInfo)
				.getZipSelectedReports(false, getSelectedReportsInfo());
	}

	public ReportFilters getReportFilters() {
		return reportFilters;
	}
}
