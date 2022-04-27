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

package com.exactprosystems.clearth.automation.actions.swift;

import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescDoesNotFitError;
import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescWithTypeNotFoundError;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.actions.ReceiveMessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.swift.*;
import com.exactprosystems.clearth.messages.AllKeyFieldsData;
import com.exactprosystems.clearth.messages.KeyFieldsData;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.messages.MessageKeyField;
import com.exactprosystems.clearth.messages.RgKeyFieldNames;
import com.exactprosystems.clearth.utils.CommaBuilder;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

public class ReceiveSwiftMessage extends ReceiveMessageAction<ClearThSwiftMessage>
{
	@Override
	public MessageBuilder<ClearThSwiftMessage> getMessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
	{
		return new ClearThSwiftMessageBuilder(serviceParameters, metaFields);
	}
	
	@Override
	protected String getDefaultCodecName()
	{
		return SwiftCodec.DEFAULT_CODEC_NAME;
	}
	
	@Override
	protected void afterSearch(GlobalContext globalContext, List<ClearThSwiftMessage> messages) throws ResultException
	{
	}
	
	@Override
	public boolean isIncoming()
	{
		return true;
	}
	
	
	@Override
	protected AllKeyFieldsData getAllKeys(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, ClearThSwiftMessage message)
	{
		String type = getMessageType();
		SwiftMessageDesc messageDesc = getMessageDesc(getCodec(globalContext), type);
		
		KeyFieldsData keys = getKeys(stepContext, matrixContext, globalContext, message);
		if (!keys.hasKeys())
			keys = getSwiftKeys(messageDesc, type, message);
		
		RgKeyFieldNames rgKeyFieldNames = getRgKeyFieldNames(stepContext, matrixContext, globalContext);
		if (rgKeyFieldNames == null || !rgKeyFieldNames.hasKeys())
			rgKeyFieldNames = getSwiftRgKeyFieldNames(messageDesc);
		
		List<KeyFieldsData> keysInRgs = getKeysInRgs(stepContext, matrixContext, globalContext, message, rgKeyFieldNames);
		return new AllKeyFieldsData(keys, rgKeyFieldNames, keysInRgs);
	}
	
	
	protected SwiftMessageDesc getMessageDesc(ICodec codec, String msgType) throws ResultException
	{
		SwiftCodec sc = (SwiftCodec) codec;
		SwiftDictionary dictionary = sc.getDictionary();
		
		SwiftMessageDesc messageDesc = dictionary.getMessageDesc(msgType);
		if(messageDesc == null)
			throw ResultException.failed(msgDescWithTypeNotFoundError(msgType));
		
		if (sc.msgDescFits(messageDesc, isIncoming()))
			return messageDesc;
		else
			throw ResultException.failed(msgDescDoesNotFitError(msgType));
	}
	
	protected KeyFieldsData getSwiftKeys(SwiftMessageDesc messageDesc, String type, ClearThSwiftMessage message)
	{
		KeyFieldsData keys = new KeyFieldsData();
		keys.setMsgType(type);
		
		CommaBuilder missingMandatoryParams = new CommaBuilder();
		for (SwiftKeyDesc keyDesc : messageDesc.getKey())
		{
			if (!StringUtils.isEmpty(keyDesc.getSubMsg()))
				continue;
			
			String name = keyDesc.getName();
			String fieldValue = message.getField(name);
			if (StringUtils.isEmpty(fieldValue))
			{
				if (keyDesc.isMandatory())
					missingMandatoryParams.append(name);
			}
			else
				keys.addKey(new MessageKeyField(name, fieldValue));
		}
		
		if (missingMandatoryParams.length() > 0)
			throw ResultException.failed("The following mandatory parameters are missing: "+missingMandatoryParams.toString());
		
		return keys;
	}
	
	protected RgKeyFieldNames getSwiftRgKeyFieldNames(SwiftMessageDesc messageDesc)
	{
		RgKeyFieldNames keys = null;
		for (SwiftKeyDesc keyDesc : messageDesc.getKey())
		{
			if (StringUtils.isEmpty(keyDesc.getSubMsg()))
				continue;
			
			if (keys == null)
				keys = new RgKeyFieldNames();
			keys.addRgKeyField(keyDesc.getSubMsg(), keyDesc.getName());
		}
		return keys;
	}
}
