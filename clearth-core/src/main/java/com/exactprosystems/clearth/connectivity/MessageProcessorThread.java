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

package com.exactprosystems.clearth.connectivity;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.HandledMessageId;
import com.exactprosystems.clearth.data.MessageHandler;
import com.exactprosystems.clearth.data.MessageHandlingUtils;

public class MessageProcessorThread extends Thread
{
	protected static final Logger logger = LoggerFactory.getLogger(MessageProcessorThread.class);
	
	protected AtomicBoolean terminated = new AtomicBoolean(false);
	protected final BlockingQueue<EncodedClearThMessage> messageQueue;
	protected final MessageHandler handler;
	protected final List<MessageListener> listeners;
	protected final AtomicLong processed = new AtomicLong(0);
	
	public MessageProcessorThread(String name, final BlockingQueue<EncodedClearThMessage> messageQueue,
			MessageHandler handler, final List<MessageListener> listeners)
	{
		super(name);
		this.messageQueue = messageQueue;
		this.handler = handler;
		this.listeners = listeners;
	}
	
	public void terminate()
	{
		terminated.set(true);
	}
	
	@Override
	public void run()
	{
		logger.info("MessageProcessor Thread started");
		
		while (!terminated.get()) 
		{
			try 
			{
				if (logger.isTraceEnabled())
					logger.trace("Getting message from internal queue, messages count = " + messageQueue.size());
				EncodedClearThMessage message = messageQueue.poll(1000, TimeUnit.MILLISECONDS);
				if (message != null)
				{
					handleMessage(message);
					notifyListeners(message);
					processed.incrementAndGet();
				}
			} 
			catch (InterruptedException e) 
			{
				logger.warn("Interrupted", e);
				terminated.set(true);
			}
			
			if (this.isInterrupted()) 
			{
				logger.warn("Thread is interrupted");
				terminated.set(true);
			}
		}
		
		logger.info("MessageProcessor Thread finished");
	}
	
	private void handleMessage(EncodedClearThMessage message)
	{
		try
		{
			HandledMessageId id = addId(message);
			
			if (handler.isActive())
			{
				logger.trace("Handling messsage '{}'", id);
				handler.onMessage(message);
			}
			else
				logger.trace("Skipped handling messsage '{}'", id);
		}
		catch (Exception e)
		{
			logger.error("Error while handling message", e);
		}
	}
	
	private void notifyListeners(EncodedClearThMessage message)
	{
		for (MessageListener listener : listeners)
		{
			try
			{
				logger.trace("Notifying listener '{}' ({})", listener.getName(), listener.getType());
				listener.onMessage(message);
			}
			catch (Exception e)
			{
				logger.error("Listener '{}' ({}) thrown exception while handling message", listener.getName(), listener.getType(), e);
			}
		}
	}
	
	public long getProcessed()
	{
		return processed.get();
	}
	
	
	private HandledMessageId addId(EncodedClearThMessage message)
	{
		ClearThMessageMetadata metadata = message.getMetadata();
		if (metadata == null)
			return null;
		
		HandledMessageId id = MessageHandlingUtils.getMessageId(metadata);
		if (id == null)
		{
			id = handler.createMessageId(metadata);
			MessageHandlingUtils.setMessageId(metadata, id);
		}
		return id;
	}
}
