/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.messages;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ReceivedClearThMessage;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that gets messages from {@link ClearThMessageCollector}
 * @author vladimir.panarin
 */
public class CollectorMessageSource implements MessageSource, StringMessageSource
{
	private static final Logger log = LoggerFactory.getLogger(CollectorMessageSource.class);
	
	public static final String ORIGINAL_TEXT_NOT_STORED = "Could not get original message text";

	protected final ClearThMessageCollector collector;
	protected long currentId;
	protected long lastMessageId;
	protected long startingReadTime;
	protected Deque<ReceivedClearThMessage> messagesBuffer;
	protected boolean canReadEarlyMessages;
	protected final boolean directOrder;
	
	/**
	 * Creates message source that will return messages, starting from first one, in direct or reversed order
	 * @param collector to get messages from
	 * @param directOrder flag that defines if messages, retrieved from collector, should be returned in direct or reversed order
	 */
	public CollectorMessageSource(ClearThMessageCollector collector, boolean directOrder)
	{
		this.collector = collector;
		this.directOrder = directOrder;
		canReadEarlyMessages = false;
		currentId = -1;
		lastMessageId = -1;
		messagesBuffer = getAllMessages();
	}
	
	/**
	 * Creates message source that will return messages, starting from the one which was received after given time value, in direct or reversed order
	 * @param collector to get messages from
	 * @param afterTime number of milliseconds from January 1, 1970, UTC. All messages received before this time will be skipped by message source
	 * @param directOrder flag that defines if messages, retrieved from collector, should be returned in direct or reversed order
	 */
	public CollectorMessageSource(ClearThMessageCollector collector, long afterTime, boolean directOrder)
	{
		this.collector = collector;
		this.directOrder = directOrder;
		currentId = -1;
		lastMessageId = -1;
		messagesBuffer = getMessagesAfterTime(lastMessageId, afterTime);
		canReadEarlyMessages = messagesBuffer.isEmpty();
		updateCurrentId();
	}
	
	
	@Override
	public ClearThMessage<?> nextMessage() throws IOException
	{
		if (messagesBuffer.isEmpty()) { //No messages left in buffer, need to get next ones
			if (canReadEarlyMessages) {
				messagesBuffer = getMessagesAfterTime(lastMessageId, startingReadTime);
				if (!messagesBuffer.isEmpty()) {
					canReadEarlyMessages = false;
				}
			} else {
				messagesBuffer = getMessages(lastMessageId);
			}
		}

		ReceivedClearThMessage msg = directOrder ? messagesBuffer.pollFirst() : messagesBuffer.pollLast();

		if (msg == null)  //No more messages in collector
			return null;
		currentId = msg.getId();
		return msg.getMessage();
	}
	
	@Override
	public String nextStringMessage() throws IOException
	{
		ClearThMessage<?> msg = nextMessage();
		if (msg == null)
			return null;

		String stringMessage = msg.getEncodedMessage();
		if (stringMessage == null)
		{
			log.warn(ORIGINAL_TEXT_NOT_STORED);
			stringMessage = ORIGINAL_TEXT_NOT_STORED;
		}
		return stringMessage;
	}
	
	@Override
	public void removeMessage()
	{
		if (currentId < 0)
			return;
		collector.removeMessage(currentId);
	}
	
	@Override
	public void removeMessage(ClearThMessage<?> message)
	{
		collector.removeMessage(message);
	}
	
	protected Deque<ReceivedClearThMessage> prepareMessages(Collection<ReceivedClearThMessage> messages)
	{
		if(messages instanceof Deque)
			return (Deque<ReceivedClearThMessage>) messages;
		else
			return new ArrayDeque<ReceivedClearThMessage>(messages);
	}

	protected void updateCurrentId() {
		ReceivedClearThMessage firstMessage = messagesBuffer.peekFirst();
		if (firstMessage != null)
			currentId = firstMessage.getId();
	}
	
	protected void updateLastId(Deque<ReceivedClearThMessage> messages) {
		ReceivedClearThMessage lastMessage = messages.peekLast();
		if (lastMessage != null)
			lastMessageId = lastMessage.getId();
	}
	
	protected Deque<ReceivedClearThMessage> getAllMessages()
	{
		Deque<ReceivedClearThMessage> result = prepareMessages(collector.getMessagesData());
		updateLastId(result);
		return result;
	}
	
	protected Deque<ReceivedClearThMessage> getMessages(long afterId)
	{
		Deque<ReceivedClearThMessage> result = prepareMessages(collector.getMessagesData(afterId));
		updateLastId(result);
		return result;
	}

	protected Deque<ReceivedClearThMessage> getMessagesAfterTime(long afterId, long afterTime)
	{
		Deque<ReceivedClearThMessage> result = prepareMessages(collector.getMessagesData(afterId));
		updateLastId(result);
		ReceivedClearThMessage firstMessage = result.peekFirst();
		while (firstMessage != null) {
			if (afterTime < firstMessage.getReceived()) {
				return result;
			}
			result.removeFirst();
			firstMessage = result.peekFirst();
		}
		return result;
	}
}
