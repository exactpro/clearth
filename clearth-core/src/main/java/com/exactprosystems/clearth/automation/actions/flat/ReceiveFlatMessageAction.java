/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.actions.flat;

import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescWithTypeNotFoundError;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.ReceiveMessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageCodec;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageDesc;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageDictionary;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageKeyDesc;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.KeyFieldsData;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.messages.MessageKeyField;
import com.exactprosystems.clearth.messages.RgKeyFieldNames;
import com.exactprosystems.clearth.utils.CommaBuilder;

public class ReceiveFlatMessageAction extends ReceiveMessageAction<SimpleClearThMessage>
{
	@Override
	public MessageBuilder<SimpleClearThMessage> getMessageBuilder(Set<String> serviceParameters)
	{
		return new SimpleClearThMessageBuilder(serviceParameters);
	}

	@Override
	protected String getDefaultCodecName()
	{
		return FlatMessageCodec.DEFAULT_CODEC_NAME;
	}

	@Override
	public boolean isIncoming()
	{
		return true;
	}

	@Override
	protected void afterSearch(GlobalContext globalContext, List<SimpleClearThMessage> messages) throws ResultException
	{
	}

	protected FlatMessageDesc getMessageDesc(ICodec codec, String msgType) throws ResultException
	{
		FlatMessageDictionary dictionary = ((FlatMessageCodec)codec).getDictionary();
		
		FlatMessageDesc messageDesc = dictionary.getMessageDesc(msgType);
		if (messageDesc == null)
			throw ResultException.failed(msgDescWithTypeNotFoundError(msgType));
		else
			return messageDesc;
	}
	
	@Override
	protected KeyFieldsData getKeys(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, SimpleClearThMessage message)
	{
		FlatMessageDesc description = getMessageDesc(getCodec(globalContext), getMessageType());
		List<FlatMessageKeyDesc> keysDesc = description.getKey();
		
		KeyFieldsData keyFields = new KeyFieldsData();
		keyFields.setMsgType(getMessageType());
		CommaBuilder mandatoryParamsNotFound = new CommaBuilder();
		for (FlatMessageKeyDesc keyDesc : keysDesc)
		{
			String fieldName = keyDesc.getName(),
					fieldValue = message.getField(fieldName);
			
			if (StringUtils.isEmpty(fieldValue))
				mandatoryParamsNotFound.append(fieldName);
			else
				keyFields.addKey(new MessageKeyField(fieldName, fieldValue));
		}
		
		if (mandatoryParamsNotFound.length() > 0)
			throw ResultException.failed("The following key fields are not filled: "+mandatoryParamsNotFound.toString());
		
		return keyFields;
	}
	
	@Override
	protected RgKeyFieldNames getRgKeyFieldNames(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		return null;
	}
}
