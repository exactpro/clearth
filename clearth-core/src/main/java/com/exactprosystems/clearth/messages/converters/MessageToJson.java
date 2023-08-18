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

package com.exactprosystems.clearth.messages.converters;

import java.util.Map;

import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class MessageToJson implements MessageConverter<String>
{
	private final MessageToMap toMap;
	private final ObjectWriter jsonWriter;
	
	public MessageToJson()
	{
		this(new MessageToMap(), new ObjectMapper().writer());
	}
	
	public MessageToJson(MessageToMap toMap)
	{
		this(toMap, new ObjectMapper().writer());
	}
	
	public MessageToJson(MessageToMap toMap, ObjectWriter jsonWriter)
	{
		this.toMap = toMap;
		this.jsonWriter = jsonWriter;
	}
	
	
	@Override
	public String convert(SimpleClearThMessage message) throws ConversionException
	{
		try
		{
			Map<String, Object> map = toMap.convert(message);
			return jsonWriter.writeValueAsString(map);
		}
		catch (ConversionException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new ConversionException("Could not convert message to JSON", e);
		}
	}
}
