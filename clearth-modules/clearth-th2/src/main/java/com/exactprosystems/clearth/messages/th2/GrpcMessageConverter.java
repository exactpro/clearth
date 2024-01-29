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

import java.util.Map.Entry;

import com.exactpro.th2.common.grpc.ListValue;
import com.exactpro.th2.common.grpc.Message;
import com.exactpro.th2.common.grpc.Value;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.messages.converters.ConversionException;

public class GrpcMessageConverter
{
	public SimpleClearThMessage convert(Message message) throws ConversionException
	{
		MessageBuilder<SimpleClearThMessage> builder = convertFields(message, createMessageBuilder());
		return builder.build();
	}
	
	
	protected MessageBuilder<SimpleClearThMessage> convertFields(Message message, MessageBuilder<SimpleClearThMessage> builder) throws ConversionException
	{
		for (Entry<String, Value> f : message.getFieldsMap().entrySet())
			builder = convertField(f.getKey(), f.getValue(), builder);
		return builder;
	}
	
	protected MessageBuilder<SimpleClearThMessage> convertField(String name, Value value, MessageBuilder<SimpleClearThMessage> builder) throws ConversionException
	{
		if (value.hasSimpleValue())
			return convertSimpleField(name, value.getSimpleValue(), builder);
		
		if (value.hasListValue())
			return convertListValue(name, value.getListValue(), builder);
		
		if (value.hasMessageValue())
			return convertMessageValue(name, value.getMessageValue(), builder);
		
		throw unsupportedValueException(name, value);
	}
	
	protected MessageBuilder<SimpleClearThMessage> convertListElement(String name, Value element, String elementDetails, MessageBuilder<SimpleClearThMessage> builder)
			throws ConversionException
	{
		if (element.hasSimpleValue())
			return convertSimpleListElement(name, element.getSimpleValue(), builder);
		
		if (element.hasListValue())
			return convertSublistElement(name, element.getListValue(), builder);
		
		if (element.hasMessageValue())
			return convertMessageListElement(name, element.getMessageValue(), builder);
		
		throw unsupportedValueException(elementDetails, element);
	}
	
	
	protected MessageBuilder<SimpleClearThMessage> convertSimpleField(String name, String value, MessageBuilder<SimpleClearThMessage> builder)
	{
		return builder.field(name, value);
	}
	
	protected MessageBuilder<SimpleClearThMessage> convertListValue(String name, ListValue list, MessageBuilder<SimpleClearThMessage> builder) throws ConversionException
	{
		int i = -1;
		for (Value element : list.getValuesList())
		{
			i++;
			builder = convertListElement(name, element, name+" #"+i, builder);
		}
		return builder;
	}
	
	protected MessageBuilder<SimpleClearThMessage> convertMessageValue(String name, Message message, MessageBuilder<SimpleClearThMessage> builder) throws ConversionException
	{
		MessageBuilder<SimpleClearThMessage> subBuilder = createMessageBuilder()
				.subMessageType(name);
		subBuilder = convertFields(message, subBuilder);
		return builder.rg(subBuilder.build());
	}
	
	
	protected MessageBuilder<SimpleClearThMessage> convertSimpleListElement(String name, String value, MessageBuilder<SimpleClearThMessage> builder)
	{
		SimpleClearThMessage subMessage = createMessageBuilder()
				.subMessageType(name)
				.field(name, value)
				.build();
		return builder.rg(subMessage);
	}
	
	protected MessageBuilder<SimpleClearThMessage> convertSublistElement(String name, ListValue list, MessageBuilder<SimpleClearThMessage> builder) throws ConversionException
	{
		MessageBuilder<SimpleClearThMessage> subBuilder = createMessageBuilder()
				.subMessageType(name);
		subBuilder = convertListValue(name, list, subBuilder);
		return builder.rg(subBuilder.build());
	}
	
	protected MessageBuilder<SimpleClearThMessage> convertMessageListElement(String name, Message message, MessageBuilder<SimpleClearThMessage> builder) throws ConversionException
	{
		return convertMessageValue(name, message, builder);
	}
	
	
	private MessageBuilder<SimpleClearThMessage> createMessageBuilder()
	{
		return new SimpleClearThMessageBuilder();
	}
	
	private ConversionException unsupportedValueException(String name, Value value)
	{
		return new ConversionException("Field '"+name+"' has unrecognized value: "+value);
	}
}
