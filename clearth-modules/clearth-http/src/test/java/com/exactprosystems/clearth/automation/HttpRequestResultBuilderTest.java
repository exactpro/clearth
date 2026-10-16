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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.automation.actions.http.HttpRequestResultBuilder;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.DefaultTableResultDetail;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.connectivity.http.HttpDeleteWithBody;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.*;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static com.exactprosystems.clearth.automation.actions.http.HttpRequestResultBuilder.*;
import static com.exactprosystems.clearth.connectivity.http.HttpUtils.allowBody;
import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpRequestResultBuilderTest extends BasicTestNgTest
{
	@DataProvider(name = "requestData")
	public Object[][] createParams()
	{
		return new Object[][]
				{
						//properties, headers, cookies, showBodyInReport
						{
								map(URL, "http://localhost:9090/", METHOD, "PUT"),
								"bodyPart",
								map("Content-Type", "application/", "TemplateHeader", "template"),
								map("session-cookie", "sessionid-123", "some-cookie", "456"),
								true
						},
						{
								map(URL, "http://localhost:9090/", METHOD, "GET"),
								null,
								map("Content-Type", "application/", "TemplateHeader", "template"),
								map("session-cookie", "sessionid-123", "some-cookie", "456"),
								true
						},
						{
								map(URL, "http://localhost:9090/", METHOD, "GET"),
								null,
								map("Content-Type", "application/", "TemplateHeader", "template"),
								map("session-cookie", "sessionid-123", "some-cookie", "456"),
								false
						},
						{
								map(URL, "http://localhost:9090/", METHOD, "PATCH"),
								"bodyPart",
								map("Content-Type", "application/", "TemplateHeader", "template"),
								map("session-cookie", "sessionid-123", "some-cookie", "456"),
								true
						},
						{
								map(URL, "http://localhost:9090/", METHOD, "POST"),
								"bodyPart",
								map("Content-Type", "application/", "TemplateHeader", "template"),
								map("session-cookie", "sessionid-123", "some-cookie", "456"),
								true
						},
						{
								map(URL, "http://localhost:9090/", METHOD, "DELETE"),
								"bodyPart",
								map("Content-Type", "application/", "TemplateHeader", "template"),
								map("session-cookie", "sessionid-123", "some-cookie", "456"),
								true
						},
						{
								map(URL, "http://localhost:9090/", METHOD, "PUT"),
								null,
								null,
								map("session-cookie", "sessionid-123", "some-cookie", "456"),
								true
						},
						{
								map(URL, "http://localhost:9090/", METHOD, "PUT"),
								null,
								map("Content-Type", "application/", "TemplateHeader", "template"),
								null,
								false
						},
						{
								map(URL, "http://localhost:9090/", METHOD, "PUT"),
								"bodyPart",
								null,
								null,
								false
						}
				};
	}

	@Test(dataProvider = "requestData")
	public void test(Map<String, String> properties, String body,  Map<String, String> headers,
	                 Map<String, String> cookies, boolean showBodyInReport) throws URISyntaxException
	{
		URI uri = new URI(properties.get(URL));
		
		CookieStore cookieStore = mock(CookieStore.class);
		List<Cookie> createdCookies = initCookies(cookies);
		when(cookieStore.getCookies()).thenReturn(createdCookies);

		HttpUriRequest httpUriRequest = initRequestByMethod(properties.get(METHOD), uri);
		if (showBodyInReport)
			setRequestBody(httpUriRequest, body);
		setRequestHeaders(httpUriRequest, headers);

		HttpRequestResultBuilder resultBuilder = new HttpRequestResultBuilder();
		ContainerResult actualResult = (ContainerResult) resultBuilder.makeHttpRequestResult(httpUriRequest, cookieStore,
				showBodyInReport);
		ContainerResult expectedResult = createExpectedResult(properties, headers, cookies, body, showBodyInReport);

		assertThat(actualResult).usingRecursiveComparison().isEqualTo(expectedResult);
	}

	private ContainerResult createExpectedResult(Map<String, String> properties, Map<String, String> headers,
	                                             Map<String, String> cookies, String body, boolean showBodyInReport)
	{
		ContainerResult containerResult = new ContainerResult();

		if (isNotEmpty(properties))
		{
			TableResult propertiesTable = createTable(PROPERTIES, properties);
			if(showBodyInReport && allowBody(properties.get(METHOD)) && body != null)
			{
				DefaultTableResultDetail defaultTableResultDetail = new DefaultTableResultDetail(BODY, body);
				propertiesTable.addDetail(defaultTableResultDetail);
			}
			containerResult.addDetail(propertiesTable);
		}

		if (isNotEmpty(headers))
		{
			TableResult headersTable = createTable(HEADERS, headers);
			containerResult.addDetail(headersTable);
		}

		if (isNotEmpty(cookies))
		{
			TableResult cookiesTable = createTable(COOKIES, cookies);
			containerResult.addDetail(cookiesTable);
		}

		return containerResult;
	}
	
	private TableResult createTable(String header, Map<String, String> parameters)
	{
		TableResult table = new TableResult(header, COLUMNS, false);
		for (Map.Entry<String, String> parameter : parameters.entrySet())
		{
			String name = parameter.getKey();
			String value = parameter.getValue();
			if (value != null)
			{
				DefaultTableResultDetail detail = new DefaultTableResultDetail(name, value);
				table.addDetail(detail);
			}
		}
		return table;
	}

	private HttpUriRequest initRequestByMethod(String method, URI uri)
	{
		HttpUriRequest request = null;
		switch (method.toLowerCase())
		{
			case "get":
				request = new HttpGet(uri);
				break;
			case "post":
				request = new HttpPost(uri);
				break;
			case "put":
				request = new HttpPut(uri);
				break;
			case "patch":
				request = new HttpPatch(uri);
				break;
			case "delete":
				request = new HttpDeleteWithBody(uri);
				break;
		}

		return request;
	}

	private void setRequestHeaders(HttpUriRequest request, Map<String, String> headers)
	{
		if (headers!= null)
		{
			for (Map.Entry<String, String> header : headers.entrySet())
			{
				request.setHeader(header.getKey(), header.getValue());
			}
		}
	}

	private void setRequestBody(HttpUriRequest request, String body)
	{
		if (body != null && allowBody(request.getMethod()))
			((HttpEntityEnclosingRequest)request).setEntity(new StringEntity(body, UTF_8));
	}

	private List<Cookie> initCookies(Map<String, String> cookies)
	{
		List<Cookie> result = new ArrayList<>();

		if (cookies != null)
		{
			for (Map.Entry<String, String> cookie : cookies.entrySet())
			{
				result.add(new BasicClientCookie(cookie.getKey(), cookie.getValue()));
			}
		}

		return result;
	}
}
