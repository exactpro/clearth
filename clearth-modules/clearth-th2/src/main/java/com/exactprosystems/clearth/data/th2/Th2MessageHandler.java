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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.th2.common.schema.message.MessageRouter;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.Direction;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.GroupBatch;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageGroup;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.RawMessage;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.HandledMessageId;
import com.exactprosystems.clearth.data.MessageHandler;
import com.exactprosystems.clearth.data.MessageHandlingException;
import com.exactprosystems.clearth.data.MessageHandlingUtils;
import com.exactprosystems.clearth.data.th2.config.StorageConfig;
import com.exactprosystems.clearth.data.th2.messages.Th2MessageId;

public class Th2MessageHandler implements MessageHandler
{
	private static final Logger logger = LoggerFactory.getLogger(Th2MessageHandler.class);
	
	private final String connectionName;
	private final MessageRouter<GroupBatch> router;
	private final AtomicLong sentMessageIndex,
			receivedMessageIndex;
	private final String bookName;
	
	public Th2MessageHandler(String connectionName, MessageRouter<GroupBatch> router, String bookName, StorageConfig config)
	{
		this.connectionName = connectionName;
		this.router = router;
		sentMessageIndex = initSentIndex(config);
		receivedMessageIndex = initReceivedIndex(config);
		this.bookName = bookName;
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
			d = Direction.INCOMING;
			sequence = receivedMessageIndex;
		}
		else
		{
			d = Direction.OUTGOING;
			sequence = sentMessageIndex;
		}
		
		return new Th2MessageId(createMessageId(metadata.getTimestamp(), d, sequence.incrementAndGet()));
	}
	
	@Override
	public void onMessage(EncodedClearThMessage message) throws MessageHandlingException
	{
		ClearThMessageMetadata metadata = message.getMetadata();
		
		MessageId id = getMessageId(metadata);
		byte[] body = createMessageBody(message.getPayload());
		RawMessage result = createMessage(id, body, metadata);
		
		GroupBatch batch = wrapMessage(result);
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
	
	protected MessageId createMessageId(Instant timestamp, Direction direction, long sequence)
	{
		return MessageId.builder()
				.setSessionAlias(connectionName)
				.setTimestamp(timestamp)
				.setDirection(direction)
				.setSequence(sequence)
				.build();
	}
	
	protected byte[] createMessageBody(Object body) throws MessageHandlingException
	{
		if (body instanceof String)
			return ((String)body).getBytes(StandardCharsets.UTF_8);
		else if (body instanceof byte[])
			return (byte[])body;
		return body.toString().getBytes(StandardCharsets.UTF_8);
	}
	
	protected RawMessage createMessage(MessageId id, byte[] body, ClearThMessageMetadata metadata) throws MessageHandlingException
	{
		return RawMessage.builder()
				.setId(id)
				.setBody(body)
				.build();
	}
	
	
	private AtomicLong initIndex()
	{
		Instant now = Instant.now();
		return new AtomicLong(TimeUnit.SECONDS.toNanos(now.getEpochSecond()) + now.getNano());
	}
	
	private GroupBatch wrapMessage(RawMessage message)
	{
		MessageId id = message.getId();
		String sessionGroup = id.getSessionGroup(),
				batchSession = StringUtils.isEmpty(sessionGroup) ? id.getSessionAlias() : sessionGroup,
				book = id.getBook(),
				batchBook = StringUtils.isEmpty(book) ? bookName : book;
		return GroupBatch.builder()
				.setBook(batchBook)
				.setSessionGroup(batchSession)
				.addGroup(MessageGroup.builder()
						.addMessage(message)
						.build())
				.build();
	}
	
	private void storeMessage(GroupBatch message) throws MessageHandlingException
	{
		try
		{
			logger.trace("Storing message: {}", message);
			router.send(message);
		}
		catch (Exception e)
		{
			throw new MessageHandlingException(e);
		}
	}
	
	private MessageId getMessageId(ClearThMessageMetadata metadata) throws MessageHandlingException
	{
		HandledMessageId id = MessageHandlingUtils.getMessageId(metadata);
		if (id == null)
			throw new MessageHandlingException("Message metadata doesn't contain ID");
		if (!(id instanceof Th2MessageId))
			throw new MessageHandlingException("Message ID must be of class "+Th2MessageId.class.getCanonicalName()+", but it is "+id.getClass().getCanonicalName());
		return ((Th2MessageId)id).getId();
	}
}
