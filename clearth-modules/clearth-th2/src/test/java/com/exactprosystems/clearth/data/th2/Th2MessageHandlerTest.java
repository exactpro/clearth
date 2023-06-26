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

package com.exactprosystems.clearth.data.th2;

import java.time.Instant;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.MessageHandler;
import com.exactprosystems.clearth.data.MessageHandlingUtils;
import com.exactprosystems.clearth.data.th2.config.StorageConfig;
import com.exactprosystems.clearth.data.th2.messages.Th2MessageId;
import com.google.protobuf.Timestamp;

public class Th2MessageHandlerTest
{
	@Test
	public void idCreation() throws Exception
	{
		String book = "book1",
				connectionName = "con1";
		Instant timestamp = Instant.now();
		ClearThMessageMetadata metadata = new ClearThMessageMetadata(ClearThMessageDirection.RECEIVED, timestamp, null);
		StorageConfig config = new StorageConfig(book, null);
		
		MessageID createdId;
		try (MessageHandler handler = new Th2MessageHandler(connectionName, new CollectingRouter<>(), config))
		{
			createdId = ((Th2MessageId)handler.createMessageId(metadata)).getId();
		}
		Timestamp createdTimestamp = createdId.getTimestamp();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(createdId.getBookName(), book, "Book name");
		soft.assertEquals(createdId.getConnectionId().getSessionAlias(), connectionName, "Connection name");
		soft.assertEquals(Direction.forNumber(createdId.getDirectionValue()), 
				Direction.FIRST, 
				"Direction");
		soft.assertEquals(Instant.ofEpochSecond(createdTimestamp.getSeconds(), createdTimestamp.getNanos()), 
				timestamp,
				"Timestamp");
		soft.assertAll();
	}
	
	@Test(description = "Tests if sent and received messages have independent sequences")
	public void independentSequences() throws Exception
	{
		StorageConfig config = new StorageConfig("book1", null);
		ClearThMessageMetadata sentMetadata = new ClearThMessageMetadata(ClearThMessageDirection.SENT, Instant.now(), null),
				receivedMetadata = new ClearThMessageMetadata(ClearThMessageDirection.RECEIVED, Instant.now(), null);
		
		MessageID sent1,
				sent2,
				received1,
				received3;
		try (MessageHandler handler = new Th2MessageHandler("con1", new CollectingRouter<>(), config))
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
		CollectingRouter<RawMessageBatch> router = new CollectingRouter<>();
		
		StorageConfig config = new StorageConfig("book1", null);
		String payload = "dummy message";
		EncodedClearThMessage message = EncodedClearThMessage.newSentMessage(payload, Instant.now());
		
		Th2MessageId id;
		try (MessageHandler handler = new Th2MessageHandler("con1", router, config))
		{
			ClearThMessageMetadata metadata = message.getMetadata();
			id = (Th2MessageId)handler.createMessageId(metadata);
			MessageHandlingUtils.setMessageId(metadata, id);
			
			handler.onMessage(message);
		}
		
		List<RawMessageBatch> batch = router.getSent();
		Assert.assertEquals(batch.size(), 1, "Number of sent messages");
		
		RawMessage sentMessage = batch.get(0).getMessages(0);
		Assert.assertEquals(sentMessage.getMetadata().getId(), id.getId(), "Message ID");
		Assert.assertEquals(sentMessage.getBody().toStringUtf8(), payload, "Payload");
	}
}
