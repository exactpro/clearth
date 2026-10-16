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
package com.exactprosystems.clearth.connectivity.http;

import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.automation.actions.http.HttpRequestSettings;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class FileDownloader
{
	private static final Logger logger = LoggerFactory.getLogger(FileDownloader.class);
	
	private final String outputPath;

	private final RequestSender requestSender;

	public FileDownloader(String outputPath, String httpRequestType, String parentUrl,
	                      ClearThHttpClient client, HttpDictionary dictionary,
	                      MatrixFunctions matrixFunctions, Map<String, String> requestFields,
	                      HttpRequestSettings settings)
	{
		this.outputPath = outputPath;
		this.requestSender = new RequestSender(httpRequestType, parentUrl, client, dictionary, matrixFunctions,
				requestFields, settings);
	}

	public RequestSenderResult downloadFile()
	{
		RequestSenderResult responseData = requestSender.sendRequest();

		if (responseData.getException() != null)
			return responseData;

		HttpEntity entity = responseData.getResponse().getEntity();

		
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputPath)))
		{
			entity.writeTo(bos);
		}
		catch (IOException io)
		{
			String message = "Error occurred while downloading file.";
			responseData.close();
			responseData.setException(io);
			responseData.setErrorMessage(message);

			return responseData;
		}
		
		if(isEmptyFile())
		{
			deleteFile();
			responseData.setErrorMessage("Downloaded file is empty.");
		}

		return responseData;
	}
	
	private void deleteFile()
	{
		try
		{
			Files.delete(Paths.get(outputPath));
		}
		catch (IOException io)
		{
			logger.error("Can't delete temp file.", io);
		}
	}
	
	//Sometimes HttpEntity contains service information and will be written to disk as empty file.
	private boolean isEmptyFile()
	{
		File file = new File(outputPath);
		return file.length() == 0;
	}
}
