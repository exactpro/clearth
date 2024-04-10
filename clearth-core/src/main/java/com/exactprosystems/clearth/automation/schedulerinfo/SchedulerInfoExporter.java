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

package com.exactprosystems.clearth.automation.schedulerinfo;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.MatrixData;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.StepData;
import com.exactprosystems.clearth.automation.schedulerinfo.template.SchedulerInfoTemplateFiles;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import freemarker.template.TemplateException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SchedulerInfoExporter
{
	private static final Logger logger = LoggerFactory.getLogger(SchedulerInfoExporter.class);
	
	public static final String SUMMARY_FILE = "schedulerInfo.html", MATRICES = "matrices/", REPORTS = "reports/";
	
	protected static final String PATH_TO_RESOURCE_FILES = ClearThCore.getInstance().getSchedulerInfoFilePath();
	protected static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public MultiValuedMap<String, SchedulerInfoFile> collectFiles(Scheduler scheduler) throws IOException
	{
		// Obtain necessary metadata to collect scheduler files
		ReportsInfo reportsInfo = getReportsInfo(scheduler);

		List<MatrixData> executedMatricesData = scheduler.getExecutedMatricesData();
		List<StepData> executedStepsData = scheduler.getExecutedStepsData();

		// if the scheduler was not started
		if (reportsInfo.getMatrices() == null
				|| reportsInfo.getPath() == null
				|| executedMatricesData.isEmpty()
				|| executedStepsData.isEmpty())
			return MultiMapUtils.emptyMultiValuedMap();

		// Create and fill up the storage with all files could be exported
		MultiValuedMap<String, SchedulerInfoFile> storage = new HashSetValuedHashMap<>();
		storage.put(SUMMARY_FILE, createSummaryFile(executedStepsData, executedMatricesData, reportsInfo));
		storage.putAll(MATRICES, collectMatrices(executedMatricesData));
		storage.putAll(REPORTS, collectReports(reportsInfo.getPath()));
		storage = collectOtherFiles(storage, scheduler);
		return MultiMapUtils.unmodifiableMultiValuedMap(storage);
	}
	
	private ReportsInfo getReportsInfo(Scheduler scheduler)
	{
		if (scheduler.isRunning())
			return getCurrentReportsInfo(scheduler);
		else
			return getLastReportsInfo(scheduler);
	}
	
	
	protected ReportsInfo getCurrentReportsInfo(Scheduler scheduler)
	{
		return scheduler.makeCurrentReports(scheduler.getReportsDir() +
				"current_" + DATETIME_FORMAT.format(new Date()), false, false);
	}
	
	protected ReportsInfo getLastReportsInfo(Scheduler scheduler)
	{
		List<XmlSchedulerLaunchInfo> launches = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		XmlSchedulerLaunchInfo lastLaunch = launches.isEmpty() ? null : launches.get(0);
		ReportsInfo reportsInfo = new ReportsInfo();
		if (lastLaunch != null)
		{
			reportsInfo.setMatrices(lastLaunch.getMatricesInfo());
			reportsInfo.setPath(ClearThCore.reportsPath() + lastLaunch.getReportsPath());
		}
		return reportsInfo;
	}
	
	
	protected SchedulerInfoFile createSummaryFile(List<StepData> stepData, List<MatrixData> matricesData,
	                                              ReportsInfo reportsInfo) throws IOException
	{
		PrintWriter writer = null;
		File summaryFile = new File(ClearThCore.tempPath() + SUMMARY_FILE);
		try
		{
			writer = new PrintWriter(new BufferedWriter(new FileWriter(summaryFile)));
			Map<String, Object> parameters = initTemplateParameters(stepData, matricesData, reportsInfo);
			ClearThCore.getInstance().getSchedulerInfoTemplatesProcessor().processTemplate(writer, parameters,
					SchedulerInfoTemplateFiles.SCHEDULER_INFO);
		}
		catch (TemplateException e)
		{
			getLogger().error("An error occurred while processing the template of the scheduler info", e);
		}
		finally
		{
			Utils.closeResource(writer);
		}
		return new SchedulerInfoFile(SUMMARY_FILE, summaryFile);
	}

	protected Map<String, Object> initTemplateParameters(List<StepData> stepData, List<MatrixData> matricesData,
	                                                     ReportsInfo reportsInfo)
	{
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("pathToStyles", PATH_TO_RESOURCE_FILES + "style.css");
		parameters.put("pathToJS", PATH_TO_RESOURCE_FILES + "script.js");
		parameters.put("revision", getRevisionData());
		parameters.put("stepsData", createExecutedStepsData(stepData));
		parameters.put("matricesData", matricesData);
		parameters.put("reportsData", reportsInfo.getMatrices());
		addExtraTemplateParameters(parameters);
		return parameters;
	}
	
	protected String getRevisionData()
	{
		return ClearThCore.getInstance().getVersion().getBuildNumber();
	}

	protected List<SchedulerStepData> createExecutedStepsData(List<StepData> stepData)
	{
		List<SchedulerStepData> stepsData = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(stepData))
		{
			for (StepData infoData : stepData)
				stepsData.add(new SchedulerStepData(infoData, StringEscapeUtils.escapeHtml(infoData.getName())));
		}
		return stepsData;
	}

	protected void addExtraTemplateParameters(Map<String, Object> parameters)
	{
	
	}
	
	protected Set<SchedulerInfoFile> collectMatrices(List<MatrixData> matricesData) throws IOException
	{
		if (CollectionUtils.isEmpty(matricesData))
			return Collections.emptySet();
		
		return matricesData.stream().map(MatrixData::getFile)
				.map(f -> new SchedulerInfoFile(MATRICES + f.getName(), f)).collect(Collectors.toSet());
	}
	
	protected Set<SchedulerInfoFile> collectReports(String reportsPath) throws IOException
	{
		return FileOperationUtils.getFilesFromDir(reportsPath, null, true)
				.stream().map(f -> new SchedulerInfoFile(REPORTS
						+ f.getAbsolutePath().substring(reportsPath.length() + 1), f)).collect(Collectors.toSet());
	}
	
	protected MultiValuedMap<String, SchedulerInfoFile> collectOtherFiles(MultiValuedMap<String, SchedulerInfoFile> storage,
			Scheduler scheduler) throws IOException
	{
		return storage;
	}
	
	
	public File exportSelectedZip(MultiValuedMap<String, SchedulerInfoFile> storage) throws IOException
	{
		return exportZip(storage, true);
	}
	
	public File exportAllZip(Scheduler scheduler) throws IOException
	{
		return exportZip(collectFiles(scheduler), false);
	}
	
	protected File exportZip(MultiValuedMap<String, SchedulerInfoFile> storage, boolean checkInclusion) throws IOException
	{
		// Collect files to export and its output paths
		List<File> files = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (SchedulerInfoFile file : storage.values())
		{
			if (!checkInclusion || file.isInclude())
			{
				files.add(file.getOriginalFile());
				names.add(file.getOutputPath());
			}
		}
		
		// Write ZIP archive with selected files
		File resultFile = File.createTempFile("scheduler_info_", ".zip", new File(ClearThCore.tempPath()));
		FileOperationUtils.zipFiles(resultFile, files, names);
		
		// Remove temporary written summary file if exist
		Collection<SchedulerInfoFile> summaryFile = storage.get(SUMMARY_FILE);
		if (!summaryFile.isEmpty())
			FileUtils.deleteQuietly(summaryFile.iterator().next().getOriginalFile());
		return resultFile;
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}
}
