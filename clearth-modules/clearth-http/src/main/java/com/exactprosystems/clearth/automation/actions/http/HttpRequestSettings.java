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

import java.util.List;
import java.util.Map;

import org.apache.http.client.protocol.HttpClientContext;

public class HttpRequestSettings implements Cloneable
{
	private String url, method, user, password, body;
	private Map<String, String> headers, parameters;
	private boolean namedParameters;
	private List<String> pathParameterNames;
	private HttpClientContext context;
	
	public HttpRequestSettings()
	{
	}
	
	public HttpRequestSettings(HttpRequestSettings copyFrom)
	{
		this.url = copyFrom.getUrl();
		this.method = copyFrom.getMethod();
		this.user = copyFrom.getUser();
		this.password = copyFrom.getPassword();
		this.body = copyFrom.getBody();
		this.headers = copyFrom.getHeaders();
		this.parameters = copyFrom.getParameters();
		this.namedParameters = copyFrom.isNamedParameters();
		this.pathParameterNames = copyFrom.getPathParameterNames();
		this.context = copyFrom.getContext();
	}
	
	public HttpRequestSettings copy()
	{
		return new HttpRequestSettings(this);
	}
	

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	
	public String getMethod()
	{
		return method;
	}

	public void setMethod(String method)
	{
		this.method = method;
	}

	
	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	
	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	
	public String getBody()
	{
		return body;
	}

	public void setBody(String body)
	{
		this.body = body;
	}

	
	public Map<String, String> getHeaders()
	{
		return headers;
	}

	public void setHeaders(Map<String, String> headers)
	{
		this.headers = headers;
	}

	
	public Map<String, String> getParameters()
	{
		return parameters;
	}

	public void setParameters(Map<String, String> parameters)
	{
		this.parameters = parameters;
	}

	
	public boolean isNamedParameters()
	{
		return namedParameters;
	}

	public void setNamedParameters(boolean namedParameters)
	{
		this.namedParameters = namedParameters;
	}


	public List<String> getPathParameterNames()
	{
		return pathParameterNames;
	}

	public void setPathParameterNames(List<String> pathParameterNames)
	{
		this.pathParameterNames = pathParameterNames;
	}

	
	public HttpClientContext getContext()
	{
		return context;
	}
	
	public void setContext(HttpClientContext context)
	{
		this.context = context;
	}
}
