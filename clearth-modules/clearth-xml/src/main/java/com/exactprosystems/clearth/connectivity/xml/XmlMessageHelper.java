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

package com.exactprosystems.clearth.connectivity.xml;

import com.exactprosystems.clearth.connectivity.iface.MessageHelper;
import com.exactprosystems.clearth.connectivity.iface.MessageColumnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Created by victor.akimov on 2/12/16.
 */
public class XmlMessageHelper extends MessageHelper
{
	private XmlDictionary dictionary;

	private List<String> keys = new ArrayList<String>();

	public XmlMessageHelper(String dictionaryFile) throws Exception
	{
		super(dictionaryFile);
		dictionary = new XmlDictionary(dictionaryFile);
	}

	@Override
	public void getMessageDescription(List<MessageColumnNode> data, String typeMessage)
	{
		XmlMessageDesc xmlMessageDesc = getXmlMessageDesc(typeMessage);
		if(xmlMessageDesc == null)
			return;

		keys = new ArrayList<String>();
		for (XmlKeyDesc key : xmlMessageDesc.getKey())
		{
			keys.add(key.getName());
		}

		includeColumns(xmlMessageDesc.getFieldDesc(), null, data, "", keys, "");
	}

	private void includeColumns(List<XmlFieldDesc> fields, List<XmlAttributeDesc> attrs, List<MessageColumnNode> data, String xParent, List<String> keys, String margin)
	{
		if(attrs != null)
		{
			for(XmlAttributeDesc attr: attrs)
				if (attr.getName() != null)
					addData(data, new MessageColumnNode(margin+attr.getName(), true, false, xParent+"[@"+attr.getName()+"]", keys.contains(attr)), false);
		}
		for (XmlFieldDesc field : fields)
		{
			String xPath = xParent+"/"+field.getSource();

			if (field.isRepeat())
			{
				addData(data, new MessageColumnNode(margin+bold(getName(field)), field.isMandatory(), field.isRepeat(), xPath, false), false);
				includeColumns(field.getFieldDesc(), field.getAttrDesc(), data, xPath, keys, margin + MARGIN);
			}
			else
			{
				for (XmlAttributeDesc attr : field.getAttrDesc())
					if (attr.getName() != null)
						addData(data, new MessageColumnNode(margin + attr.getName(), true, false, xPath + "[@" + attr.getName() + "]", keys.contains(attr.getName())), false);


				if (field.getFieldDesc().isEmpty())
				{
					addData(data, new MessageColumnNode(margin + getName(field), field.isMandatory(), field.isRepeat(), xPath, keys.contains(field.getName())), true);
				} else
				{
					includeColumns(field.getFieldDesc(), null, data, xPath, keys, margin);
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

	private String getName(XmlFieldDesc field)
	{
		return field.getName() == null ? "" : field.getName();
	}

	protected XmlMessageDesc getXmlMessageDesc(String type)
	{
		for (Entry<String, XmlMessageDesc> md : dictionary.getMessageDescMap().entrySet())
		{
			if (type.equalsIgnoreCase(md.getKey()))
				return md.getValue();
		}
		return null;
	}

	@Override
	public List<String> getColumns()
	{
		return Arrays.asList("Name", "Mandatory", "Repetition", "XPath");
	}

	@Override
	public List<String> getMessagesNames()
	{
		List<String> messagesNames = new ArrayList<String>(dictionary.getMessageDescMap().keySet());
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
