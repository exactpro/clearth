/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.protocol.HttpClientContext;

import com.exactprosystems.clearth.connectivity.http.ClearThHttpClient;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class HttpRequestSettingsBuilder
{
	protected HttpRequestSettings settings;
	
	public HttpRequestSettingsBuilder()
	{
		this.settings = createSettings();
	}
	
	public HttpRequestSettingsBuilder(HttpRequestSettings settings)
	{
		this.settings = settings;
	}
	
	
	protected HttpRequestSettings createSettings()
	{
		return new HttpRequestSettings();
	}
	
	
	public HttpRequestSettingsBuilder url(String url)
	{
		settings.setUrl(url);
		return this;
	}
	
	public HttpRequestSettingsBuilder method(String method)
	{
		settings.setMethod(method);
		return this;
	}
	
	public HttpRequestSettingsBuilder get()
	{
		settings.setMethod(ClearThHttpClient.METHOD_GET);
		return this;
	}
	
	public HttpRequestSettingsBuilder post()
	{
		settings.setMethod(ClearThHttpClient.METHOD_POST);
		return this;
	}
	
	public HttpRequestSettingsBuilder put()
	{
		settings.setMethod(ClearThHttpClient.METHOD_PUT);
		return this;
	}
	
	public HttpRequestSettingsBuilder delete()
	{
		settings.setMethod(ClearThHttpClient.METHOD_DELETE);
		return this;
	}
	
	
	public HttpRequestSettingsBuilder header(String header, String value)
	{
		if (settings.getHeaders() == null)
			settings.setHeaders(new LinkedHashMap<String, String>());
		
		settings.getHeaders().put(header, value);
		return this;
	}
	
	public HttpRequestSettingsBuilder headers(Map<String, String> headers)
	{
		settings.setHeaders(headers);
		return this;
	}
	
	
	public HttpRequestSettingsBuilder user(String user)
	{
		settings.setUser(user);
		return this;
	}
	
	public HttpRequestSettingsBuilder password(String password)
	{
		settings.setPassword(password);
		return this;
	}
	
	public HttpRequestSettingsBuilder body(String body)
	{
		settings.setBody(body);
		return this;
	}
	
	
	public HttpRequestSettingsBuilder parameter(String parameter, String value)
	{
		if (settings.getParameters() == null)
			settings.setParameters(new LinkedHashMap<String, String>());
		
		settings.getParameters().put(parameter, value);
		return this;
	}
	
	public HttpRequestSettingsBuilder optionalParameter(String name, String value)
	{
		return isNotBlank(value) ? parameter(name, value) : this;
	}
	
	public HttpRequestSettingsBuilder parameters(Map<String, String> parameters)
	{
		settings.setParameters(parameters);
		return this;
	}
	
	
	public HttpRequestSettingsBuilder namedParameters(boolean namedParameters)
	{
		settings.setNamedParameters(namedParameters);
		return this;
	}
	
	
	public HttpRequestSettingsBuilder pathParameterNames(List<String> names)
	{
		settings.setPathParameterNames(names);
		return this;
	}
	
	
	/**
	 * Defines context to be used while sending request.
	 * In case of multi-threaded use, each thread must have its own context and send requests using it. 
	 * @param context to use to send request
	 * @return a reference to this object.
	 */
	public HttpRequestSettingsBuilder context(HttpClientContext context)
	{
		settings.setContext(context);
		return this;
	}
	
	
	public HttpRequestSettings build()
	{
		HttpRequestSettings result = settings;
		settings = createSettings();
		return result;
	}
}
