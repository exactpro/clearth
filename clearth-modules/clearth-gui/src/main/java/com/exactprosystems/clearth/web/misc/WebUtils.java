/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.context.PrimeFacesContext;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;

import javax.activation.MimetypesFileTypeMap;
import javax.faces.context.FacesContext;
import java.io.*;

public class WebUtils
{
	public static String getContext()
	{
		String result = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath();
		if (result == null)
			result = "";
		if (StringUtils.isNotEmpty(result) && !result.startsWith("/"))
			return "/"+result;
		return result;
	}
	
	public static File storeUploadedFile(UploadedFile uploadedFile, File storageDir, String prefix, String suffix)
			throws IOException
	{
		InputStream input = null;
		OutputStream output = null;
		try
		{
			File result = File.createTempFile(prefix, suffix, storageDir);
			
			input = uploadedFile.getInputStream();
			output = new FileOutputStream(result);
			
			IOUtils.copy(input, output);
			return result;
		}
		finally
		{
			Utils.closeResource(input);
			Utils.closeResource(output);
		}
	}
	
	public static StreamedContent downloadFile(File file, String fileName, String warnMess) throws FileNotFoundException
	{
		if (file == null || !file.exists())
		{
			MessageUtils.addWarningMessage("Nothing to download", warnMess);
			return null;
		}

		@SuppressWarnings("resource")
		InputStream stream = new FileInputStream(file);
		return DefaultStreamedContent.builder()
				.stream(() -> stream)
				.contentType(new MimetypesFileTypeMap().getContentType(file))
				.name(fileName)
				.build();
	}

	public static StreamedContent downloadFile(File file) throws FileNotFoundException
	{
		return downloadFile(file, "File does not exist");
	}

	public static StreamedContent downloadFile(File file, String warnMess) throws FileNotFoundException
	{
		String fileName = file != null ? file.getName() : null;
		return downloadFile(file, fileName, warnMess);
	}

	public static boolean addCanCloseCallback(boolean canClose)
	{
		PrimeFaces context = PrimeFaces.current();
		if (context != null)
		{
			context.ajax().addCallbackParam("canClose", canClose);
			return true;
		}
		else
		{
			MessageUtils.addErrorMessage("RequestContext is null", "RequestContext malfunction detected, window cannot be closed automatically");
			return false;
		}
	}

	public static File getLogsDir()
	{
		return new File(ClearThCore.getInstance().getLogsPath());
	}
	
	public static void logAndGrowlException(String errMsg, Throwable e, Logger logger)
	{
		logger.error(errMsg, e);
		MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
	}
	
	public static String getMimeType(String fileName)
	{
		return PrimeFacesContext.getCurrentInstance().getExternalContext().getMimeType(fileName);
	}
}
