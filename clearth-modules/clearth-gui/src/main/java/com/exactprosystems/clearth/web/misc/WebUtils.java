/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.context.PrimeFacesContext;
import org.primefaces.context.RequestContext;
import org.primefaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.Utils;

public class WebUtils
{
	private static final Logger logger = LoggerFactory.getLogger(WebUtils.class);

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
			
			input = uploadedFile.getInputstream();
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

	public static void redirectToFile(String filePath)
	{
		ExternalContext ex = FacesContext.getCurrentInstance().getExternalContext();
		String context = WebUtils.getContext();
		try
		{
			ex.redirect(String.format("%s/download?file=%s", context, filePath));
		}
		catch (IOException e)
		{
			String errMsg = "Error while redirecting to file";
			logger.error(errMsg, e);
			MessageUtils.addErrorMessage(errMsg, ExceptionUtils.getDetailedMessage(e));
		}
	}
	
	public static boolean addCanCloseCallback(boolean canClose)
	{
		RequestContext context = RequestContext.getCurrentInstance();
		if (context != null)
		{
			PrimeFaces.current().ajax().addCallbackParam("canClose", canClose);
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

	public static String getURLParam(String url, String urlParam) throws Exception {
		final List<NameValuePair> params = URLEncodedUtils.parse(new URI(url), Charset.forName(Utils.UTF8));
		for (NameValuePair param : params) {
			if (param.getName().equals(urlParam))
				return param.getValue();
		}
		throw new Exception("URL '" + url + "' does not contain param '" + urlParam + "'");
	}

	public static void logAndGrowlException(String message, Exception e, Logger logger) {
		logger.error(message, e);
		MessageUtils.addErrorMessage(message, ExceptionUtils.getDetailedMessage(e));
	}
	
	public static String getMimeType(String fileName)
	{
		return PrimeFacesContext.getCurrentInstance().getExternalContext().getMimeType(fileName);
	}
}
