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

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;

public class HttpResponseData
{
	private int code;
	private String response;
	private long requestTimestamp;
	private long responseTimestamp;
	private CookieStore cookieStore;
	private HttpUriRequest request;
	
	public HttpResponseData(int code, String response, long requestTimestamp, long responseTimestamp,
	                        CookieStore cookieStore, HttpUriRequest request)
	{
		this.code = code;
		this.response = response;
		this.requestTimestamp = requestTimestamp;
		this.responseTimestamp = responseTimestamp;
		this.cookieStore = cookieStore;
		this.request = request;
	}
	

	public int getCode()
	{
		return code;
	}
	
	public void setCode(int code)
	{
		this.code = code;
	}
	
	
	public String getResponse()
	{
		return response;
	}

	public void setResponse(String response)
	{
		this.response = response;
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

	public CookieStore getCookieStore()
	{
		return cookieStore;
	}

	public void setCookieStore(CookieStore cookieStore)
	{
		this.cookieStore = cookieStore;
	}

	public HttpUriRequest getRequest()
	{
		return request;
	}

	public void setRequest(HttpUriRequest request)
	{
		this.request = request;
	}
}
