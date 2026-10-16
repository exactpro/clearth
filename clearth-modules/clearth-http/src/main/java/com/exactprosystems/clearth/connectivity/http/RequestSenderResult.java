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

import com.exactprosystems.clearth.utils.Utils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

public class RequestSenderResult implements AutoCloseable
{
	private long requestTimestamp;
	private long responseTimestamp;
	private HttpUriRequest request;
	private CloseableHttpResponse response;
	private CookieStore cookieStore;
	private Exception exception;
	private String errorMessage;

	public RequestSenderResult(long requestTimestamp, long responseTimestamp,
	                           HttpUriRequest request, CloseableHttpResponse response,
	                           CookieStore cookieStore, Exception exception, String errorMessage)
	{
		this.requestTimestamp = requestTimestamp;
		this.responseTimestamp = responseTimestamp;
		this.request = request;
		this.response = response;
		this.cookieStore = cookieStore;
		this.exception = exception;
		this.errorMessage = errorMessage;
	}

	public long getRequestTimestamp()
	{
		return requestTimestamp;
	}

	public void setRequestTimestamp(long requestTimestamp)
	{
		this.requestTimestamp = requestTimestamp;
	}

	public long getResponseTimestamp()
	{
		return responseTimestamp;
	}

	public void setResponseTimestamp(long responseTimestamp)
	{
		this.responseTimestamp = responseTimestamp;
	}

	public HttpUriRequest getRequest()
	{
		return request;
	}

	public void setRequest(HttpUriRequest request)
	{
		this.request = request;
	}

	public CloseableHttpResponse getResponse()
	{
		return response;
	}

	public void setResponse(CloseableHttpResponse response)
	{
		this.response = response;
	}

	public CookieStore getCookieStore()
	{
		return cookieStore;
	}

	public void setCookieStore(CookieStore cookieStore)
	{
		this.cookieStore = cookieStore;
	}

	public Exception getException()
	{
		return exception;
	}

	public void setException(Exception exception)
	{
		this.exception = exception;
	}
	
	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	@Override
	public void close()
	{
		Utils.closeResource(response);
	}
}
