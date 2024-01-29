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

import java.time.Instant;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.Direction;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.EventId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.ParsedMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.messages.converters.ConversionException;

public class Th2MessageFactoryTest
{
	private Th2MessageFactory factory;
	
	@BeforeClass
	public void init()
	{
		factory = new Th2MessageFactory();
	}
	
	@Test
	public void buildFromSimpleMessage() throws ConversionException
	{
		String sessionAlias = "TestConn-1";
		SimpleClearThMessage message = MessageTestUtils.createComplexMessage();
		
		EventId parentEvent = EventId.builder()
				.setBook("Book1")
				.setId("RootEvent")
				.setScope("defaultScope")
				.setTimestamp(Instant.now())
				.build();
		
		ParsedMessage th2Message = factory.createParsedMessage(message, sessionAlias, parentEvent);
		MessageId id = th2Message.getId();
		Assert.assertEquals(id.getSessionAlias(), sessionAlias, "Session alias");
		Assert.assertNotNull(id.getTimestamp(), "Timestamp");
		Assert.assertEquals(id.getDirection(), Direction.OUTGOING, "Direction");
		Assert.assertEquals(id.getSequence(), 0, "Sequence");
		Assert.assertEquals(th2Message.getEventId(), parentEvent, "Parent event ID");
		
		Assert.assertEquals(th2Message.getBody().toString(),
				"{PlainField1=PlainValue12, PlainField2=PlainValue3, "
						+ "simpleList=["
								+ "{Field10=Value20, Field20=Value30}"
								+ "], "
						+ "simpleMap={"
								+ "MapField1=123, MapField2=234"
								+ "}"
						+ "}",
				"Message body");
	}
}
