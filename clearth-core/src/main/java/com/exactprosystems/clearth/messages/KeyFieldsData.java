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

package com.exactprosystems.clearth.messages;

import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.clearth.utils.CommaBuilder;

public class KeyFieldsData
{
	private String msgType,
			actionId;
	private final List<MessageKeyField> keys;
	
	public KeyFieldsData()
	{
		keys = new ArrayList<>();
	}
	
	
	public String getMsgType()
	{
		return msgType;
	}
	
	public void setMsgType(String msgType)
	{
		this.msgType = msgType;
	}
	
	
	public String getActionId()
	{
		return actionId;
	}
	
	public void setActionId(String actionId)
	{
		this.actionId = actionId;
	}
	
	
	public List<MessageKeyField> getKeys()
	{
		return keys;
	}
	
	public void addKey(MessageKeyField key)
	{
		keys.add(key);
	}
	
	
	public boolean hasKeys()
	{
		return !keys.isEmpty();
	}
	
	
	public String keysToString()
	{
		String typeString = "'"+msgType+"'";
		if (!hasKeys())
			return typeString;
		
		CommaBuilder keysString = new CommaBuilder();
		for (MessageKeyField keyField : keys)
			keysString.append(keyField.getName()+" = '"+keyField.getValue()+"'");
		return typeString+". Key fields: "+keysString.toString();
	}
	
	public String actionKeysToString()
	{
		if (!hasKeys())
			return "";
		
		CommaBuilder result = new CommaBuilder();
		for (MessageKeyField keyField : keys)
			result.append(actionId+" -> "+keyField.getName()+" = '"+keyField.getValue()+"'");
		return result.toString();
	}
}
