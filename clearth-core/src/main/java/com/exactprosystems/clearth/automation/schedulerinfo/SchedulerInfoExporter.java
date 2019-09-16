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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SchedulerInfoExporter
{
	private static final Logger logger = LoggerFactory.getLogger(SchedulerInfoExporter.class);

	private static final String SCHEDULER_INFO_FILE = "schedulerInfo.html", MATRICES_DIR = "matrices", REPORTS_DIR = "reports";
	protected static final String pathToResourceFiles;
	protected static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

	static
	{
		pathToResourceFiles = ClearThCore.getInstance().getSchedulerInfoFilePath();
	}

	public File export(Scheduler scheduler, List<XmlMatrixInfo> matricesInfo, String reportsPath) throws IOException
	{
		String destDir = ClearThCore.tempPath();
		File exportFolder = FileOperationUtils.createTempDirectory("scheduler_info_", new File(destDir));
		
		createInfoPage(exportFolder, scheduler, matricesInfo);
		copyMatrices(exportFolder, scheduler);
		copyReports(exportFolder, reportsPath);
		prepareOtherFiles(exportFolder);

		File resultFile = zipExportedFiles(exportFolder, destDir);
		
		FileUtils.deleteDirectory(exportFolder);
		return resultFile;
	}
	
	public File export(Scheduler scheduler) throws IOException
	{
		List<XmlMatrixInfo> reports = null;
		String reportsPath = null;
		
		if (scheduler.isRunning())
		{
			ReportsInfo selectedReportsInfo = scheduler.makeCurrentReports
					(scheduler.getReportsDir()+"current_"+DATETIME_FORMAT.format(new Date()));
			reports = selectedReportsInfo.getMatrices();
			reportsPath = selectedReportsInfo.getPath();
		}
		else
		{
			List<XmlSchedulerLaunchInfo> launches = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
			XmlSchedulerLaunchInfo lastLaunch = launches.size() > 0 ? launches.get(0) : null;
			if (lastLaunch != null)
			{
				reports = lastLaunch.getMatricesInfo();
				reportsPath = ClearThCore.getInstance()
						.getRootRelative(ClearThCore.configFiles().getReportsDir() + lastLaunch.getReportsPath());
			}
			else
			{
				reports = null;
				reportsPath = null;
			}
		}
		
		return export(scheduler, reports, reportsPath);
	}
	
	protected void createInfoPage(File exportFolder, Scheduler scheduler, List<XmlMatrixInfo> matricesInfo) throws IOException
	{
		String pathToFiles = exportFolder.getCanonicalPath() + "/";
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(new BufferedWriter(new FileWriter(pathToFiles + SCHEDULER_INFO_FILE)));
			Map<String, Object> parameters = initTemplateParameters(scheduler, matricesInfo);
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
	}
	
	protected void copyMatrices(File exportFolder, Scheduler scheduler) throws IOException
	{
		File matricesFolder = new File(exportFolder, MATRICES_DIR);
		matricesFolder.mkdirs();
		String matricesPath = matricesFolder.getCanonicalPath();

		for (MatrixData matrixData : scheduler.getMatricesData())
			FileOperationUtils.copyFile(matrixData.getFile().getCanonicalPath(), matricesPath + File.separator + matrixData.getFile().getName());
	}
	
	protected void copyReports(File exportFolder, String reportsPath) throws IOException
	{
		File reportFolder = new File(exportFolder, REPORTS_DIR);
		reportFolder.mkdirs();
		if (reportsPath != null)
			FileUtils.copyDirectory(new File(reportsPath), reportFolder);
	}
	
	protected void prepareOtherFiles(File exportFolder) throws IOException
	{
		
	}
	
	protected File zipExportedFiles(File exportFolder, String uploadsDirPath) throws IOException
	{
		File resultFile = File.createTempFile("scheduler_info_", ".zip", new File(uploadsDirPath));
		FileOperationUtils.zipFiles(resultFile, exportFolder.listFiles());
		FileOperationUtils.zipDirectories(resultFile, Arrays.asList(exportFolder.listFiles()));
		return resultFile;
	}

	protected Map<String, Object> initTemplateParameters(Scheduler scheduler, List<XmlMatrixInfo> matricesInfo)
	{
		Map<String, Object> parameters = new HashMap<String, Object>();

		parameters.put("pathToStyles", pathToResourceFiles + "style.css");
		parameters.put("pathToJS", pathToResourceFiles + "script.js");

		parameters.put("revision", getRevisionData());

		parameters.put("stepsData", createStepsData(scheduler.getSteps()));
		parameters.put("matricesData", scheduler.getMatricesData());

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
		if (allSteps == null || allSteps.isEmpty())
			return Collections.emptyList();
		else
		{
			List<SchedulerStepData> stepsData = new ArrayList<SchedulerStepData>(allSteps.size());
			for (Step step : allSteps)
			{
				String stepName = step.getName();
				SchedulerStepData data = new SchedulerStepData(step, StringEscapeUtils.escapeHtml(stepName));
				stepsData.add(data);
			}
			return stepsData;
		}
	}

	protected void addExtraTemplateParameters(Map<String, Object> parameters)
	{
	}

	protected Logger getLogger()
	{
		return logger;
	}
}
