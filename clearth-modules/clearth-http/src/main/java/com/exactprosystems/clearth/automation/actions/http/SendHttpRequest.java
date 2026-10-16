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

package com.exactprosystems.clearth.automation.actions.http;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.http.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.RedirectException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.Utils;

import static com.exactprosystems.clearth.utils.inputparams.InputParamsUtils.*;
import static org.apache.http.client.protocol.HttpClientContext.COOKIE_STORE;

public abstract class SendHttpRequest<T extends ClearThMessage<T>> extends MessageAction<T>
{
	public static final String PARAM_PARENTURL = "ParentURL",
			PARAM_USER = "User",
			PARAM_PASSWORD = "Password",
			PARAM_REQUEST_BODY = "RequestBody",
			PARAM_HTTP_REQUEST_TYPE = "HttpRequestType",
			PARAM_SHOW_REQUEST_IN_REPORT = "ShowRequestInReport",
			PARAM_SHOW_BODY_IN_REPORT = "ShowBodyInReport";
	public static final Set<String> SERVICE_PARAMETERS = new HashSet<String>(Arrays.asList(
			PARAM_PARENTURL,
			PARAM_USER,
			PARAM_PASSWORD,
			PARAM_REQUEST_BODY,
			PARAM_HTTP_REQUEST_TYPE,
			PARAM_SHOW_REQUEST_IN_REPORT,
			PARAM_SHOW_BODY_IN_REPORT));
	
	private static final int MAX_OUTPUT = 1024;
	
	private StepContext stepContext;
	private MatrixContext matrixContext;
	private GlobalContext globalContext;

	protected HttpUriRequest request;
	
	protected abstract String getDefaultParentUrl();
	protected abstract String getDefaultUser();
	protected abstract String getDefaultPassword();
	protected abstract String prepareMessage(HttpRequestSettings settings) throws ResultException;
	protected abstract HttpDictionary getHttpDictionary();
	protected abstract ClearThHttpClient getHttpClient() throws ResultException;
	protected abstract void closeResourcesAfterSending(ClearThHttpClient client, CloseableHttpResponse response);
	
	
	public static String getResponseDataContextName(String actionId)
	{
		return SendHttpRequest.class.getSimpleName()+"_response_"+actionId;
	}
	
	
	@Override
	public boolean isIncoming()
	{
		return false;
	}
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		this.stepContext = stepContext;
		this.matrixContext = matrixContext;
		this.globalContext = globalContext;

		boolean showRequestInReport = getBooleanOrDefault(inputParams, PARAM_SHOW_REQUEST_IN_REPORT, true);
		boolean showBodyInReport = getBooleanOrDefault(inputParams, PARAM_SHOW_BODY_IN_REPORT, true);

		configureAction();

		HttpRequestSettings settings = createRequestSettings();
		
		try
		{
			String messageToSend = prepareMessage(settings);
			settings.setBody(messageToSend);
			
			HttpResponseData response = sendRequest(settings, 1);
			responseToLog(response);
			storeResponse(response);

			return createResult(response, messageToSend, settings, showRequestInReport, showBodyInReport);
		}
		catch (Exception e)
		{
			HttpRequestResultBuilder builder = new HttpRequestResultBuilder();
			Result result = builder.makeHttpRequestResult(request, null, showBodyInReport);
			result.setError(e);
			return result;
		}
	}

	protected void configureAction()
	{
		// Method to do some checks/initializations before starting the execution of action logic
	}

	@Override
	protected Set<String> getServiceParameters()
	{
		return SERVICE_PARAMETERS;
	}
	
	protected HttpRequestSettings createRequestSettings() throws ResultException
	{
		String user = getStringOrDefault(inputParams, PARAM_USER, getDefaultUser());
		String password = getStringOrDefault(inputParams, PARAM_PASSWORD, getDefaultPassword());

		HttpRequestSettings settings = new HttpRequestSettings();

		settings.setUser(user);
		settings.setPassword(password);

		return settings;
	}
	
	
	protected HttpResponseData sendRequest(HttpRequestSettings settings, int iteration) throws ResultException
	{
		try
		{
			ClearThHttpClient client = getHttpClient(settings);
			request = createRequest(settings);
			logger.debug("Sending request {}", request.getRequestLine());

			CloseableHttpResponse response = null;
			try
			{
				long requestTime = System.currentTimeMillis();
				response = client.send(request, settings, false);
				long responseTime = System.currentTimeMillis();
				if (response.getStatusLine().getStatusCode() == 302)  //Handling redirect request
				{
					String header = client.findLocationHeader(response.getAllHeaders());
					if (header != null)
						throw new RedirectException(header);
				}
				
				return createResponseData(response, request, client, requestTime, responseTime);
			}
			finally
			{
				closeResourcesAfterSending(client, response);
			}
		}
		catch (RedirectException e)
		{
			if (iteration >= 100)
				throw ResultException.failed("Too many redirects requested. Looks like infinite loop. Skipped last redirect requested to "+e.getMessage());
			
			HttpRequestSettings newSettings = settings.copy();
			newSettings.setUrl(e.getMessage());
			return sendRequest(newSettings, iteration+1);
		}
		catch (ResultException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw ResultException.failed("Error while sending request", e);
		}
	}
	
	protected void responseToLog(HttpResponseData response)
	{
		if (!logger.isTraceEnabled())
			return;
			
		String r = response.getResponse(),
				toPrint;
		if (r.length() > MAX_OUTPUT)
			toPrint = r.substring(0, MAX_OUTPUT)+"...";
		else
			toPrint = r;

		logger.trace("Request sent. Response:" + Utils.EOL + "{}", toPrint);
	}
	
	protected void storeResponse(HttpResponseData response)
	{
		matrixContext.setContext(getResponseDataContextName(getIdInMatrix()), response);
	}
	
	protected Result processResponse(HttpResponseData response, String messageToSend, HttpRequestSettings settings)
	{
		return null;   //By default, no additional processing required and action is treated as passed
	}
	
	protected Result createResult(HttpResponseData response, String messageToSend, HttpRequestSettings settings,
	                              boolean showRequestInReport, boolean showBodyInReport)
	{
		ContainerResult containerResult = new ContainerResult();
		Result processedResult = processResponse(response, messageToSend, settings);
		if (processedResult != null)
		{
			containerResult.addDetail(processedResult);
			containerResult.setComment(processedResult.getComment());
			containerResult.setSuccess(processedResult.isSuccess());
		}

		if (showRequestInReport)
		{
			HttpRequestResultBuilder builder = new HttpRequestResultBuilder();
			Result requestResult = builder.makeHttpRequestResult(response.getRequest(),
					response.getCookieStore(),
					showBodyInReport);
			containerResult.addDetail(requestResult);
		}
		return containerResult;
	}	
	
	protected ClearThHttpClient getHttpClient(HttpRequestSettings settings)
	{
		ClearThHttpClient client = getHttpClient();
		client.setHost(settings.getUrl());
		return client;
	}
	
	protected HttpUriRequest createRequest(HttpRequestSettings settings) throws ConnectivityException
	{
		HttpDictionary dictionary = getHttpDictionary();

		ClearThUriRequestBuilder httpBuilder = new ClearThUriRequestBuilder(dictionary, getHttpRequestType());

		if (globalContext != null)
			httpBuilder.usingFunctionsForExpressions(globalContext.getMatrixFunctions());

		String encodedBody = settings.getBody();
		if(encodedBody != null)
			httpBuilder.allowBlankBody(true)
					.encodedBody(encodedBody);

		String absoluteUrl = settings.getUrl();
		if(absoluteUrl != null)
			httpBuilder.absoluteUrl(absoluteUrl);
		else
			httpBuilder.rootUrl(getParentUrl());

		httpBuilder.requestFields(inputParams);

		return httpBuilder.build();
	}

	protected String getParentUrl()
	{
		return getStringOrDefault(getInputParams(), PARAM_PARENTURL, getDefaultParentUrl());
	}

	protected HttpResponseData createResponseData(HttpResponse response, HttpUriRequest request,
	                                              ClearThHttpClient client, long requestTimestamp,
	                                              long responseTimestamp) throws ParseException, IOException
	{
		HttpEntity entity = response.getEntity();
		CookieStore cookieStore = (CookieStore) client.getLocalContext().getAttribute(COOKIE_STORE);
		return new HttpResponseData(response.getStatusLine().getStatusCode(), 
				entity != null ? EntityUtils.toString(entity) : "", requestTimestamp, responseTimestamp, cookieStore,
				request);
	}
	
	
	protected StepContext getStepContext()
	{
		return stepContext;
	}
	
	protected MatrixContext getMatrixContext()
	{
		return matrixContext;
	}
	
	protected GlobalContext getGlobalContext()
	{
		return globalContext;
	}

	protected String getHttpRequestType()
	{
		return getRequiredString(getInputParams(), PARAM_HTTP_REQUEST_TYPE);
	}

	@Override
	public void dispose()
	{
		super.dispose();
		request = null;
	}
}