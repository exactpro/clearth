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
package com.exactprosystems.clearth.automation.actions.http;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.connectivity.http.*;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static com.exactprosystems.clearth.utils.inputparams.InputParamsUtils.*;

public abstract class DownloadFile extends Action
{
	public static final String PARAM_ROOT_URL = "RootURL";
	public static final String PARAM_USER = "User";
	public static final String PARAM_PASSWORD = "Password";
	public static final String PARAM_HTTP_REQUEST_TYPE = "HttpRequestType";
	public static final String PARAM_REQUEST_BODY = "RequestBody";
	public static final String PARAM_SHOW_REQUEST_IN_REPORT = "ShowRequestInReport";
	public static final String PARAM_SHOW_BODY_IN_REPORT = "ShowBodyInReport";
	public static final String PARAM_OUTPUT_PATH = "OutputPath";
	
	protected abstract String getDefaultRootUrl();
	protected abstract String getDefaultUser();
	protected abstract String getDefaultPassword();
	protected abstract HttpDictionary getHttpDictionary();
	protected abstract ClearThHttpClient getHttpClient();
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException
	{

		boolean showRequestInReport = getBooleanOrDefault(inputParams, PARAM_SHOW_REQUEST_IN_REPORT, true);
		boolean showBodyInReport = getBooleanOrDefault(inputParams, PARAM_SHOW_BODY_IN_REPORT, true);

		String user = getStringOrDefault(inputParams, PARAM_USER, getDefaultUser());
		String password = getStringOrDefault(inputParams, PARAM_PASSWORD, getDefaultPassword());
		
		HttpRequestSettings settings = RequestSender.createRequestSettings(user, password);
		
		String body = inputParams.get(PARAM_REQUEST_BODY);
		
		settings.setBody(body);
		
		String outputPath = getOutputPath();
		
		ClearThHttpClient client =  getHttpClient(settings);

		FileDownloader downloader = new FileDownloader(outputPath, getHttpRequestType(),
				getRootUrl(), client, getHttpDictionary(),
				getMatrixFunctions(globalContext), inputParams, settings);

		try (RequestSenderResult response = downloader.downloadFile())
		{
			Exception exception = response.getException();
			String errorMessage = response.getErrorMessage();
			if (exception != null || errorMessage != null)
			{
				HttpRequestResultBuilder builder = new HttpRequestResultBuilder();
				Result result = builder.makeHttpRequestResult(response.getRequest(), null, showBodyInReport);
				result.setSuccess(false);
				if(exception != null)
					result.setError(response.getException());
			
				if (response.getErrorMessage() != null)
					result.setComment(errorMessage);
				
				return result;
			}
			
			return createResult(response, showRequestInReport, showBodyInReport, outputPath);
		}
		finally
		{
			if(isNeedCloseClient())
				client.close();
		}
	}

	protected Result createResult(RequestSenderResult response, boolean showRequestInReport, 
	                              boolean showBodyInReport, String outputPath)
	{
		ContainerResult containerResult = new ContainerResult();

		int code = response.getResponse().getStatusLine().getStatusCode();
		String reasonPhrase = response.getResponse().getStatusLine().getReasonPhrase();
				
		containerResult.setSuccess(code >= 200 && code < 300);
		containerResult.setComment(String.format("%s : %s", code, reasonPhrase));

		if (showRequestInReport)
		{
			HttpRequestResultBuilder builder = new HttpRequestResultBuilder();
			Result requestResult = builder.makeHttpRequestResult(response.getRequest(),
					response.getCookieStore(),
					showBodyInReport);
			containerResult.addDetail(requestResult);
		}
		
		List<String> columns = Arrays.asList("Name", "Value");
		TableResult downloadedFilePathResult = new TableResult(null, columns, false);
		downloadedFilePathResult.addDetail(true, Arrays.asList("Downloaded file path", outputPath));
		addOutputParam("DownloadedFilePath", outputPath);

		containerResult.addDetail(downloadedFilePathResult);

		return containerResult;
	}

	protected ClearThHttpClient getHttpClient(HttpRequestSettings settings)
	{
		ClearThHttpClient client = getHttpClient();
		client.setHost(settings.getUrl());
		return client;
	}

	protected String getHttpRequestType()
	{
		return getRequiredString(getInputParams(), PARAM_HTTP_REQUEST_TYPE);
	}

	protected MatrixFunctions getMatrixFunctions(GlobalContext globalContext)
	{
		return globalContext.getMatrixFunctions();
	}

	protected String getRootUrl()
	{
		return getStringOrDefault(getInputParams(), PARAM_ROOT_URL, getDefaultRootUrl());
	}

	protected String getOutputPath()
	{
		return getFilePathOrDefault(getInputParams(), PARAM_OUTPUT_PATH, getDefaultOutputPath());
	}
	
	protected String getDefaultOutputPath()
	{
		String date = new SimpleDateFormat("yyMMddHHmmssSSS").format(System.currentTimeMillis());
		return Paths.get(ClearThCore.tempPath(), "DownloadedFile_" + this.getIdInMatrix() + "_" + date).toString();
	}
	
	protected boolean isNeedCloseClient()
	{
		return false;
	}
}
