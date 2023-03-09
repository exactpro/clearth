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

package com.exactprosystems.clearth.automation.actions.json;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.MessageComparator;
import com.exactprosystems.clearth.automation.actions.ReceiveMessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.json.*;
import com.exactprosystems.clearth.messages.*;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescWithTypeNotFoundError;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class ReceiveJsonMessage extends ReceiveMessageAction<ClearThJsonMessage>
{
	@Override
	public MessageBuilder<ClearThJsonMessage> getMessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
	{
		return new ClearThJsonMessageBuilder(serviceParameters, metaFields);
	}
	
	@Override
	protected String getDefaultCodecName()
	{
		return JsonCodec.DEFAULT_CODEC_NAME;
	}
	
	@Override
	protected void afterSearch(GlobalContext globalContext, List<ClearThJsonMessage> messages) throws ResultException
	{
	}

	@Override
	public boolean isIncoming()
	{
		return true;
	}
	
	
	@Override
	protected AllKeyFieldsData getAllKeys(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, ClearThJsonMessage message)
	{
		String type = getMessageType();
		JsonMessageDesc messageDesc = usesDictionary() ? findMessageDesc(getCodec(globalContext), type) : null;
		
		// with this implementation user is able to override key fields in matrix and skip their definition in dictionary
		
		KeyFieldsData keys = getKeys(stepContext, matrixContext, globalContext, message);
		if (!keys.hasKeys())
			keys = getJsonKeys(messageDesc, type, message);
		
		RgKeyFieldNames rgKeyFieldNames = getRgKeyFieldNames(stepContext, matrixContext, globalContext);
		if (rgKeyFieldNames == null || !rgKeyFieldNames.hasKeys())
			rgKeyFieldNames = getJsonRgKeyFieldNames(messageDesc);
		
		List<KeyFieldsData> keysInRgs = getKeysInRgs(stepContext, matrixContext, globalContext, message, rgKeyFieldNames);
		return new AllKeyFieldsData(keys, rgKeyFieldNames, keysInRgs);
	}
	
	@Override
	protected MessageComparator<ClearThJsonMessage> getMessageComparator()
	{
		return new JsonMessageComparator(getServiceParameters(), !isIgnoreExtraRgs(), true, isLogSubMessagesOutput());
	}
	
	
	protected boolean usesDictionary()
	{
		return true;
	}
	
	protected JsonMessageDesc findMessageDesc(ICodec codec, String msgType) throws ResultException
	{
		JsonDictionary dictionary = ((JsonCodec)codec).getDictionary();

		JsonMessageDesc messageDesc = dictionary.getMessageDesc(msgType);
		if (messageDesc == null)
			throw ResultException.failed(msgDescWithTypeNotFoundError(msgType));
		else
			return messageDesc;
	}

	protected KeyFieldsData getJsonKeys(JsonMessageDesc messageDesc, String msgType, ClearThJsonMessage message) throws ResultException
	{
		List<JsonKeyDesc> keyDescs = messageDesc.getKey();
		if (keyDescs.isEmpty())
			throw ResultException.failed(format("Keys aren't defined for message with type '%s' in dictionary.", msgType));
		
		KeyFieldsData keys = new KeyFieldsData();
		keys.setMsgType(msgType);
		InputParamsHandler handler = new InputParamsHandler(message.getFields());
		for (JsonKeyDesc desc : keyDescs)
		{
			if (!StringUtils.isEmpty(desc.getForSubMsg()))
				continue;
			String name = desc.getName();
			keys.addKey(new MessageKeyField(name, handler.getRequiredString(name)));
		}
		handler.check();
		return keys;
	}
	
	protected RgKeyFieldNames getJsonRgKeyFieldNames(JsonMessageDesc messageDesc)
	{
		RgKeyFieldNames keys = null;
		for (JsonKeyDesc keyDesc : messageDesc.getKey())
		{
			if (isEmpty(keyDesc.getForSubMsg()))
				continue;
			
			if (keys == null)
				keys = new RgKeyFieldNames();
			keys.addRgKeyField(keyDesc.getForSubMsg(), keyDesc.getName());
		}
		return keys;
	}
}
