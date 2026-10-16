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

import static com.exactprosystems.clearth.automation.actions.http.SendHttpRequest.PARAM_HTTP_REQUEST_TYPE;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.actions.http.HttpRequestSettings;
import com.exactprosystems.clearth.automation.actions.http.HttpResponseData;
import com.exactprosystems.clearth.automation.actions.http.SendRestRequest;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.http.ClearThHttpClient;
import com.exactprosystems.clearth.connectivity.http.HttpDictionary;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.utils.Utils;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class SendRestRequestTest extends BasicTestNgTest
{
	private static final String HTTP_DICTIONARY_PATH = "cfg/dicts/http.xml";
	
	public WireMockRule wireMock;
	
	@BeforeClass
	public void startWireMock()
	{
		wireMock = new WireMockRule(options()
				.bindAddress("localhost")
				.dynamicPort()
				.disableRequestJournal());
		wireMock.stubFor(get(urlEqualTo("/some-request"))
				.willReturn(aResponse()
						.withStatus(HttpStatus.SC_OK)));
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
	
	
	@DataProvider(name = "requests")
	public Object[][] requests()
	{
		return new Object[][] {
			{ Map.of(PARAM_HTTP_REQUEST_TYPE, "DownloadRequest"), true, null, String.valueOf(HttpStatus.SC_OK)},
			{ Map.of(PARAM_HTTP_REQUEST_TYPE, "NullRequestParametersRequest"), false,
				"There is no body in request description with type 'NullRequestParametersRequest'.",
				null }
		};
	}
	
	@Test(dataProvider = "requests")
	public void sendRestRequest(Map<String, String> inputParams, boolean success, String errorText, String comment) throws FailoverException
	{
		Action action = createAction(inputParams, wireMock.port());
		Result result = action.execute(null, new MatrixContext(), null);
		
		assertEquals(result.isSuccess(), success, "Result success");
		
		if (errorText == null)
			assertNull(result.getError(), "Result error");
		else
		{
			Throwable error = result.getError();
			assertNotNull(error, "Result error");
			assertEquals(error.getMessage(), errorText, "Result error message");
		}
		
		assertEquals(result.getComment(), comment, "Result comment");
	}
	
	public Action createAction(Map<String, String> params, int port)
	{
		ActionSettings settings = new ActionSettings();
		settings.setParams(params);
		
		Action action = new TestSendRestRequest(port);
		action.preInit(null, TestSendRestRequest.class.getSimpleName(), new HashMap<>());
		action.init(settings);
		return action;
	}
	
	
	private static class TestSendRestRequest extends SendRestRequest<SimpleClearThMessage>
	{
		private final int port;
		
		private TestSendRestRequest(int port) 
		{
			this.port = port;
		}
		
		@Override
		protected String getDefaultParentUrl()
		{
			return "http://localhost:"+port+"/";
		}
		
		@Override
		protected String getDefaultUser()
		{
			return null;
		}
		
		@Override
		protected String getDefaultPassword()
		{
			return null;
		}
		
		@Override
		protected HttpDictionary getHttpDictionary()
		{
			try
			{
				String path = resourceToAbsoluteFilePath(HTTP_DICTIONARY_PATH);
				return new HttpDictionary(path);
			}
			catch (Exception e)
			{
				throw new ResultException(format("Error while loading HTTP Requests Dictionary from file '%s'.",
						HTTP_DICTIONARY_PATH), e);
			}
		}
		
		@Override
		protected ClearThHttpClient getHttpClient() throws ResultException
		{
			try
			{
				return new ClearThHttpClient();
			}
			catch (Exception e)
			{
				throw ResultException.failed(e);
			}
		}
		
		@Override
		protected void closeResourcesAfterSending(ClearThHttpClient client, CloseableHttpResponse response)
		{
			Utils.closeResource(client);
			Utils.closeResource(response);
		}
		
		@Override
		public MessageBuilder<SimpleClearThMessage> getMessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
		{
			return new SimpleClearThMessageBuilder();
		}
		
		@Override
		protected String getDefaultCodecName()
		{
			return "JSON";
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