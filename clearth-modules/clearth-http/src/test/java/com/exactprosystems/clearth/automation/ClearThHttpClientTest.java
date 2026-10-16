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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.automation.actions.http.HttpRequestSettings;
import com.exactprosystems.clearth.connectivity.http.ClearThHttpClient;
import com.exactprosystems.clearth.utils.Utils;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.testng.annotations.*;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.testng.Assert.assertEquals;

public class ClearThHttpClientTest
{
	private WireMockRule wireMock = new WireMockRule(options()
			.bindAddress("localhost")
			.dynamicPort()
			.disableRequestJournal());
	
	private ClearThHttpClient httpClient;
	

	@BeforeClass
	public void startWireMock() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException
	{
		wireMock.start();
		httpClient = new ClearThHttpClient();
	}

	@AfterClass
	public void stopWireMock()
	{
		wireMock.stop();
		Utils.closeResource(httpClient);
	}

	@DataProvider(name = "requestData")
	public Object[][] createParameters()
	{
		int port = wireMock.port();
		return new Object[][]
				{
						{
								"/hello/1",
								"http://localhost:"+port+"/hello/1",
								HttpStatus.SC_OK
						},
						{
								"/hello/1",
								"http://localhost:"+port+"/hello/11",
								HttpStatus.SC_NOT_FOUND
						}
				};
	}

	@Test(dataProvider = "requestData")
	public void testSimple(String stubUrl, String requestUrl, int httpStatus) throws IOException
	{
		wireMock.stubFor(get(urlEqualTo(stubUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)));

		try (CloseableHttpResponse response = httpClient.send(new HttpGet(requestUrl), true))
		{
			assertEquals(response.getStatusLine().getStatusCode(), httpStatus);
		}
	}

	@Test
	public void testRedirect() throws IOException
	{
		int port = wireMock.port();
		
		String expectedResponse = "Hello world 2!";
		wireMock.stubFor(get(urlEqualTo("/hello/2"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_MOVED_TEMPORARILY)
						.withHeader("Location", "http://localhost:"+port+"/hello/3")));

		wireMock.stubFor(get(urlEqualTo("/hello/3"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)
						.withBody(expectedResponse)));

		HttpUriRequest request = new HttpGet("http://localhost:"+port+"/hello/2");
		try (CloseableHttpResponse response = httpClient.send(request, new HttpRequestSettings(), false))
		{
			HttpEntity entity = response.getEntity();
			String actualResponse = EntityUtils.toString(entity);

			assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
			assertEquals(actualResponse, expectedResponse);
		}
	}
}
