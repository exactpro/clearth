/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ClearThMessageMetadata
{
	private ClearThMessageDirection direction;
	private Instant timestamp;
	private Map<String, Object> fields;
	
	public ClearThMessageMetadata()
	{
	}
	
	public ClearThMessageMetadata(ClearThMessageDirection direction, Instant timestamp, Map<String, Object> fields)
	{
		this.direction = direction;
		this.timestamp = timestamp;
		this.fields = fields;
	}
	
	public ClearThMessageMetadata(ClearThMessageMetadata copyFrom)
	{
		this.direction = copyFrom.getDirection();
		this.timestamp = copyFrom.getTimestamp();
		
		Map<String, Object> copyFields = copyFrom.getFields();
		if (copyFields != null)
			this.fields = new HashMap<>(copyFields);
	}
	
	
	@Override
	public int hashCode()
	{
		return Objects.hash(direction, fields, timestamp);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ClearThMessageMetadata other = (ClearThMessageMetadata) obj;
		return direction == other.direction && Objects.equals(fields, other.fields) && timestamp == other.timestamp;
	}
	
	
	public ClearThMessageDirection getDirection()
	{
		return direction;
	}
	
	public void setDirection(ClearThMessageDirection direction)
	{
		this.direction = direction;
	}
	
	
	public Instant getTimestamp()
	{
		return timestamp;
	}
	
	public void setTimestamp(Instant timestamp)
	{
		this.timestamp = timestamp;
	}
	
	
	public Object getField(String name)
	{
		return fields == null ? null : fields.get(name);
	}
	
	public void addField(String name, Object value)
	{
		createFieldsIfNeeded();
		fields.put(name, value);
	}
	
	
	public Map<String, Object> getFields()
	{
		return fields;
	}
	
	
	protected void createFieldsIfNeeded()
	{
		if (fields == null)
			fields = new HashMap<>();
	}
}
