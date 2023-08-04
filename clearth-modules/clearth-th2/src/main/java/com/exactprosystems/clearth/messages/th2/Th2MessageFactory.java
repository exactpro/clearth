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

package com.exactprosystems.clearth.messages.th2;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.Direction;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.EventId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage.FromMapBuilder;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.messages.converters.ConversionException;
import com.exactprosystems.clearth.messages.converters.MessageToMap;

public class Th2MessageFactory
{
	private static final Set<String> SERVICE_FIELDS = Set.of(ClearThMessage.MSGTYPE, ClearThMessage.SUBMSGTYPE, 
			ClearThMessage.SUBMSGSOURCE, MessageToMap.SUBMSGKIND);
	
	private final MessageToMap converter;
	
	public Th2MessageFactory()
	{
		converter = new MessageToMap(SERVICE_FIELDS);
	}
	
	public Th2MessageFactory(String flatDelimiter)
	{
		converter = new MessageToMap(flatDelimiter, SERVICE_FIELDS);
	}
	
	
	public ParsedMessage createParsedMessage(SimpleClearThMessage message, String sessionAlias, EventId parentEvent) throws ConversionException
	{
		String msgType = getMessageType(message);
		MessageId id = createId(sessionAlias, message);
		
		Map<String, Object> fieldsMap = converter.convert(message);
		return createParsedMessage(msgType, id, fieldsMap, parentEvent);
	}
	
	
	protected String getMessageType(SimpleClearThMessage message)
	{
		return message.getField(ClearThMessage.MSGTYPE);
	}
	
	protected MessageId createId(String sessionAlias, SimpleClearThMessage message)
	{
		return MessageId.builder()
				.setSessionAlias(sessionAlias)
				.setTimestamp(Instant.now())
				.setDirection(Direction.OUTGOING)
				.setSequence(0)  //Must be zero for ParsedMessage being sent for encoding and forwarding to th2-conn
				.build();
	}
	
	protected ParsedMessage createParsedMessage(String msgType, MessageId id, Map<String, Object> fieldsMap, EventId parentEvent)
	{
		FromMapBuilder builder = ParsedMessage.builder()
				.setType(msgType)
				.setId(id);
		
		if (parentEvent != null)
			builder.setEventId(parentEvent);
		
		fieldsMap.forEach((key, value) -> builder.addField(key, value));
		return builder.build();
	}
}
