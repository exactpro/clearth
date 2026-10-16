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

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.http.HttpBody;
import com.exactprosystems.clearth.connectivity.http.HttpDictionary;
import com.exactprosystems.clearth.connectivity.http.HttpMethod;
import com.exactprosystems.clearth.connectivity.http.HttpRequestDesc;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import org.apache.commons.lang3.StringUtils;

public abstract class SendRestRequest<T extends ClearThMessage<T>> extends SendHttpRequest<T>
{
	@Override
	protected String prepareMessage(HttpRequestSettings settings) throws ResultException
	{
		String messageType = getMsgTypeFromHttpDictionary();
		if (StringUtils.isBlank(messageType))
			return null;

		T msg = buildMessage(getMatrixContext());

		msg.addField(ClearThMessage.MSGTYPE, messageType);

		ICodec codec = getCodec(getGlobalContext());
		try
		{
			return codec.encode(msg);
		}
		catch (EncodeException e)
		{
			throw ResultException.failed(e);
		}
	}

	protected String getMsgTypeFromHttpDictionary()
	{
		HttpDictionary dictionary = getHttpDictionary();
		String httpRequestType = getHttpRequestType();
		HttpRequestDesc httpRequestDesc = dictionary.findRequestDesc(httpRequestType);
		if (httpRequestDesc == null)
		{
			throw ResultException.failed(String.format("Could not find request description by request type '%s' " +
					"in http dictionary.", httpRequestType));
		}

		HttpBody httpBody = httpRequestDesc.getBody();
		if (httpBody == null)
		{
			if (httpRequestDesc.getMethod() != HttpMethod.GET)
				throw ResultException.failed(String.format("There is no body in request description with type '%s'.",
					httpRequestType));
			return null;
		}

		return httpBody.getMsgType();
	}
}
