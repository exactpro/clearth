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
package com.exactprosystems.clearth.automation.actions.http;

import static com.exactprosystems.clearth.connectivity.http.HttpUtils.allowBody;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.util.EntityUtils;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.report.results.DefaultTableResultDetail;
import com.exactprosystems.clearth.automation.report.results.TableResult;

public class HttpRequestResultBuilder
{
	public static final String NAME = "Name";
	public static final String VALUE = "Value";
	public static final String URL = "URL";
	public static final String METHOD = "Method";
	public static final String BODY = "Body";
	public static final String PROPERTIES = "Properties";
	public static final String HEADERS = "Headers";
	public static final String COOKIES = "Cookies";

	public static final List<String> COLUMNS = Arrays.asList(NAME, VALUE);

	public Result makeHttpRequestResult(HttpUriRequest request, CookieStore cookieStore, boolean showBodyInReport)
	{
		ContainerResult containerResult = new ContainerResult();

		try
		{
			TableResult requestProperties = createPropertiesTable(request, showBodyInReport);
			containerResult.addDetail(requestProperties);
		}
		catch (Exception e)
		{
			containerResult.addDetail(DefaultResult.failed(e));
			return containerResult;
		}

		Header[] headers = request.getAllHeaders();

		if ((headers != null) && (headers.length != 0))
		{
			TableResult requestHeaders = createHeadersTable(headers);
			containerResult.addDetail(requestHeaders);
		}

		if (cookieStore != null)
		{
			List<Cookie> cookies = cookieStore.getCookies();
			if (isNotEmpty(cookies))
			{
				TableResult requestCookies = createCookiesTable(cookies);
				containerResult.addDetail(requestCookies);
			}
		}

		return containerResult;
	}

	private TableResult createHeadersTable(Header[] headers)
	{
		TableResult result = new TableResult(HEADERS, COLUMNS, false);

		for (Header header : headers)
		{
			addToTable(header.getName(), header.getValue(), result);
		}

		return result;
	}

	private TableResult createCookiesTable(List<Cookie> cookies)
	{
		TableResult result = new TableResult(COOKIES, COLUMNS, false);

		for (Cookie cookie : cookies)
		{
			addToTable(cookie.getName(), cookie.getValue(), result);
		}

		return result;
	}

	private TableResult createPropertiesTable(HttpUriRequest request, boolean showBodyInReport) throws IOException
	{
		TableResult result = new TableResult(PROPERTIES, COLUMNS, false);
		String method = request.getMethod();

		addToTable(URL, request.getURI().toURL().toString(), result);
		addToTable(METHOD, method, result);

		if (showBodyInReport && allowBody(method))
		{
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			if (entity != null)
			{
				String body = (EntityUtils.toString(entity));
				addToTable(BODY, body, result);
			}
		}

		return result;
	}

	private void addToTable(String name, String value, TableResult tableResult)
	{
		if (value != null)
		{
			DefaultTableResultDetail defaultTableResultDetail = new DefaultTableResultDetail(name, value);
			tableResult.addDetail(defaultTableResultDetail);
		}
	}
}
