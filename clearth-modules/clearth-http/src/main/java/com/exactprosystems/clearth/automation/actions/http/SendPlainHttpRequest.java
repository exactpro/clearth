/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import java.util.Set;

import org.apache.http.client.methods.CloseableHttpResponse;

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.http.ClearThHttpClient;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.utils.Utils;

public abstract class SendPlainHttpRequest extends SendHttpRequest
{
	@Override
	public MessageBuilder getMessageBuilder(Set serviceParameters, Set metaFields)
	{
		return null;  //Not used
	}
	
	@Override
	protected String prepareMessage(HttpRequestSettings settings) throws ResultException
	{
		return getInputParam(PARAM_REQUEST_BODY);  //null here will result in sending of plain request with no body
	}

	@Override
	protected String getDefaultCodecName()
	{
		//Not used
		return "";
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
}
