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

package com.exactprosystems.clearth.web.misc;

import com.csvreader.CsvReader;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class StepUploadHandler
{
	private static final Logger logger = LoggerFactory.getLogger(StepUploadHandler.class);
	
	public static final String CSV = "csv", CFG = "cfg", VALID_MIME_TYPE_STEPS_CFG = "text/", ERROR = "Error";
	
	public static void uploadSteps(FileUploadEvent event, String mimeType, Scheduler scheduler, boolean appendSteps)
	{
		UploadedFile uploadedFile = event.getFile();
		if (!isValidStepConfig(uploadedFile, mimeType))
			return;
		
		String uploadedFilename = uploadedFile.getFileName();
		File storedFile = null;
		try
		{
			storedFile = WebUtils.storeUploadedFile(uploadedFile, new File(ClearThCore.automationStoragePath()), "scconfig_", "." + CSV);
			if (!checkStepConfigHeader(uploadedFilename, storedFile))
			{
				deleteStoredFile(storedFile);
				return;
			}
			
			List<String> warnings = new ArrayList<>();
			scheduler.uploadSteps(storedFile, uploadedFilename, warnings, appendSteps);
			checkUploadWarnings(warnings);
			logger.info("Uploaded configuration '{}' for scheduler '{}' with {} steps", uploadedFilename,
					scheduler.getName(), appendSteps ? "append" : "apply");
		}
		catch (Exception e)
		{
			String message = String.format("Error occurred while working with scheduler configuration from file '%s'", uploadedFilename);
			logger.error(message, e);
			MessageUtils.addErrorMessage(ERROR, message + ": " + e.getMessage());
			
			if (storedFile != null)
				deleteStoredFile(storedFile);
		}
	}
	
	
	public static boolean isValidStepConfig(UploadedFile file, String mimeType)
	{
		if (file == null || file.getContent().length == 0)
		{
			String message = "Steps configuration file doesn't exist or empty";
			logger.error(message);
			MessageUtils.addErrorMessage(ERROR, message);
			return false;
		}
		
		boolean isCsvOrCfg = FilenameUtils.isExtension(file.getFileName().toLowerCase(), new String[] { CSV, CFG }),
				isValidContent = mimeType != null && mimeType.startsWith(VALID_MIME_TYPE_STEPS_CFG);
		if (!isValidContent || !isCsvOrCfg)
		{
			String message = String.format("Invalid file type for steps configuration file. Valid ones are: '%s', '%s'", CFG, CSV);
			logger.error(message);
			MessageUtils.addErrorMessage(ERROR, message);
			return false;
		}
		return true;
	}
	
	public static boolean checkStepConfigHeader(String uploadedFilename, File storedFile) throws IOException
	{
		List<String> undefinedFields = readConfigHeaders(storedFile.getAbsolutePath());
		if (!undefinedFields.isEmpty())
		{
			String message = String.format("File '%s' doesn't contain the following header parameters: %s", uploadedFilename, undefinedFields);
			logger.error(message);
			MessageUtils.addErrorMessage(ERROR, message);
			return false;
		}
		return true;
	}
	
	private static void checkUploadWarnings(List<String> warnings)
	{
		if (warnings.isEmpty())
			MessageUtils.addInfoMessage("Success", "Scheduler configuration uploaded.");
		else
		{
			String separator = "\r\n<br/><li>";
			MessageUtils.addWarningMessage("Warning", "Scheduler configuration uploaded with errors:"
					+ separator + StringUtils.join(warnings, separator));
		}
	}
	
	
	public static List<String> readConfigHeaders(String storedFilePath) throws IOException
	{
		CsvReader reader = null;
		List<String> undefinedFields = new ArrayList<>();
		try
		{
			reader = new CsvReader(storedFilePath);
			reader.setSafetySwitch(false);
			reader.readHeaders();

			List<String> header = Arrays.asList(reader.getHeaders());
			Set<Step.StepParams> excludedParams = getExcludedParams();
			for (Step.StepParams param : Step.StepParams.values())
			{
				if (!header.contains(param.getValue()) && !excludedParams.contains(param))
					undefinedFields.add(param.getValue());
			}
		}
		finally
		{
			Utils.closeResource(reader);
		}
		return undefinedFields;
	}
	
	private static Set<Step.StepParams> getExcludedParams()
	{
		return new HashSet<Step.StepParams>()
		{
			{
				add(Step.StepParams.START_AT_TYPE);
				add(Step.StepParams.WAIT_NEXT_DAY);
				add(Step.StepParams.COMMENT);
				add(Step.StepParams.ASK_IF_FAILED);
				add(Step.StepParams.STARTED);
				add(Step.StepParams.ACTIONS_SUCCESSFUL);
				add(Step.StepParams.FINISHED);
			}
		};
	}
	
	private static void deleteStoredFile(File storedFile)
	{
		try
		{
			Files.delete(storedFile.toPath());
			logger.info("Stored file '{}' has been deleted", storedFile.getAbsolutePath());
		}
		catch (IOException e)
		{
			logger.warn("Error while deleting stored file '{}'", storedFile.getAbsolutePath(), e);
		}
	}
}
