/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.actions.th2;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.automation.actions.metadata.MetaFieldsGetter;
import com.exactprosystems.clearth.automation.actions.metadata.SimpleMetaFieldsGetter;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.converters.MessageToJson;
import com.exactprosystems.clearth.messages.converters.MessageToMap;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

public class SendTh2LoaderMessage extends Action
{
	public static final String PARAM_URL = "URL",
			PARAM_HOST = "Host",
			PARAM_PORT = "Port",
			PARAM_ACTOR = "Actor",
			PARAM_FLAT_DELIMITER = "FlatDelimiter",
			
			CONTEXT_LOADER_CLIENT = "Th2LoaderClient";
	
	//MsgType is excluded from these sets because it is often part of the message being sent to loader
	private static final Set<String> BUILDER_SERVICE_PARAMS = Set.of(PARAM_URL,
			PARAM_HOST, PARAM_PORT, PARAM_ACTOR, PARAM_FLAT_DELIMITER,
			ClearThMessage.SUBMSGTYPE, ClearThMessage.SUBMSGSOURCE,
			MessageAction.REPEATINGGROUPS, MessageAction.META_FIELDS);
	private static final Set<String> CONVERTER_SERVICE_FIELDS = Set.of(ClearThMessage.SUBMSGTYPE,
			ClearThMessage.SUBMSGSOURCE, MessageToMap.SUBMSGKIND);
			
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		//User can specify full URL (to send message wherever specified) or separate components of URL (recommended way)
		String url = getInputParam(PARAM_URL);
		if (StringUtils.isEmpty(url))
		{
			InputParamsHandler handler = new InputParamsHandler(inputParams);
			String host = handler.getRequiredString(PARAM_HOST);
			Integer port = handler.getRequiredInteger(PARAM_PORT);
			String actor = handler.getRequiredString(PARAM_ACTOR);
			handler.check();
			
			url = buildUrl(host, port, actor);
		}
		
		String flatDelimiter = getInputParam(PARAM_FLAT_DELIMITER);
		
		SimpleClearThMessage message = createClearThMessage(matrixContext);
		String messageString = createMessageString(message, flatDelimiter);
		
		logger.trace("Sending message to {}: {}", url, messageString);
		
		//client is not closed here. It is stored in globalContext to be reused by other actions and will be closed when Scheduler finishes execution 
		CloseableHttpClient client = getHttpClient(globalContext);
		return sendMessage(url, messageString, client);
	}
	
	
	protected String buildUrl(String host, int port, String actor)
	{
		return String.format("http://%s:%s/actor/%s/onevent",
				host, port, actor);
	}
	
	protected SimpleClearThMessage createClearThMessage(MatrixContext matrixContext)
	{
		Map<String, String> ip = getInputParams();
		return getMessageBuilder(getBuilderServiceParameters(), getMetaFields(ip))
				.fields(ip)
				.metaFields(ip)
				.rgs(matrixContext, this)
				.build();
	}
	
	protected String createMessageString(SimpleClearThMessage message, String flatDelimiter)
	{
		Set<String> serviceParams = getConverterServiceFields();
		
		//Checking for null, not with StringUtils.isEmpty(), to allow switching flat mode off by specifying #FlatDelimiter=""
		MessageToMap toMap = flatDelimiter == null ? new MessageToMap(serviceParams) : new MessageToMap(flatDelimiter, serviceParams);
		MessageToJson toJson = new MessageToJson(toMap);
		try
		{
			return toJson.convert(message);
		}
		catch (Exception e)
		{
			throw ResultException.failed("Error while converting message to JSON", e);
		}
	}
	
	protected CloseableHttpClient getHttpClient(GlobalContext gc)
	{
		try
		{
			CloseableHttpClient result = gc.getCloseableContext(CONTEXT_LOADER_CLIENT);
			if (result == null)
			{
				result = createHttpClient();
				gc.setCloseableContext(CONTEXT_LOADER_CLIENT, result);
			}
			return result;
		}
		catch (Exception e)
		{
			throw ResultException.failed("Error while preparing HTTP client", e);
		}
	}
	
	protected Result sendMessage(String url, String message, CloseableHttpClient client)
	{
		HttpPost request = createRequest(url, message);
		try
		{
			try (CloseableHttpResponse response = client.execute(request))
			{
				HttpEntity entity = response.getEntity();
				int responseCode = response.getStatusLine().getStatusCode();
				String responseText = entity != null ? EntityUtils.toString(entity) : "";
				
				DefaultResult result = new DefaultResult();
				result.setComment(String.format("Request has been sent. Response code: %s, response text: %s",
						responseCode, responseText));
				result.setSuccess(responseCode == 200);
				return result;
			}
		}
		catch (Exception e)
		{
			throw ResultException.failed("Error while sending HTTP request", e);
		}
	}
	
	
	protected Set<String> getBuilderServiceParameters()
	{
		return BUILDER_SERVICE_PARAMS;
	}
	
	protected Set<String> getConverterServiceFields()
	{
		return CONVERTER_SERVICE_FIELDS;
	}
	
	protected Set<String> getMetaFields(Map<String, String> params)
	{
		MetaFieldsGetter metaGetter = getMetaFieldsGetter();
		Set<String> result = metaGetter.getFields(params);
		metaGetter.checkFields(result, params);
		return result;
	}
	
	
	protected SimpleClearThMessageBuilder getMessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
	{
		return new SimpleClearThMessageBuilder(serviceParameters, metaFields);
	}
	
	protected MetaFieldsGetter getMetaFieldsGetter()
	{
		return new SimpleMetaFieldsGetter(false);
	}
	
	protected CloseableHttpClient createHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
	{
		HttpClientBuilder builder = HttpClients.custom();
		builder.useSystemProperties();
		builder.setRedirectStrategy(new LaxRedirectStrategy());
		builder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true));
		
		//For HTTPS
		SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
		HostnameVerifier verifier = new DefaultHostnameVerifier();
		SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, verifier);
		builder.setSSLSocketFactory(sslConnectionFactory);
		
		//Default request configuration
		builder.setDefaultRequestConfig(RequestConfig.custom()
				.setCookieSpec(CookieSpecs.DEFAULT)
				.setCircularRedirectsAllowed(true)
				.setRedirectsEnabled(true)
				.build());
		
		return builder.build();
	}
	
	protected HttpPost createRequest(String url, String message)
	{
		HttpPost result = new HttpPost(url);
		result.setEntity(new StringEntity(message, UTF_8));
		return result;
	}
}
