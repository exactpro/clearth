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

package com.exactprosystems.clearth.data.th2.tables;

import java.io.IOException;
import java.util.Map;

import com.exactpro.th2.common.event.bean.IRow;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(using = MapRow.MapSerializer.class)
public class MapRow implements IRow
{
	private final Map<String, String> values;
	
	public MapRow(Map<String, String> values)
	{
		this.values = values;
	}
	
	
	public Map<String, String> getValues()
	{
		return values;
	}
	
	
	public static class MapSerializer extends JsonSerializer<MapRow>
	{
		@Override
		public void serialize(MapRow value, JsonGenerator gen, SerializerProvider serializers) throws IOException
		{
			//This results in flat representation, i.e. {"key1": "value1", "key2": "value2"},
			//instead of "values": {"key1": "value1", "key2": "value2"}
			gen.writeRawValue(new ObjectMapper().writeValueAsString(value.getValues()));
		}
	}
}
