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

package com.exactprosystems.clearth.web.api;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.thoughtworks.xstream.XStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ApiUtils
{
	public static final String HEADER_AUTH = "Authorization";
	private static final Logger logger = LoggerFactory.getLogger(ApiUtils.class);

	public static Response response(Response.Status status, XmlResponse details)
	{
		return Response.status(status).entity(details).build();
	}


	public static Response badRequest(String message, String error, Object data)
	{
		return response(Response.Status.BAD_REQUEST, new XmlResponse(message, error, data));
	}

	public static Response badRequest(String message, Object data)
	{
		return badRequest(message, null, data);
	}

	public static Response badRequest(String message)
	{
		return badRequest(message, null, null);
	}


	public static Response error(String message, String error, Object data)
	{
		return response(Response.Status.INTERNAL_SERVER_ERROR, new XmlResponse(message, error, data));
	}

	public static Response error(String message, Object data)
	{
		return error(message, null, data);
	}

	public static Response error(String message, String error)
	{
		return error(message, error, null);
	}

	public static Response errorWithLog(String message, Exception e, Logger logger)
	{
		logger.error(message, e);
		return error(message, ExceptionUtils.getDetailedMessage(e));
	}

	public static Response error(String message)
	{
		return error(message, null, null);
	}


	public static Response ok(XmlResponse details)
	{
		return Response.ok(details).build();
	}

	public static Response ok(String message, Object data)
	{
		return ok(new XmlResponse(message, null, data));
	}

	public static Response ok(String message)
	{
		return ok(new XmlResponse(message, null, null));
	}

	public static Response ok(Object data, Class clazz)
	{
		return Response.ok(new XmlResponse(null, null, data).toXml(new Class[]{clazz})).build();
	}

	public static Response ok(Object data, Class clazz, XStream xs)
	{
		return Response.ok(new XmlResponse(null, null, data).toXml(new Class[]{clazz}, xs)).build();
	}

	public static boolean getBooleanValue(String paramName, String newValue) throws ParametersException
	{
		if ("true".equalsIgnoreCase(newValue))
			return true;
		if ("false".equalsIgnoreCase(newValue))
			return false;
		throw new ParametersException(String.format("Parameter value '%s=%s' is not correct", paramName, newValue));
	}

	public static String[] getAuthorizationUser(String authHeader)
	{
		if (authHeader == null)
			return null;
		int index = authHeader.indexOf(' ');
		if (index > -1)
			authHeader = authHeader.substring(index + 1);
		byte[] userColonPass = DatatypeConverter.parseBase64Binary(authHeader);
		return new String(userColonPass).split(":");
	}

	public static Response fileResponse(File file)
	{
		if (file == null)
		{
			throw new WebApplicationException(ApiUtils.error("Could not send response file: null"));
		}
		return fileResponse(file.getAbsolutePath(), file.getName());
	}

	public static Response fileResponse(final String sourceFilePath, String resultName)
	{
		String relativePath = ClearThCore.getInstance().excludeRoot(sourceFilePath);
		File sourceFile = new File(sourceFilePath);
		if (!sourceFile.exists())
			return Response.status(404).entity("File not found: " + relativePath).build();
		
		return Response.ok(new StreamingOutput()
		{
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException
			{
				FileInputStream fis = null;
				try
				{
					//Names of directory and file should be constants obtained from Scheduler or Executor. Consider 
					// use of some method to obtain report path
					logger.debug("Reading file '{}'", sourceFilePath);
					fis = new FileInputStream(sourceFilePath);
					byte[] buffer = new byte[4096];
					int read;
					while ((read = fis.read(buffer)) > -1)
					{
						os.write(buffer, 0, read);
						os.flush();
					}
				}
				catch (IOException e)
				{
					logger.error("Error while reading file '{}'", sourceFilePath, e);
					throw new WebApplicationException(
							ApiUtils.error("Error while downloading file: ", relativePath));
				}
				finally
				{
					Utils.closeResource(fis);
				}
			}
		}).header("content-disposition", "attachment; filename = " + resultName).build();
	}
	
	public static File getZippedFiles(List<File> files, String namePrefix) throws WebApplicationException
	{
		try
		{
			File zipFile = File.createTempFile(namePrefix + "_", ".zip", new File(ClearThCore.tempPath()));
			FileOperationUtils.zipFiles(zipFile, files);
			return zipFile;
		}
		catch (IOException e)
		{
			String msg = "Error while zipping files";
			logger.error(msg, e);
			throw new WebApplicationException(ApiUtils.error(msg, e));
		}
	}
}
