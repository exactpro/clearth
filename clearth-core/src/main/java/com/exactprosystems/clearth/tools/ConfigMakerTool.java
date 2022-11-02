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

package com.exactprosystems.clearth.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.generator.ActionReader;
import com.exactprosystems.clearth.automation.generator.CsvActionReader;
import com.exactprosystems.clearth.automation.generator.XlsActionReader;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by alexander.magomedov on 11/7/16.
 */
public class ConfigMakerTool
{
	private static final Logger logger = LoggerFactory.getLogger(ConfigMakerTool.class);
	protected static final String stepColumn = ActionGenerator.HEADER_DELIMITER + ActionGenerator.COLUMN_GLOBALSTEP;
	public static final String CSV_EXT ="csv";
	public static final String XLS_EXT = "xls";
	public static final String XLSX_EXT = "xlsx";
	public static final Pattern EXTENSION_FILTER = Pattern.compile("(.*\\.(csv|xls|xlsx)$)");

	public static boolean checkMatrixFileExtension(String matrixFilePath)
	{
		return EXTENSION_FILTER.matcher(matrixFilePath).matches();
	}

	@Deprecated
	public List<String> makeConfigAndApply(Scheduler selectedScheduler, File matrixFile, File destDir)
			throws ClearThException
	{
		return makeConfigAndApply(selectedScheduler, matrixFile, destDir, false);
	}

	public List<String> makeConfigAndApply(Scheduler selectedScheduler, File matrixFile, File destDir, boolean append)
			throws ClearThException
	{
		if (selectedScheduler == null)
			throw new ClearThException("No scheduler selected");

		if(matrixFile == null)
			throw new ClearThException("No matrix file selected");
		if(!checkMatrixFileExtension(matrixFile.getName()))
			throw new ClearThException("Unsupported matrix file format");

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
			selectedScheduler.uploadSteps(configFile, configFile.getName(), warnings, append);
			if (warnings.isEmpty())
				logger.debug("Scheduler configuration uploaded. Scheduler: {}; matrix file: {}",
						selectedScheduler.getName(), matrixFile.getName());
			else
				logger.warn("Scheduler configuration uploaded with errors:{}{}", Utils.EOL,
						StringUtils.join(warnings, Utils.EOL));

			return warnings;
		}
		catch (Exception e)
		{
			String msg = String.format("An error occurred while uploading scheduler configuration from file '%s'",
					matrixFile.getName());
			logger.error(msg, e);
			throw new ClearThException(msg, e);
		}
	}

	private ActionReader getActionReader(File matrixFile) throws IOException
	{
		if(FilenameUtils.getExtension(matrixFile.getName()).equalsIgnoreCase(CSV_EXT) )
		{
			return new CsvActionReader(matrixFile.getAbsolutePath(), true);
		}
		else if ( FilenameUtils.getExtension(matrixFile.getName()).equalsIgnoreCase(XLS_EXT)
				|| FilenameUtils.getExtension(matrixFile.getName()).equalsIgnoreCase(XLSX_EXT))
		{
			return new XlsActionReader(matrixFile.getAbsolutePath(), true);
		}
		return null;
	}

	private List<String> readMatrixFile(File matrixFile) throws IOException
	{
		ActionReader reader = getActionReader(matrixFile);
		if (reader == null) return null;
		List<String> stepsNames = new ArrayList<String>();
		int headerColumn = -1;
		try
		{
			while (reader.readNextLine())
			{
				String[] values = reader.parseLine(false).toArray(new String[0]);
				if (reader.isCommentLine())
					continue;
				else if (reader.isHeaderLine())
				{
					headerColumn = -1;
					for (int i = 0; i < values.length; i++)
					{
						if (values[i].equalsIgnoreCase(stepColumn))
						{
							headerColumn = i;
							break;
						}
					}
				}
				else if (headerColumn > -1)
				{
					if (values.length <= headerColumn)
						continue;
					if ((!values[headerColumn].trim().isEmpty()) &&
							(!stepsNames.contains(values[headerColumn])))
					{
						stepsNames.add(values[headerColumn]);
					}
				}
			}
		}
		finally
		{
			reader.close();
		}
		return stepsNames;
	}

	public File makeConfig (File matrixFile, File destDir, String resultConfigName) throws
			IOException, ClearThException
	{
		if (matrixFile == null || !matrixFile.exists())
			throw new ClearThException("Matrix file does not exist!");
		if(!checkMatrixFileExtension(matrixFile.getName()))
			throw new ClearThException("Unsupported matrix file format");


		String[] configHeader;
		List<Scheduler> schedulers = ClearThCore.getInstance().getSchedulersManager().getCommonSchedulers();
		StepFactory stepFactory;
		if (schedulers != null && !schedulers.isEmpty())
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
		List<String> stepsNames = null;
		stepsNames = readMatrixFile(matrixFile);

		if (stepsNames == null || stepsNames.isEmpty())
			throw new ClearThException("No steps references found in uploaded file " + resultConfigName +
					". Please make sure that you upload a script file with right extension");

		List<Step> steps = new ArrayList<Step>();
		for (String stepName : stepsNames)
		{
			steps.add(createStep(stepName, stepFactory));
		}

		File configFile = File.createTempFile(resultConfigName + "_config_", ".cfg", destDir);
		SchedulerData.saveSteps(configFile, configHeader, steps);

		return configFile;
	}

	protected Step createStep (String stepName, StepFactory stepFactory)
	{
		if (stepFactory != null)
			return stepFactory.createStep(stepName, CoreStepKind.Default.getLabel(), "", StartAtType.DEFAULT,
					false,
					"", false, false, true, "");
		return new DefaultStep(stepName, CoreStepKind.Default.getLabel(), "", StartAtType.DEFAULT, false, "", false,
				false, true, "");
	}
}