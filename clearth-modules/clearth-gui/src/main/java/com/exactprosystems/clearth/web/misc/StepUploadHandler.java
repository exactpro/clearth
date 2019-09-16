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

package com.exactprosystems.clearth.web.misc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.csvreader.CsvReader;
import org.apache.commons.lang.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.utils.Utils;

public class StepUploadHandler {

	private static final Logger logger = LoggerFactory.getLogger(StepUploadHandler.class);

	public static final String CSV = ".csv", 
			CFG = ".cfg", 
			VALID_MIME_TYPE_STEPS_CFG = "text/";

	public static void uploadSteps(FileUploadEvent event, Scheduler scheduler, boolean appendSteps)
	{
		UploadedFile file = event.getFile();
		if((file == null) || (file.getContents().length == 0))
		{
			logger.error("Steps configuration file doesn't exist or empty");
			MessageUtils.addErrorMessage("Error", "Error occurred while working with scheduler configuration");
			return;
		}

		if(!isValidStepConfig(file))
		{
			String message = "Invalid file's type for steps configuration file. Valid type is: " + CFG + ", " + CSV;
			logger.error(message);
			MessageUtils.addErrorMessage("Error", message);
			return;
		}

		try
		{
			File storedFile = WebUtils.storeUploadedFile(file, new File(ClearThCore.automationStoragePath()), "scconfig_", CSV);

			List<String> headerErrors = checkStepConfigHeader(storedFile.getAbsolutePath());
			if(!headerErrors.isEmpty())
			{
				boolean success = storedFile.delete();
				logger.info(storedFile.getName() + " was " + (success ? "successfully" : "not") + " deleted.");

				String message = file.getFileName() + " doesn't contain following header params: " + headerErrors;
				logger.error(message);
				MessageUtils.addErrorMessage("Error", message);
				return;
			}

			List<String> warnings = new ArrayList<String>();
			scheduler.uploadSteps(storedFile, new File(file.getFileName()).getName(), warnings, appendSteps);
			if(warnings.isEmpty())
				MessageUtils.addInfoMessage("Success", "Scheduler configuration uploaded.");
			else
				MessageUtils.addWarningMessage("Warning",
											   "Scheduler configuration uploaded with errors:\r\n<br /><li>" + StringUtils.join(warnings,
																																"\r\n<br /><li>"));
			logger.info("uploaded configuration '" + file.getFileName() + "' for scheduler '" + scheduler.getName() + "' with " + (appendSteps ? "append" : "apply"));
		} catch (Exception e)
		{
			logger.error("Error while working with scheduler configuration from file " + file.getFileName(), e);
			MessageUtils.addErrorMessage("Error",
										 "Error occurred while working with scheduler configuration from file " + file.getFileName() + ": " + e.getMessage());
		}
	}

	public static boolean isValidStepConfig(UploadedFile file)
	{
		String fileName = file.getFileName().toLowerCase();

		if(!file.getContentType().toLowerCase().startsWith(VALID_MIME_TYPE_STEPS_CFG) && !fileName.endsWith(CSV) && !fileName.endsWith(CFG))
			return false;

		return true;
	}

	public static List<String> checkStepConfigHeader(String filename) throws IOException
	{
		CsvReader reader = null;

		try
		{
			reader = new CsvReader(filename);
			reader.setSafetySwitch(false);
			reader.readHeaders();

			List<String> header = Arrays.asList(reader.getHeaders());
			List<String> undefinedFields = new ArrayList<String>();

			for (Step.StepParams param : Step.StepParams.values())
			{
				if (!header.contains(param.getValue()) && !excludedParams().contains(param))
					undefinedFields.add(param.getValue());
			}
			return undefinedFields;
		} finally
		{
			Utils.closeResource(reader);
		}
	}

	private static Set<Step.StepParams> excludedParams()
	{
		return new HashSet<Step.StepParams>()
		{
			{
				add(Step.StepParams.COMMENT);
				add(Step.StepParams.ASK_IF_FAILED);
				add(Step.StepParams.START_AT_TYPE);
				add(Step.StepParams.WAIT_NEXT_DAY);
			}
		};
	}
}
