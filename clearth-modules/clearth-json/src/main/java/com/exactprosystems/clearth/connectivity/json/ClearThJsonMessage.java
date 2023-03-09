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

package com.exactprosystems.clearth.connectivity.json;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import org.apache.commons.collections4.CollectionUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;

public class ClearThJsonMessage extends ClearThMessage<ClearThJsonMessage>
{
	public static final String ARRAY_ITEM_NAME = "value";
	
	protected final Map<String, JsonField> jsonFields;
	protected Map<String, String> textFields;
	
	protected boolean validate = true;
	
	public ClearThJsonMessage()
	{
		jsonFields = createJsonFieldsMap();
	}
	
	public ClearThJsonMessage(Map<String, String> fields, List<ClearThJsonMessage> subMessages)
	{
		this.jsonFields = createJsonFieldsMap(fields.size());
		for (Map.Entry<String, String> entry : fields.entrySet())
		{
			this.jsonFields.put(entry.getKey(), new JsonTextField(entry.getValue()));
		}
		if (CollectionUtils.isNotEmpty(subMessages))
			getSubMessages().addAll(subMessages);
	}

	protected Map<String, JsonField> createJsonFieldsMap()
	{
		return new LinkedHashMap<>();
	}

	protected Map<String, JsonField> createJsonFieldsMap(int size)
	{
		return new LinkedHashMap<>(size);
	}

	@Override
	public void addField(String name, String value)
	{
		jsonFields.put(name, new JsonTextField(value));
		textFields = null;
	}
	
	public void addField(String name, JsonField jsonField)
	{
		jsonFields.put(name, jsonField);
		textFields = null;
	}

	@Override
	public void removeField(String name)
	{
		jsonFields.remove(name);
		textFields = null;
	}

	@Override
	public String getField(String name)
	{
		JsonField jsonField = jsonFields.get(name);
		return jsonField == null ? null : jsonField.getTextValue();
	}
	
	public JsonField getJsonField(String name)
	{
		return jsonFields.get(name);
	}
	
	public Map<String, JsonField> getJsonFields()
	{
		return jsonFields;
	}

	@Override
	public Map<String, String> getFields()
	{
		if (textFields == null)
		{
			if (jsonFields.isEmpty())
				textFields = emptyMap();
			else 
			{
				textFields = new LinkedHashMap<>();
				for (Map.Entry<String, JsonField> entry : jsonFields.entrySet())
				{
					JsonField jsonField = entry.getValue();
					String textValue = (jsonField != null) ? jsonField.getTextValue() : null;
					textFields.put(entry.getKey(), textValue);
				}
			}
		}
		return textFields;
	}

	@Override
	public boolean isFieldSet(String name)
	{
		return jsonFields.containsKey(name);
	}

	@Override
	protected Set<String> getFieldsKeySet()
	{
		return jsonFields.keySet();
	}

	@Override
	public ClearThJsonMessage cloneMessage()
	{
		ClearThJsonMessage message = new ClearThJsonMessage();		
		for (Map.Entry<String, JsonField> entry : jsonFields.entrySet())
		{
			JsonField value = entry.getValue();
			message.jsonFields.put(entry.getKey(), value == null ? null : value.clone());
		}		
		if (this.hasSubMessages())
		{
			for (ClearThJsonMessage subMessage : getSubMessages())
			{
				message.addSubMessage(subMessage.cloneMessage());
			}
		}
		message.setEncodedMessage(this.getEncodedMessage());
		message.setMetadata(this.getMetadata());
		return message;
	}

	@Override
	protected Object getFieldObject(String fieldName)
	{
		return jsonFields.get(fieldName);
	}
	
	
	public boolean isValidate()
	{
		return validate;
	}

	public void setValidate(boolean validate)
	{
		this.validate = validate;
	}
}
