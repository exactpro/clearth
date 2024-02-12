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
import java.util.Set;

import com.exactprosystems.clearth.messages.SimpleClearThMessageFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.th2.Th2LoaderConnection;
import com.exactprosystems.clearth.messages.converters.MessageToJson;
import com.exactprosystems.clearth.messages.converters.MessageToMap;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;

public class SendTh2LoaderMessage extends Action
{
	public static final String PARAM_FLAT_DELIMITER = "FlatDelimiter",
			CONTEXT_LOADER_CLIENT = "Th2LoaderClient";
	
	//MsgType is excluded from these sets because it is often part of the message being sent to loader
	private static final Set<String> BUILDER_SERVICE_PARAMS = Set.of(MessageAction.CONNECTIONNAME, 
			MessageAction.REPEATINGGROUPS, MessageAction.META_FIELDS,
			PARAM_FLAT_DELIMITER,
			ClearThMessage.SUBMSGTYPE, ClearThMessage.SUBMSGSOURCE);
	private static final Set<String> CONVERTER_SERVICE_FIELDS = Set.of(ClearThMessage.SUBMSGTYPE,
			ClearThMessage.SUBMSGSOURCE, MessageToMap.SUBMSGKIND);
			
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		ClearThConnection con = handler.getRequiredClearThConnection(MessageAction.CONNECTIONNAME, Th2LoaderConnection.TYPE_TH2_LOADER);
		handler.check();
		
		Th2LoaderConnection loaderCon = (Th2LoaderConnection)con;
		
		String flatDelimiter = getInputParam(PARAM_FLAT_DELIMITER);
		
		SimpleClearThMessage message = createClearThMessage(matrixContext);
		String messageString = createMessageString(message, flatDelimiter),
				url = loaderCon.getSettings().getUrl();
		
		logger.trace("Sending message to {}: {}", url, messageString);
		
		//client is not closed here. It is stored in globalContext to be reused by other actions and will be closed when Scheduler finishes execution 
		CloseableHttpClient client = getHttpClient(globalContext, loaderCon);
		return sendMessage(url, messageString, client);
	}
	
	protected SimpleClearThMessage createClearThMessage(MatrixContext matrixContext)
	{
		return new SimpleClearThMessageFactory(getBuilderServiceParameters(), getMetaFieldsGetter())
				.createMessageWithoutType(getInputParams(), matrixContext, this);
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
	
	protected CloseableHttpClient getHttpClient(GlobalContext gc, Th2LoaderConnection loaderCon)
	{
		try
		{
			CloseableHttpClient result = gc.getCloseableContext(CONTEXT_LOADER_CLIENT);
			if (result == null)
			{
				result = createHttpClient(loaderCon);
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
	
	protected MetaFieldsGetter getMetaFieldsGetter()
	{
		return new SimpleMetaFieldsGetter(false);
	}
	
	protected CloseableHttpClient createHttpClient(Th2LoaderConnection loaderCon) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
	{
		return loaderCon.createClient();
	}
	
	protected HttpPost createRequest(String url, String message)
	{
		HttpPost result = new HttpPost(url);
		result.setEntity(new StringEntity(message, UTF_8));
		return result;
	}
}
