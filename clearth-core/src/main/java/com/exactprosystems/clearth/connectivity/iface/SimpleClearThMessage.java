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

package com.exactprosystems.clearth.connectivity.iface;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

public class SimpleClearThMessage extends ClearThMessage<SimpleClearThMessage>
{
	private final Map<String, String> fields = new LinkedHashMap<String, String>();
	
	
	public SimpleClearThMessage()
	{
		this(null, null);
	}
	
	public SimpleClearThMessage(Map<String, String> fields)
	{
		this(fields, null);
	}
	
	public SimpleClearThMessage(Map<String, String> fields, Collection<SimpleClearThMessage> subMessages)
	{
		if (isNotEmpty(fields))
			this.fields.putAll(fields);
		if (isNotEmpty(subMessages))
			getSubMessages().addAll(subMessages);
	}
	

	@Override
	public void addField(String name, String value)
	{
		fields.put(name, value);
	}

	@Override
	public void removeField(String name)
	{
		fields.remove(name);
	}

	@Override
	public String getField(String name)
	{
		return fields.get(name);
	}

	@Override
	public Map<String, String> getFields()
	{
		return fields;
	}

	@Override
	public boolean isFieldSet(String name)
	{
		return fields.containsKey(name);
	}

	@Override
	public Set<String> getFieldNames()
	{
		return fields.keySet();
	}

	@Override
	public SimpleClearThMessage cloneMessage()
	{
		SimpleClearThMessage copy = new SimpleClearThMessage(this.fields);
		if (hasSubMessages())
		{
			for (SimpleClearThMessage subMessage : getSubMessages())
			{
				copy.addSubMessage(subMessage.cloneMessage());
			}
		}
		copy.setEncodedMessage(this.getEncodedMessage());
		return copy;
	}

	@Override
	protected Object getFieldObject(String fieldName)
	{
		return fields.get(fieldName);
	}
}
