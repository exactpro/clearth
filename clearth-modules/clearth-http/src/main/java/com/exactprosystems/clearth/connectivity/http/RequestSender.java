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
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import org.apache.http.ParseException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.util.Map;

import static org.apache.http.client.protocol.HttpClientContext.COOKIE_STORE;

public class RequestSender
{
	private final String httpRequestType;
	private final String parentUrl;

	private final ClearThHttpClient client;
	private final HttpDictionary dictionary;
	private final MatrixFunctions matrixFunctions;
	private final HttpRequestSettings settings;

	private final Map<String, String> requestFields;

	public RequestSender(String httpRequestType, String parentUrl,
	                     ClearThHttpClient client, HttpDictionary dictionary,
	                     MatrixFunctions matrixFunctions, Map<String, String> requestFields,
	                     HttpRequestSettings settings)
	{
		this.httpRequestType = httpRequestType;
		this.parentUrl = parentUrl;
		this.client = client;
		this.dictionary = dictionary;
		this.matrixFunctions = matrixFunctions;
		this.requestFields = requestFields;
		this.settings = settings;
	}

	public RequestSenderResult sendRequest() 
	{
		try
		{
			HttpUriRequest request = createRequest();

			try
			{
				long requestTime = System.currentTimeMillis();
				CloseableHttpResponse response = client.send(request, settings, false);
				long responseTime = System.currentTimeMillis();

				return createResponseData(response, request, client, requestTime, responseTime, null, null);
			}
			catch (IOException ex)
			{
				String message = "Error occurred while sending request.";
				return createResponseData(null, request, client, 0, 0,
						new ConnectivityException(message, ex), message);
			}
		}
		catch (ConnectivityException ex)
		{
			return createResponseData(null, null, client, 0, 0, ex, 
					"Error occurred while creating Http Request.");
		}
		
	}

	private HttpUriRequest createRequest() throws ConnectivityException
	{
		ClearThUriRequestBuilder httpBuilder = new ClearThUriRequestBuilder(dictionary, httpRequestType);

		if (matrixFunctions != null)
			httpBuilder.usingFunctionsForExpressions(matrixFunctions);

		String encodedBody = settings.getBody();
		if(encodedBody != null)
			httpBuilder.allowBlankBody(true)
					.encodedBody(encodedBody);

		String absoluteUrl = settings.getUrl();
		if(absoluteUrl != null)
			httpBuilder.absoluteUrl(absoluteUrl);
		else
			httpBuilder.rootUrl(parentUrl);

		httpBuilder.requestFields(requestFields);

		return httpBuilder.build();
	}

	private RequestSenderResult createResponseData(CloseableHttpResponse response, HttpUriRequest request, 
	                                               ClearThHttpClient client, long requestTimestamp, 
	                                               long responseTimestamp, Exception exception,
	                                               String errorMessage) throws ParseException
	{
		CookieStore cookieStore = (CookieStore) client.getLocalContext().getAttribute(COOKIE_STORE);
		return new RequestSenderResult(requestTimestamp, responseTimestamp, request, 
				response, cookieStore, exception, errorMessage);
	}

	public static HttpRequestSettings createRequestSettings(String user, String password)
	{
		HttpRequestSettings settings = new HttpRequestSettings();

		settings.setUser(user);
		settings.setPassword(password);

		return settings;
	}

}
