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

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.actions.http.HttpRequestSettings;
import com.exactprosystems.clearth.automation.actions.http.HttpResponseData;
import com.exactprosystems.clearth.automation.actions.http.SendPlainHttpRequest;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.http.HttpDictionary;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import com.exactprosystems.clearth.utils.SettingsException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import static com.exactprosystems.clearth.automation.actions.http.SendHttpRequest.PARAM_HTTP_REQUEST_TYPE;
import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class SendPlainHttpRequestTest extends BasicTestNgTest
{
	private static final String HTTP_DICTIONARY_PATH = "cfg/dicts/http.xml";
	private static final String USER = "admin";
	private static final String PASSWORD = "admin";

	public WireMockRule wireMock = new WireMockRule(options()
			.bindAddress("localhost")
			.dynamicPort()
			.disableRequestJournal());

	@BeforeClass
	public void startWireMock()
	{
		wireMock.start();
	}

	@AfterClass
	public void stopWireMock()
	{
		wireMock.stop();
	}

	@Override
	protected void mockOtherApplicationFields(ClearThCore application) throws Exception
	{
		ICodec codec = mock(ICodec.class);
		when(codec.encode(any(ClearThMessage.class))).thenReturn("encodedMessage");
		doReturn(codec).when(application).createCodec(anyString());
	}

	@DataProvider(name = "requestData")
	public Object[][] createParameters()
	{
		return new Object[][]
				{
						{
								"/some-request?request-id=12345&start-time=123&end-time=234&instrument=345",
								map(PARAM_HTTP_REQUEST_TYPE, "SimpleRequest",
										"RequestId", "12345",
										"StartTime", "123",
										"EndTime", "234",
										"Instrument", "345")
						}
				};
	}

	@Test(dataProvider = "requestData")
	public void test(String expectedUrl, Map<String, String> actionParams) throws FailoverException
	{
		wireMock.stubFor(get(urlEqualTo(expectedUrl))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)));

		Action action = createAction(actionParams);
		Result result = action.execute(null, new MatrixContext(), null);

		assertTrue(result.isSuccess(), result.getComment());
		assertNull(result.getError());
		assertEquals(result.getComment(), String.valueOf(HttpStatus.SC_OK));
	}

	public Action createAction(Map<String, String> params)
	{
		Action action = new TestSendHttpRequest(wireMock.port());
		action.preInit(null, TestSendHttpRequest.class.getSimpleName(), new HashMap<>());
		ActionSettings settings = new ActionSettings();
		settings.setParams(params);
		action.init(settings);
		return action;
	}

	private static class TestSendHttpRequest extends SendPlainHttpRequest
	{
		private final int port;

		private TestSendHttpRequest(int port) 
		{
			this.port = port;
		}

		@Override
		protected HttpDictionary getHttpDictionary()
		{
			try
			{
				String path = resourceToAbsoluteFilePath(HTTP_DICTIONARY_PATH);
				return new HttpDictionary(path);
			}
			catch (DictionaryLoadException | SettingsException | FileNotFoundException e)
			{
				throw new ResultException(format("Error while loading HTTP Requests Dictionary from file '%s'.",
						HTTP_DICTIONARY_PATH), e);
			}
		}

		@Override
		protected String getDefaultParentUrl() 
		{
			return "http://localhost:"+port+"/";
		}

		@Override
		protected String getDefaultUser()
		{
			return USER;
		}

		@Override
		protected String getDefaultPassword()
		{
			return PASSWORD;
		}

		@Override
		protected Result processResponse(HttpResponseData response, String messageToSend, HttpRequestSettings settings)
		{
			int code = response.getCode();
			DefaultResult result = new DefaultResult();
			result.setSuccess(code == HttpStatus.SC_OK);
			result.setComment(String.valueOf(code));
			return result;
		}
	}
}
