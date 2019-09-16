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

import com.exactprosystems.clearth.connectivity.iface.MessageColumnNode;
import com.exactprosystems.clearth.connectivity.iface.MessageHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by victor.akimov on 2/12/16.
 */
public class SwiftMessageHelper extends MessageHelper
{
	private Map<String, SwiftMessageDesc> messageDescMap;

	private List<String> keys = new ArrayList<String>();

	public SwiftMessageHelper(String dictionaryFile) throws Exception
	{
		super(dictionaryFile);
		messageDescMap = new SwiftDictionary(dictionaryFile).getMessageDescMap();
	}

	@Override
	public void getMessageDescription(List<MessageColumnNode> data, String typeMessage)
	{
		SwiftMessageDesc xmlMessageDesc = getXMLMessageDesc(typeMessage);
		if(xmlMessageDesc == null)
			return;

		keys = new ArrayList<String>();
		for (SwiftKeyDesc key : xmlMessageDesc.getKey())
		{
			keys.add(key.getName());
		}

		includeColumns(xmlMessageDesc.getFieldDesc(), data, keys, "");
	}

	private void includeColumns(List<SwiftFieldDesc> fields, List<MessageColumnNode> data, List<String> keys, String margin)
	{
		for (SwiftFieldDesc field : fields)
		{
			String xPath = ":"+field.getTag()+":" + (field.getValuePrefix() != null ? field.getValuePrefix() : "");

			if (field.isRepeat())
			{
				addData(data, new MessageColumnNode(margin+bold(field.getName()), field.isMandatory(), field.isRepeat(), "", false), false);
				includeColumns(field.getFieldDesc(), data, keys, margin + MARGIN);
			}
			else
			{
				if (field.getFieldDesc().isEmpty())
				{
					String subValue = field.getSubvalue() != null ? " / " +field.getSubvalue() : "";
					addData(data, new MessageColumnNode(margin + field.getName() + subValue, field.isMandatory(), field.isRepeat(), xPath, keys.contains(field.getName())), true);
				} else
				{
					includeColumns(field.getFieldDesc(), data, keys, margin);
				}
			}
		}
	}

	public static String bold(String text)
	{
		return "<b>"+text+"</b>";
	}

	private void addData(List<MessageColumnNode> data, MessageColumnNode messageColumnNode, boolean addXpath)
	{
		for (MessageColumnNode mcn : data)
		{
			if (mcn.getName().equals(messageColumnNode.getName()) && mcn.getRepetitive() == messageColumnNode.getRepetitive())
			{
				if (addXpath)
					mcn.addInfoInDescription("<br/>" + messageColumnNode.getDescription());
				return;
			}
		}
		data.add(messageColumnNode);
	}

	protected SwiftMessageDesc getXMLMessageDesc(String type)
	{
		for (Map.Entry<String, SwiftMessageDesc> entry : messageDescMap.entrySet())
		{
			if (type.equalsIgnoreCase(entry.getKey()))
				return entry.getValue();
		}
		return null;
	}

	@Override
	public List<String> getColumns()
	{
		return Arrays.asList("Name / Subvalue", "Mandatory", "Repetition", "Tag");
	}

	@Override
	public List<String> getMessagesNames()
	{
		List<String> messagesNames = new ArrayList<String>();
		for(String messageType : messageDescMap.keySet())
		{
			if(!messagesNames.contains(messageType))
				messagesNames.add(messageType);
		}
		Collections.sort(messagesNames);
		return messagesNames;
	}

	@Override
	public String getDirection()
	{
		return null;
	}

	@Override
	public List<String> getKeys()
	{
		return keys;
	}
}
