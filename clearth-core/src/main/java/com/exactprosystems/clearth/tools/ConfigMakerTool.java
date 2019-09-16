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

package com.exactprosystems.clearth.tools;

import com.csvreader.CsvReader;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexander.magomedov on 11/7/16.
 */
public class ConfigMakerTool
{
	private static final Logger logger = LoggerFactory.getLogger(ConfigMakerTool.class);
	protected static final String stepColumn = ActionGenerator.HEADER_DELIMITER + ActionGenerator.COLUMN_GLOBALSTEP;

	public List<String> makeConfigAndApply(Scheduler selectedScheduler, File matrixFile, File destDir) throws ClearThException
	{
		if (selectedScheduler == null)
			throw new ClearThException("No scheduler selected");

		File configFile;
		try
		{
			configFile = makeConfig(matrixFile, destDir, matrixFile.getName());
		}
		catch (Exception e)
		{
			String msg = "An error occurred while making configuration file";
			logger.error(msg, e);
			throw new ClearThException(msg, e);
		}

		try
		{
			List<String> warnings = new ArrayList<String>();
			selectedScheduler.uploadSteps(configFile, configFile.getName(), warnings, false);
			if (warnings.isEmpty())
				logger.debug("Scheduler configuration uploaded. Scheduler: {}; matrix file: {}", selectedScheduler.getName(), matrixFile.getName());
			else
				logger.warn("Scheduler configuration uploaded with errors:{}{}", Utils.EOL, StringUtils.join(warnings, Utils.EOL));

			return warnings;
		}
		catch (Exception e)
		{
			String msg = String.format("An error occurred while uploading scheduler configuration from file '%s'", matrixFile.getName());
			logger.error(msg, e);
			throw new ClearThException(msg, e);
		}
	}

	public File makeConfig(File matrixFile, File destDir, String resultConfigName) throws IOException, ClearThException
	{
		if (matrixFile == null || !matrixFile.exists())
			throw new ClearThException("Matrix file does not exists!");

		String[] configHeader;
		List<Scheduler> schedulers = ClearThCore.getInstance().getSchedulersManager().getCommonSchedulers();
		StepFactory stepFactory;
		if (!schedulers.isEmpty())
		{
			Scheduler sch = schedulers.get(0);
			configHeader = sch.getSchedulerData().getConfigHeader();
			stepFactory = sch.getStepFactory();
		}
		else
		{
			configHeader = DefaultSchedulerData.CONFIG_HEADER;
			stepFactory = null;
		}
		
		CsvReader reader = null;
		List<String> stepsNames = new ArrayList<String>();
		try
		{
			reader = new CsvReader(new FileReader(matrixFile));
			reader.setSafetySwitch(false);
			reader.setDelimiter(ActionGenerator.DELIMITER);
			reader.setTextQualifier(ActionGenerator.TEXT_QUALIFIER);
			int headerColumn = -1;
			while (reader.readRecord())
			{
				String[] values = reader.getValues();
				
				if (values[0].startsWith(ActionGenerator.COMMENT_INDICATOR))
					continue;
				else if (values[0].startsWith(ActionGenerator.HEADER_DELIMITER))
				{
					headerColumn = -1;
					for (int i = 0; i < values.length; i++)
						if (values[i].equalsIgnoreCase(stepColumn))
						{
							headerColumn = i;
							break;
						}
				}
				else if (headerColumn > -1)
				{
					if (values.length <= headerColumn)
						continue;
					if ((!values[headerColumn].trim().isEmpty()) && (!stepsNames.contains(values[headerColumn])))
					{
						stepsNames.add(values[headerColumn]);
					}
				}
			}
		}
		finally
		{
			Utils.closeResource(reader);
		}
		
		if (stepsNames.size() == 0)
			throw new ClearThException("No steps references found in uploaded file " + resultConfigName + ". Please make sure that you upload a script file");
		
		List<Step> steps = new ArrayList<Step>();
		for (String stepName : stepsNames)
			steps.add(createStep(stepName, stepFactory));
		
		File configFile = File.createTempFile(resultConfigName + "_config_", ".cfg", destDir);
		SchedulerData.saveSteps(configFile.getCanonicalPath(), configHeader, steps);
		
		return configFile;
	}
	
	protected Step createStep(String stepName, StepFactory stepFactory)
	{
		if (stepFactory != null)
			return stepFactory.createStep(stepName, CoreStepKind.Default.getLabel(), "", StartAtType.DEFAULT, false, "", false, false, true, "");
		return new DefaultStep(stepName, CoreStepKind.Default.getLabel(), "", StartAtType.DEFAULT, false, "", false, false, true, "");
	}
}
