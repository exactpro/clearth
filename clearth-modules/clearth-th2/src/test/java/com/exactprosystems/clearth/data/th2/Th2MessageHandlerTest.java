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

package com.exactprosystems.clearth.data.th2;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.Direction;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.GroupBatch;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.MessageHandler;
import com.exactprosystems.clearth.data.MessageHandlingUtils;
import com.exactprosystems.clearth.data.th2.config.StorageConfig;
import com.exactprosystems.clearth.data.th2.messages.Th2MessageId;

public class Th2MessageHandlerTest
{
	@Test
	public void idCreation() throws Exception
	{
		String book = "book1",
				connectionName = "con1";
		Instant timestamp = Instant.now();
		ClearThMessageMetadata metadata = new ClearThMessageMetadata(ClearThMessageDirection.RECEIVED, timestamp, null);
		StorageConfig config = new StorageConfig(null);
		
		MessageId createdId;
		try (MessageHandler handler = new Th2MessageHandler(connectionName, new CollectingRouter<>(), book, config))
		{
			createdId = ((Th2MessageId)handler.createMessageId(metadata)).getId();
		}
		Instant createdTimestamp = createdId.getTimestamp();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(createdId.getBook(), "", "Book name");  //book cannot be set while creating MessageId
		soft.assertEquals(createdId.getSessionAlias(), connectionName, "Connection name");
		soft.assertEquals(createdId.getDirection(), 
				Direction.INCOMING, 
				"Direction");
		soft.assertEquals(createdTimestamp, 
				timestamp,
				"Timestamp");
		soft.assertAll();
	}
	
	@Test(description = "Tests if sent and received messages have independent sequences")
	public void independentSequences() throws Exception
	{
		StorageConfig config = new StorageConfig(null);
		ClearThMessageMetadata sentMetadata = new ClearThMessageMetadata(ClearThMessageDirection.SENT, Instant.now(), null),
				receivedMetadata = new ClearThMessageMetadata(ClearThMessageDirection.RECEIVED, Instant.now(), null);
		
		MessageId sent1,
				sent2,
				received1,
				received3;
		try (MessageHandler handler = new Th2MessageHandler("con1", new CollectingRouter<>(), "book1", config))
		{
			sent1 = ((Th2MessageId)handler.createMessageId(sentMetadata)).getId();
			received1 = ((Th2MessageId)handler.createMessageId(receivedMetadata)).getId();
			handler.createMessageId(receivedMetadata);
			sent2 = ((Th2MessageId)handler.createMessageId(sentMetadata)).getId();
			received3 = ((Th2MessageId)handler.createMessageId(receivedMetadata)).getId();
		}
		
		Assert.assertEquals(sent2.getSequence(), sent1.getSequence()+1, "Next sequence for sent");
		Assert.assertEquals(received3.getSequence(), received1.getSequence()+2, "Next sequence for received");
	}
	
	@Test
	public void messageHandling() throws Exception
	{
		CollectingRouter<GroupBatch> router = new CollectingRouter<>();
		
		StorageConfig config = new StorageConfig(null);
		String payload = "dummy message";
		EncodedClearThMessage message = EncodedClearThMessage.newSentMessage(payload, Instant.now());
		
		Th2MessageId id;
		try (MessageHandler handler = new Th2MessageHandler("con1", router, "book1", config))
		{
			ClearThMessageMetadata metadata = message.getMetadata();
			id = (Th2MessageId)handler.createMessageId(metadata);
			MessageHandlingUtils.setMessageId(metadata, id);
			
			handler.onMessage(message);
		}
		
		List<GroupBatch> batch = router.getSent();
		Assert.assertEquals(batch.size(), 1, "Number of sent messages");
		
		RawMessage sentMessage = (RawMessage) batch.get(0).getGroups().get(0).getMessages().get(0);
		Assert.assertEquals(sentMessage.getId(), id.getId(), "Message ID");
		Assert.assertEquals(sentMessage.getBody().toString(StandardCharsets.UTF_8), payload, "Payload");
	}
}
