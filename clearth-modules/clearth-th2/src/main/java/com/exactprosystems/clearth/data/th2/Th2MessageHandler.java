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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.exactpro.th2.common.grpc.ConnectionID;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactpro.th2.common.grpc.RawMessage;
import com.exactpro.th2.common.grpc.RawMessageBatch;
import com.exactpro.th2.common.grpc.RawMessageMetadata;
import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.HandledMessageId;
import com.exactprosystems.clearth.data.MessageHandler;
import com.exactprosystems.clearth.data.MessageHandlingException;
import com.exactprosystems.clearth.data.MessageHandlingUtils;
import com.exactprosystems.clearth.data.th2.config.StorageConfig;
import com.exactprosystems.clearth.data.th2.messages.Th2MessageId;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;

public class Th2MessageHandler implements MessageHandler
{
	private final String connectionName;
	private final MessageRouter<RawMessageBatch> router;
	private final AtomicLong sentMessageIndex,
			receivedMessageIndex;
	private final String bookName;
	
	public Th2MessageHandler(String connectionName, MessageRouter<RawMessageBatch> router, StorageConfig config)
	{
		this.connectionName = connectionName;
		this.router = router;
		sentMessageIndex = initSentIndex(config);
		receivedMessageIndex = initReceivedIndex(config);
		this.bookName = config.getBook();
	}
	
	
	@Override
	public void close() throws Exception
	{
		router.close();
	}
	
	@Override
	public HandledMessageId createMessageId(ClearThMessageMetadata metadata)
	{
		Direction d;
		AtomicLong sequence;
		if (metadata.getDirection() == ClearThMessageDirection.RECEIVED)
		{
			d = Direction.FIRST;
			sequence = receivedMessageIndex;
		}
		else
		{
			d = Direction.SECOND;
			sequence = sentMessageIndex;
		}
		
		return new Th2MessageId(createMessageId(metadata.getTimestamp(), d, sequence.incrementAndGet()));
	}
	
	@Override
	public void onMessage(EncodedClearThMessage message) throws MessageHandlingException
	{
		ClearThMessageMetadata metadata = message.getMetadata();
		
		MessageID id = getMessageId(metadata);
		ByteString body = createMessageBody(message.getPayload());
		RawMessage result = createMessage(id, body, metadata);
		
		RawMessageBatch batch = wrapMessage(result);
		storeMessage(batch);
	}
	
	@Override
	public boolean isActive()
	{
		return true;
	}
	
	
	protected AtomicLong initSentIndex(StorageConfig config)
	{
		return initIndex();
	}
	
	protected AtomicLong initReceivedIndex(StorageConfig config)
	{
		return initIndex();
	}
	
	protected MessageID createMessageId(Instant timestamp, Direction direction, long sequence)
	{
		return MessageID.newBuilder()
				.setBookName(bookName)
				.setConnectionId(ConnectionID.newBuilder().setSessionAlias(connectionName).build())
				.setTimestamp(Timestamp.newBuilder()
						.setSeconds(timestamp.getEpochSecond())
						.setNanos(timestamp.getNano())
						.build())
				.setDirection(direction)
				.setSequence(sequence)
				.build();
	}
	
	protected ByteString createMessageBody(Object body) throws MessageHandlingException
	{
		if (body instanceof String)
			return ByteString.copyFromUtf8((String)body);
		else if (body instanceof byte[])
			return ByteString.copyFrom((byte[])body);
		return ByteString.copyFromUtf8(body.toString());
	}
	
	protected RawMessage createMessage(MessageID id, ByteString body, ClearThMessageMetadata metadata) throws MessageHandlingException
	{
		return RawMessage.newBuilder()
				.setMetadata(RawMessageMetadata.newBuilder()
						.setId(id)
						.build())
				.setBody(body)
				.build();
	}
	
	
	private AtomicLong initIndex()
	{
		Instant now = Instant.now();
		return new AtomicLong(TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + now.getNano());
	}
	
	private RawMessageBatch wrapMessage(RawMessage message)
	{
		return RawMessageBatch.newBuilder().addMessages(message).build();
	}
	
	private void storeMessage(RawMessageBatch message) throws MessageHandlingException
	{
		try
		{
			router.send(message);
		}
		catch (Exception e)
		{
			throw new MessageHandlingException(e);
		}
	}
	
	private MessageID getMessageId(ClearThMessageMetadata metadata) throws MessageHandlingException
	{
		HandledMessageId id = MessageHandlingUtils.getMessageId(metadata);
		if (id == null)
			throw new MessageHandlingException("Message metadata doesn't contain ID");
		if (!(id instanceof Th2MessageId))
			throw new MessageHandlingException("Message ID must be of class "+Th2MessageId.class.getCanonicalName()+", but it is "+id.getClass().getCanonicalName());
		return ((Th2MessageId)id).getId();
	}
}
