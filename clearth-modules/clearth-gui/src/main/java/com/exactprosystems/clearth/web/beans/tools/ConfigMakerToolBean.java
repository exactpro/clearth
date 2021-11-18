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

package com.exactprosystems.clearth.web.beans.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.tools.ConfigMakerTool;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static java.lang.String.format;


/**
 * Created by alexander.magomedov on 10/31/16.
 */
public class ConfigMakerToolBean extends ClearThBean
{
	protected ConfigMakerTool configMakerTool;
	protected static final File destDir = new File(ClearThCore.tempPath());
	protected static final File uploadStorage = new File(ClearThCore.uploadStoragePath());
	protected UploadedFile file = null;
	protected Scheduler selectedScheduler;


	@PostConstruct
	public void init()
	{
		configMakerTool = ClearThCore.getInstance().getToolsFactory().createConfigMakerTool();
	}

	public void makeConfigAndApply()
	{
		if (file == null) {
			MessageUtils.addWarningMessage("No file selected", "Please select a script file (matrix)!");
			return;
		}

		try
		{
			File storedUploadedFile = storeUploadedFile();
			if (storedUploadedFile == null) return;
			List<String> warnings = configMakerTool.makeConfigAndApply(selectedScheduler, storedUploadedFile, destDir);
			handleWarnings(warnings);
		}
		catch (ClearThException e)
		{
			handleException(e);
		}
	}

	public StreamedContent makeConfigAndDownload()
	{
		if (file == null) {
			MessageUtils.addWarningMessage("No file selected", "Please select a script file (matrix)!");
			return null;
		}
		
		try
		{
			File storedUploadedFile = storeUploadedFile();
			if (storedUploadedFile == null)
				return null;
			File configFile = configMakerTool.makeConfig(storedUploadedFile, destDir, storedUploadedFile.getName());
			return new DefaultStreamedContent(new FileInputStream(configFile), new MimetypesFileTypeMap().getContentType(configFile), configFile.getName());
		}
		catch (Exception e)
		{
			handleException(e);
			return null;
		}
	}
	
	public static void handleWarnings(List<String> warnings)
	{
		if (CollectionUtils.isEmpty(warnings))
			MessageUtils.addInfoMessage("Success", "Scheduler configuration uploaded");
		else
			MessageUtils.addWarningMessage("Warning",
					"Scheduler configuration uploaded with errors:" + Utils.EOL + "<ul><li>" + StringUtils.join(warnings, "</li><li>") + "</li></ul>");
	}

	public static void handleException(Exception e)
	{
		String summary = e.getMessage();

		String cause = null;
		if (e.getCause() != null)
			cause = e.getCause().getMessage();

		if (cause == null)
		{
			summary = "Error";
			cause = e.getMessage();
		}

		MessageUtils.addErrorMessage(summary, cause);
	}


	private File storeUploadedFile() throws ClearThException
	{
		try
		{
			final File storedUploadedFile;
			String fileExtension = '.' + FilenameUtils.getExtension(file.getFileName());
			if (!ConfigMakerTool.EXTENSION_FILTER.matcher(fileExtension).matches())
			{
				MessageUtils.addErrorMessage("Error",
						format("Matrix file format must be .%s, .%s or .%s",ConfigMakerTool.CSV_EXT,
								ConfigMakerTool.XLS_EXT,ConfigMakerTool.XLSX_EXT));
				return null;
			}
			storedUploadedFile =
					WebUtils.storeUploadedFile(file, uploadStorage, "matrixforconfig_", fileExtension);
			MessageUtils.addInfoMessage("Success", "File " + storedUploadedFile.getName() + " uploaded");
			return storedUploadedFile;
		}
		catch (Exception e)
		{
			getLogger().error("error while uploading matrix file", e);
			throw new ClearThException("Error occurred while uploading matrix file", e);
		}
	}

	public String getSelectedScheduler()
	{
		return selectedScheduler != null ? selectedScheduler.getName() : "";
	}

	public void setSelectedScheduler(String selectedScheduler)
	{
		this.selectedScheduler = ClearThCore.getInstance().getSchedulersManager().getSchedulerByName(selectedScheduler, UserInfoUtils.getUserName());
	}

	public List<String> getSchedulers()
	{
		return ClearThCore.getInstance().getSchedulersManager().getAvailableSchedulerNames(UserInfoUtils.getUserName());
	}

	public UploadedFile getFile()
	{
		return this.file;
	}

	public void setFile(UploadedFile file)
	{
		this.file = file;
	}
}