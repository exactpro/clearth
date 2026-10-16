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

import com.exactprosystems.clearth.BasicConnectionTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.RedirectException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class RequestSenderTest extends BasicConnectionTest
{
	@Override
	protected void mockOtherApplicationFields(ClearThCore application) throws Exception
	{
		ICodec codec = mock(ICodec.class);
		when(codec.encode(any(ClearThMessage.class))).thenReturn("encodedMessage");
		doReturn(codec).when(application).createCodec(anyString());
	}

	@DataProvider(name = "sendRequestData")
	public Object[][] createParametersForSendRequest()
	{
		return new Object[][]
				{
						{
								"/some-request?request-id=12345&start-time=123&end-time=234&instrument=345",
								"SimpleRequest",
								"http://localhost:" + wireMock.port() + "/",
								map("RequestId", "12345",
										"StartTime", "123",
										"EndTime", "234",
										"Instrument", "345")
						}
				};
	}

	@Test(dataProvider = "sendRequestData")
	public void testSendRequest(String expectedUrl, String httpRequestType, String rootUrl,
	                             Map<String, String> requestParams)
			throws Exception
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)));
		
		try(ClearThHttpClient client = createClient())
		{
			RequestSender requestSender = new RequestSender(httpRequestType, rootUrl,
					client, createHttpDictionary(), null, requestParams, createSettings());

			try (RequestSenderResult result = requestSender.sendRequest())
			{
				assertNotNull(result);
				CloseableHttpResponse response = result.getResponse();
				assertNotNull(response);
				StatusLine statusLine = response.getStatusLine();
				assertNotNull(statusLine);
				int statusCode = statusLine.getStatusCode();
				assertEquals(statusCode, HttpStatus.SC_OK);
			}
		}
	}

	@Test(dataProvider = "sendRequestData")
	public void testSendRequestWithRedirection(String expectedUrl, String httpRequestType, String rootUrl,
	                            Map<String, String> requestParams)
			throws Exception
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_MOVED_TEMPORARILY)
						.withHeader("Location", rootUrl+"/some-request/2")));

		wireMock.stubFor(get(urlEqualTo("/some-request/2"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)));

		try(ClearThHttpClient client = createClient())
		{
			RequestSender requestSender = new RequestSender(httpRequestType, rootUrl,
					client, createHttpDictionary(), null, requestParams, createSettings());

			try (RequestSenderResult result = requestSender.sendRequest())
			{
				assertNotNull(result);
				CloseableHttpResponse response = result.getResponse();
				assertNotNull(response);
				StatusLine statusLine = response.getStatusLine();
				assertNotNull(statusLine);
				int statusCode = statusLine.getStatusCode();
				assertEquals(statusCode, HttpStatus.SC_OK);
			}
		}
	}

	@Test(dataProvider = "sendRequestData")
	public void testSendRequestWithRedirectionException(String expectedUrl, String httpRequestType, String rootUrl,
	                                           Map<String, String> requestParams)
			throws Exception
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_MOVED_TEMPORARILY)
						.withHeader("Location", rootUrl + "/some-request/2")));

		wireMock.stubFor(get(urlEqualTo("/some-request/2"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_MOVED_TEMPORARILY)
						.withHeader("Location", rootUrl + expectedUrl)));

		try(ClearThHttpClient client = createClient())
		{
			RequestSender requestSender = new RequestSender(httpRequestType, rootUrl,
					client, createHttpDictionary(), null, requestParams, createSettings());

			try (RequestSenderResult result = requestSender.sendRequest())
			{
				Exception exception = result.getException();
				assertNotNull(exception);
				Throwable firstCause = exception.getCause();
				assertNotNull(firstCause);
				Throwable secondCause = firstCause.getCause();
				assertNotNull(secondCause);
				String message = secondCause.getMessage();
				assertEquals(secondCause.getClass(), RedirectException.class);
				assertEquals(message, "Maximum redirects (50) exceeded");
			}
		}
	}
}