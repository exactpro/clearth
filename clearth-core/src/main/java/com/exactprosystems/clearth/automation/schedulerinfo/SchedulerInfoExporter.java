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

package com.exactprosystems.clearth.automation.schedulerinfo;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.MatrixData;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.schedulerinfo.template.SchedulerInfoTemplateFiles;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import freemarker.template.TemplateException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SchedulerInfoExporter
{
	private static final Logger logger = LoggerFactory.getLogger(SchedulerInfoExporter.class);

	protected static final String SCHEDULER_SUMMARY_INFO_FILE = "schedulerInfo.html",
			PATH_TO_RESOURCE_FILES = ClearThCore.getInstance().getSchedulerInfoFilePath();
	protected static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public SchedulerInfoFile collectFiles(Scheduler scheduler) throws IOException
	{
		// Obtaining necessary metadata to collect scheduler files
		List<XmlMatrixInfo> matricesInfo = null;
		String reportsPath = null;
		if (scheduler.isRunning())
		{
			ReportsInfo info = scheduler.makeCurrentReports(scheduler.getReportsDir() + "current_" + DATETIME_FORMAT.format(new Date()));
			matricesInfo = info.getMatrices();
			reportsPath = info.getPath();
		}
		else
		{
			List<XmlSchedulerLaunchInfo> launches = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
			XmlSchedulerLaunchInfo lastLaunch = launches.size() > 0 ? launches.get(0) : null;
			if (lastLaunch != null)
			{
				matricesInfo = lastLaunch.getMatricesInfo();
				reportsPath = ClearThCore.reportsPath() + lastLaunch.getReportsPath();
			}
		}
		
		SchedulerInfoFile exportFiles = createSchedulerInfoFile("root", null);
		exportFiles.addChildFile(getMatrices(scheduler.getMatricesData(), exportFiles));
		exportFiles.addChildFile(getReports(reportsPath, exportFiles));
		exportFiles.addChildFile(getOtherFiles(exportFiles));
		exportFiles.addChildFile(createSchedulerSummaryFile(SCHEDULER_SUMMARY_INFO_FILE, scheduler.getSteps(), scheduler.getMatricesData(), matricesInfo, exportFiles));
		return exportFiles;
	}
	
	public File exportSelectedZip(SchedulerInfoFile toExport) throws IOException
	{
		return exportZip(toExport, true);
	}
	
	public File exportAllZip(Scheduler scheduler) throws IOException
	{
		return exportZip(collectFiles(scheduler), false);
	}
	
	protected File exportZip(SchedulerInfoFile toExport, boolean checkInclude) throws IOException
	{
		// Collecting all selected files and putting them into zip archive
		File resultFile = File.createTempFile("scheduler_info_", ".zip", new File(ClearThCore.tempPath()));
		Map<File, String> filesAndNames = collectFilesAndNamesToExport(toExport, "", checkInclude);
		FileOperationUtils.zipFiles(resultFile, filesAndNames.keySet().toArray(new File[0]), filesAndNames.values().toArray(new String[0]));
		
		// Trying to find and delete scheduler summary info file
		filesAndNames.entrySet().stream().filter(fileAndName -> fileAndName.getValue().equals(SCHEDULER_SUMMARY_INFO_FILE))
				.findFirst().map(Map.Entry::getKey).ifPresent(File::delete);
		return resultFile;
	}
	
	protected Map<File, String> collectFilesAndNamesToExport(SchedulerInfoFile file, String archiveDirPath, boolean checkInclude) throws IOException
	{
		Map<File, String> result = new HashMap<>();
		for (SchedulerInfoFile child : file.getChildren())
		{
			if (checkInclude && !child.isInclude())
				continue;
			
			if (child.isDirectory())
				result.putAll(collectFilesAndNamesToExport(child, archiveDirPath + child.getName() + "/", checkInclude));
			else if (child instanceof SchedulerSummaryFile)
			{
				SchedulerSummaryFile summaryFile = (SchedulerSummaryFile)child;
				File generatedFile = createSummaryInfoPage(summaryFile.getSteps(), summaryFile.getMatricesData(), summaryFile.getMatricesInfo());
				summaryFile.setFilePath(generatedFile.getAbsolutePath());
				result.put(generatedFile, archiveDirPath + summaryFile.getName());
			}
			else
				result.put(new File(child.getFilePath()), archiveDirPath + child.getName());
		}
		return result;
	}
	
	
	protected SchedulerInfoFile getMatrices(List<MatrixData> matricesData, SchedulerInfoFile parent) throws IOException
	{
		if (CollectionUtils.isEmpty(matricesData))
			return null;
		
		SchedulerInfoFile matricesDir = createSchedulerInfoFile("matrices", parent);
		for (MatrixData matrixData : matricesData)
			matricesDir.addChildFile(createSchedulerInfoFile(matrixData.getFile(), parent));
		return matricesDir;
	}
	
	protected SchedulerInfoFile getReports(String reportsPath, SchedulerInfoFile parent) throws IOException
	{
		if (reportsPath != null)
		{
			File reports = new File(reportsPath);
			if (reports.isDirectory())
				return processReportDir(reports, createSchedulerInfoFile("reports", parent));
		}
		return null;
	}
	
	protected SchedulerInfoFile processReportDir(File reportDir, SchedulerInfoFile parent) throws IOException
	{
		File[] reports = reportDir.listFiles();
		if (ArrayUtils.isNotEmpty(reports))
		{
			for (File file : reports)
				parent.addChildFile(file.isDirectory() ? processReportDir(file, createSchedulerInfoFile(file, parent)) : createSchedulerInfoFile(file, parent));
		}
		return parent;
	}
	
	protected SchedulerInfoFile getOtherFiles(SchedulerInfoFile parent) throws IOException
	{
		return null;
	}
	
	
	protected File createSummaryInfoPage(List<Step> steps, List<MatrixData> matricesData, List<XmlMatrixInfo> matricesInfo) throws IOException
	{
		PrintWriter writer = null;
		File summaryInfoFile = File.createTempFile(SCHEDULER_SUMMARY_INFO_FILE, null, new File(ClearThCore.tempPath()));
		try
		{
			writer = new PrintWriter(new BufferedWriter(new FileWriter(summaryInfoFile)));
			Map<String, Object> parameters = initTemplateParameters(steps, matricesData, matricesInfo);
			ClearThCore.getInstance().getSchedulerInfoTemplatesProcessor().processTemplate(writer, parameters, SchedulerInfoTemplateFiles.SCHEDULER_INFO);
		}
		catch (TemplateException e)
		{
			getLogger().error("An error occurred while processing the template of the scheduler info: ", e);
		}
		finally
		{
			Utils.closeResource(writer);
		}
		return summaryInfoFile;
	}
	
	protected Map<String, Object> initTemplateParameters(List<Step> steps, List<MatrixData> matricesData, List<XmlMatrixInfo> matricesInfo)
	{
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("pathToStyles", PATH_TO_RESOURCE_FILES + "style.css");
		parameters.put("pathToJS", PATH_TO_RESOURCE_FILES + "script.js");
		parameters.put("revision", getRevisionData());
		parameters.put("stepsData", createStepsData(steps));
		parameters.put("matricesData", matricesData);
		parameters.put("reportsData", matricesInfo);
		addExtraTemplateParameters(parameters);
		return parameters;
	}
	
	protected String getRevisionData()
	{
		return ClearThCore.getInstance().getVersion().getBuildNumber();
	}
	
	protected List<SchedulerStepData> createStepsData(List<Step> allSteps)
	{
		List<SchedulerStepData> stepsData = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(allSteps))
		{
			for (Step step : allSteps)
				stepsData.add(new SchedulerStepData(step, StringEscapeUtils.escapeHtml(step.getName())));
		}
		return stepsData;
	}
	
	protected void addExtraTemplateParameters(Map<String, Object> parameters)
	{
	
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	protected SchedulerInfoFile createSchedulerInfoFile(String name, SchedulerInfoFile parent)
	{
		return new SchedulerInfoFile(name, parent);
	}
	
	protected SchedulerInfoFile createSchedulerInfoFile(File f, SchedulerInfoFile parent)
	{
		return new SchedulerInfoFile(f, parent);
	}
	
	public SchedulerSummaryFile createSchedulerSummaryFile(String name, List<Step> steps, List<MatrixData> matricesData,
			List<XmlMatrixInfo> matricesInfo, SchedulerInfoFile parent)
	{
		return new SchedulerSummaryFile(name, steps, matricesData, matricesInfo, parent);
	}
}
