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

package com.exactprosystems.clearth.connectivity.swift;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClearThSwiftMessage extends ClearThMessage<ClearThSwiftMessage>
{
	protected final LinkedHashMap<String, String> tags;
	protected ClearThSwiftMessage parent = null;
	private String dictionaryMsgType = null;
	
	private final SwiftMetaData metaData;
	
	public final static String INCOMINGMESSAGE = "IncomingMessage",
			USEBLOCK3 = "UseBlock3",
			ADDBLOCK5 = "AddBlock5",
			DICTIONARYMSGTYPE = "DictionaryMsgType",
			MSGCOUNT = "MsgCount";

	public ClearThSwiftMessage()
	{
		tags = new LinkedHashMap<String, String>();
		this.metaData = initMetaData(tags);
	}
	
	public ClearThSwiftMessage(LinkedHashMap<String, String> map, List<ClearThSwiftMessage> subMessages)
	{
		this.tags = new LinkedHashMap<String, String>(map);
		if (subMessages!=null)
		{
			for (ClearThSwiftMessage subMsg : subMessages)
				addSubMessage(subMsg);
		}
		this.metaData = initMetaData(tags);
	}

	public ClearThSwiftMessage(LinkedHashMap<String, String> map, List<ClearThSwiftMessage> subMessages, 
			SwiftMetaData smd)
	{
		this.tags = new LinkedHashMap<String, String>(map);
		if (subMessages!=null)
		{
			for (ClearThSwiftMessage subMsg : subMessages)
				addSubMessage(subMsg);
		}
		this.metaData = smd != null ?
				smd :
				initMetaData(null);
	}

	protected SwiftMetaData initMetaData(Map<String, String> tags)
	{
		return new SwiftMetaData(tags);
	}

	@Override
	public void addField(String name, String value)
	{
		if (!metaData.addIfNeeded(name, value))
			tags.put(name, value);
	}

	@Override
	public void removeField(String name)
	{
		tags.remove(name);
	}

	@Override
	public String getField(String name)
	{
		String value = tags.get(name);
		return value != null ? value : this.metaData.getMetaDataValue(name);
	}
	
	@Override
	public Map<String, String> getFields()
	{
		return tags;
	}

	@Override
	public boolean isFieldSet(String name)
	{
		return tags.containsKey(name);
	}

	@JsonIgnore
	@Override
	public Set<String> getFieldNames()
	{
		return tags.keySet();
	}
	
	
	public void addFieldIfNotPresent(String name, String value)
	{
		if (!isFieldSet(name))
			addField(name, value);
	}
	
	
	@Override
	public void addSubMessage(ClearThSwiftMessage submessage)
	{
		submessage.setParent(this);
		super.addSubMessage(submessage);
	}
	
	@Override
	public void removeSubMessage(ClearThSwiftMessage submessage)
	{
		submessage.setParent(null);
		super.removeSubMessage(submessage);
	}
	
	
	public ClearThSwiftMessage getParent()
	{
		return parent;
	}
	
	public void setParent(ClearThSwiftMessage parent)
	{
		this.parent = parent;
	}
	

	@Override
	public ClearThSwiftMessage cloneMessage()
	{
		LinkedHashMap<String, String> newTags = new LinkedHashMap<String, String>(tags);
		
		List<ClearThSwiftMessage> newSubMsgs;
		if (getSubMessages().size()>0)
		{
			newSubMsgs = new ArrayList<ClearThSwiftMessage>();
			for (ClearThSwiftMessage subMsg : getSubMessages())
				newSubMsgs.add(subMsg.cloneMessage());
		}
		else
			newSubMsgs = null;
		
		return new ClearThSwiftMessage(newTags, newSubMsgs, new SwiftMetaData(metaData));
	}
	
	
	@Override
	protected Object getFieldObject(String fieldName)
	{
		return tags.get(fieldName);
	}

	@Override
	public String toString()
	{
		return toString(0);
	}
	
	public String toString(int shift)
	{
		String tab;
		if (shift>0)
		{
			StringBuilder tabBuilder = new StringBuilder();
			for (int i = 0; i < shift; i++)
				tabBuilder.append("\t");
			tab = tabBuilder.toString();
		}
		else
			tab = "";
			
		LineBuilder lb = new LineBuilder();
		String metaStr = metaData.toString();
		if (!metaStr.isEmpty())
			lb.append(metaStr);
		
		for (String key : tags.keySet())
			lb.append(tab+key+"="+tags.get(key));
		if (getSubMessages().size()>0)
		{
			lb.append("");
			for (ClearThSwiftMessage subMessage : getSubMessages())
			{
				lb.append(tab+"Submessage fields:");
				lb.append(subMessage.toString(shift+1));
			}
		}
		return lb.toString();
	}

	public SwiftMetaData getMetaData()
	{
		return metaData;
	}

	public String getDictionaryMsgType()
	{
		return dictionaryMsgType;
	}

	public void setDictionaryMsgType(String dictionaryMsgType)
	{
		this.dictionaryMsgType = dictionaryMsgType;
	}
}
