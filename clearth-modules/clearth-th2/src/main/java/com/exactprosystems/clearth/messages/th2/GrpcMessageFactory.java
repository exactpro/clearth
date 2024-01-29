/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.messages.th2;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.MessageMetadata;
import com.exactpro.th2.common.grpc.Value;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.messages.converters.ConversionException;
import com.exactprosystems.clearth.messages.converters.MessageToMap;

public class GrpcMessageFactory
{
	private final MessageToMap converter;
	
	public GrpcMessageFactory()
	{
		converter = createConverter();
	}
	
	public GrpcMessageFactory(String flatDelimiter)
	{
		converter = createConverter(flatDelimiter);
	}
	
	
	public Message createMessage(SimpleClearThMessage message, MessageProperties properties) throws ConversionException
	{
		Map<String, Object> fieldsMap = converter.convert(message);
		
		return createMessageBuilder(fieldsMap)
				.setMetadata(createMetadata(properties))
				.build();
	}
	
	
	protected MessageToMap createConverter()
	{
		return new MessageToMap();
	}
	
	protected MessageToMap createConverter(String flatDelimiter)
	{
		return new MessageToMap(flatDelimiter);
	}
	
	
	protected Message.Builder createMessageBuilder(Map<String, Object> fieldsMap) throws ConversionException
	{
		Message.Builder builder = Message.newBuilder();
		for (Entry<String, Object> f : fieldsMap.entrySet())
		{
			String name = f.getKey();
			Value value = createValue(name, f.getValue());
			builder = builder.putFields(name, value);
		}
		return builder;
	}
	
	protected MessageMetadata createMetadata(MessageProperties messageProperties)
	{
		return MessageMetadata.newBuilder()
				.setMessageType(messageProperties.getMsgType())
				.setId(messageProperties.toMessageId())
				.build();
	}
	
	
	protected Value createValue(String name, Object value) throws ConversionException
	{
		if (value instanceof String)
		{
			String s = (String) value;
			return createSimpleValue(s);
		}
		
		if (value instanceof List)
			return createListValue(name, (List<Object>) value);
		
		if (value instanceof Map)
			return createMapValue((Map<String, Object>) value);
		
		throw unsupportedValueClass(name, value);
	}
	
	
	protected Value createSimpleValue(String value)
	{
		return Value.newBuilder()
				.setSimpleValue(value)
				.build();
	}
	
	protected Value createListValue(String name, List<Object> list) throws ConversionException
	{
		ListValue.Builder builder = ListValue.newBuilder();
		int index = 0;
		for (Object element : list)
		{
			index++;
			
			Value elementValue = createValue(name+" #"+index, element);
			builder = builder.addValues(elementValue);
		}
		
		ListValue listValue = builder.build();
		return Value.newBuilder()
				.setListValue(listValue)
				.build();
	}
	
	protected Value createMapValue(Map<String, Object> fields) throws ConversionException
	{
		Message mapValue = createMessageBuilder(fields).build();
		return Value.newBuilder()
				.setMessageValue(mapValue)
				.build();
	}
	
	
	private ConversionException unsupportedValueClass(String name, Object value)
	{
		return new ConversionException(String.format("Class of field %s (%s) is not supported",
				name, value.getClass()));
	}
}
