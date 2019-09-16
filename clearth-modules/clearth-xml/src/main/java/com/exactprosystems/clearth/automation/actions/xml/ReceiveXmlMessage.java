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

package com.exactprosystems.clearth.automation.actions.xml;

import static com.exactprosystems.clearth.connectivity.Dictionary.msgDescWithTypeNotFoundError;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.actions.ReceiveMessageAction;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.xml.ClearThXmlMessage;
import com.exactprosystems.clearth.connectivity.xml.ClearThXmlMessageBuilder;
import com.exactprosystems.clearth.connectivity.xml.XmlCodec;
import com.exactprosystems.clearth.connectivity.xml.XmlDictionary;
import com.exactprosystems.clearth.connectivity.xml.XmlKeyDesc;
import com.exactprosystems.clearth.connectivity.xml.XmlMessageDesc;
import com.exactprosystems.clearth.messages.AllKeyFieldsData;
import com.exactprosystems.clearth.messages.KeyFieldsData;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.messages.MessageKeyField;
import com.exactprosystems.clearth.messages.RgKeyFieldNames;
import com.exactprosystems.clearth.utils.CommaBuilder;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

public class ReceiveXmlMessage extends ReceiveMessageAction<ClearThXmlMessage>
{
	@Override
	public MessageBuilder<ClearThXmlMessage> getMessageBuilder(Set<String> serviceParameters)
	{
		return new ClearThXmlMessageBuilder(serviceParameters);
	}
	
	@Override
	protected String getDefaultCodecName()
	{
		return XmlCodec.DEFAULT_CODEC_NAME;
	}
	
	@Override
	protected void afterSearch(GlobalContext globalContext, List<ClearThXmlMessage> messages) throws ResultException
	{
	}
	
	@Override
	public boolean isIncoming()
	{
		return true;
	}
	
	
	@Override
	protected AllKeyFieldsData getAllKeys(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext, ClearThXmlMessage message)
	{
		String type = getMessageType();
		XmlMessageDesc messageDesc = getMessageDesc(getCodec(globalContext), type);
		
		KeyFieldsData keys = getKeys(stepContext, matrixContext, globalContext, message);
		if (!keys.hasKeys())
			keys = getXmlKeys(messageDesc, type, message);
		
		RgKeyFieldNames rgKeyFieldNames = getRgKeyFieldNames(stepContext, matrixContext, globalContext);
		if (rgKeyFieldNames == null || !rgKeyFieldNames.hasKeys())
			rgKeyFieldNames = getXmlRgKeyFieldNames(messageDesc);
		
		List<KeyFieldsData> keysInRgs = getKeysInRgs(stepContext, matrixContext, globalContext, message, rgKeyFieldNames);
		return new AllKeyFieldsData(keys, rgKeyFieldNames, keysInRgs);
	}
	
	
	protected XmlMessageDesc getMessageDesc(ICodec codec, String msgType) throws ResultException
	{
		XmlCodec sc = (XmlCodec) codec;
		XmlDictionary dictionary = sc.getDictionary();

		XmlMessageDesc messageDesc = dictionary.getMessageDesc(msgType);
		if (messageDesc == null)
			throw ResultException.failed(msgDescWithTypeNotFoundError(msgType));
		return messageDesc;
	}
	
	protected KeyFieldsData getXmlKeys(XmlMessageDesc messageDesc, String type, ClearThXmlMessage message)
	{
		KeyFieldsData keys = new KeyFieldsData();
		keys.setMsgType(type);
		
		CommaBuilder missingMandatoryParams = new CommaBuilder();
		for (XmlKeyDesc keyDesc : messageDesc.getKey())
		{
			if (!StringUtils.isEmpty(keyDesc.getInRG()))
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
	
	protected RgKeyFieldNames getXmlRgKeyFieldNames(XmlMessageDesc messageDesc)
	{
		RgKeyFieldNames keys = null;
		for (XmlKeyDesc keyDesc : messageDesc.getKey())
		{
			if (StringUtils.isEmpty(keyDesc.getInRG()))
				continue;
			
			if (keys == null)
				keys = new RgKeyFieldNames();
			
			String subMsgType = keyDesc.getInRG();
			Set<String> rgKeys = keys.getRgKeyFields(subMsgType);
			if (rgKeys == null)
			{
				rgKeys = new HashSet<String>();
				keys.addRgKeyFields(subMsgType, rgKeys);
			}
			rgKeys.add(keyDesc.getName());
		}		
		return keys;
	}
}
