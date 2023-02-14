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

package com.exactprosystems.clearth.data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;

public class DummyMessageHandler implements MessageHandler
{
	private final BlockingQueue<EncodedClearThMessage> sentMessages = new LinkedBlockingQueue<>(),
			receivedMessages = new LinkedBlockingQueue<>();
	private boolean active;
	
	public DummyMessageHandler(boolean active)
	{
		this.active = active;
	}
	
	@Override
	public void close() throws Exception
	{
	}
	
	@Override
	public HandledMessageId createMessageId(ClearThMessageMetadata metadata)
	{
		return new UuidMessageId();
	}
	
	@Override
	public void onMessage(EncodedClearThMessage message) throws MessageHandlingException
	{
		ClearThMessageMetadata metadata = message.getMetadata();
		HandledMessageId id = MessageHandlingUtils.getMessageId(metadata);
		if (id == null)
			throw new MessageHandlingException("No ID stored in message metadata");
		if (!(id instanceof UuidMessageId))
			throw new MessageHandlingException("Stored ID has wrong class - "+id.getClass().getCanonicalName());
		
		try
		{
			if (metadata.getDirection() == ClearThMessageDirection.SENT)
				sentMessages.add(message);
			else
				receivedMessages.add(message);
		}
		catch (Exception e)
		{
			 throw new MessageHandlingException("Could not save message", e);
		}
	}
	
	@Override
	public boolean isActive()
	{
		return active;
	}
	
	
	public void setActive(boolean active)
	{
		this.active = active;
	}
	
	public EncodedClearThMessage pollSentMessage(long timeout, TimeUnit unit) throws InterruptedException
	{
		return pollMessage(sentMessages, timeout, unit);
	}
	
	public EncodedClearThMessage pollReceivedMessage(long timeout, TimeUnit unit) throws InterruptedException
	{
		return pollMessage(receivedMessages, timeout, unit);
	}
	
	
	private EncodedClearThMessage pollMessage(BlockingQueue<EncodedClearThMessage> messages, long timeout, TimeUnit unit) throws InterruptedException
	{
		return messages.poll(timeout, unit);
	}
}
