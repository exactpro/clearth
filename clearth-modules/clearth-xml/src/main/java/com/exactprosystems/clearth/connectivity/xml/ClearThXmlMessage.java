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

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class ClearThXmlMessage extends ClearThMessage<ClearThXmlMessage>
{
	public static final Logger logger = LoggerFactory.getLogger(ClearThXmlMessage.class);
	public static final String EMPTY_VALUE = "empty";
	
	protected final Map<String, XmlField> fields;
	protected Map<String, String> textFields;
	
	protected String namespacePrefix;
	
	public ClearThXmlMessage()
	{
		fields = new LinkedHashMap<String, XmlField>();
	}

	public ClearThXmlMessage(Map<String, String> fields, List<ClearThXmlMessage> subMessages)
	{
		this.fields = new LinkedHashMap<String, XmlField>();
		for (Map.Entry<String, String> entry : fields.entrySet())
		{
			this.fields.put(entry.getKey(), new XmlField(entry.getValue(), false));
		}
		if (subMessages!=null)
			getSubMessages().addAll(subMessages);
	}
	
	@Override
	public void addField(String name, String value)
	{
		fields.put(name, new XmlField(value, false));
		textFields = null;
	}
	
	public void addXMLField(String name, XmlField field)
	{
		fields.put(name, field);
		textFields = null;
	}

	@Override
	public ClearThXmlMessage cloneMessage()
	{
		ClearThXmlMessage cloned = new ClearThXmlMessage();
		for (Map.Entry<String, XmlField> e : fields.entrySet())
		{
			cloned.addXMLField(e.getKey(), e.getValue().clone());
		}
		for (ClearThXmlMessage subMessage : getSubMessages())
			cloned.addSubMessage(subMessage.cloneMessage());
		cloned.setEncodedMessage(this.getEncodedMessage());
		return cloned;
	}

	@Override
	public String getField(String name)
	{
		XmlField field = fields.get(name);
		if (field!=null)
			return field.getValue();
		else
			return null;
	}
	
	public XmlField getXMLField(String name)
	{
		return fields.get(name);
	}

	@Override
	public Map<String, String> getFields()
	{
		if (textFields == null)
		{
			if (fields.isEmpty())
				textFields = emptyMap();
			else 
			{
				textFields = new LinkedHashMap<String, String>();
				for (Map.Entry<String, XmlField> e : fields.entrySet())
				{
					XmlField xmlField = e.getValue();
					String textValue = (xmlField != null) ? xmlField.getValue() : null;
					textFields.put(e.getKey(), textValue);
				}
			}
		}
		return textFields;
	}

	@Override
	protected Set<String> getFieldsKeySet()
	{
		return fields.keySet();
	}

	@Override
	public boolean isFieldSet(String name)
	{
		return fields.containsKey(name);
	}

	@Override
	public void removeField(String name)
	{
		fields.remove(name);
		textFields = null;
	}
	
	@Override
	protected Object getFieldObject(String fieldName)
	{
		return fields.get(fieldName);
	}

	public String getNamespacePrefix()
	{
		return namespacePrefix;
	}

	@SuppressWarnings("unused")
	public void setNamespacePrefix(String namespacePrefix)
	{
		this.namespacePrefix = isEmpty(namespacePrefix) ? null : namespacePrefix;
	}
}
