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

package com.exactprosystems.clearth.connectivity;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.utils.Pair;

public class MessageProcessorThread extends Thread
{
	protected static final Logger logger = LoggerFactory.getLogger(MessageProcessorThread.class);
	
	protected AtomicBoolean terminated = new AtomicBoolean(false);
	protected final BlockingQueue<Pair<String, Date>> messageQueue;
	protected final List<ReceiveListener> listeners;
	protected AtomicLong processed = new AtomicLong(0);
	
	public MessageProcessorThread(String name, final BlockingQueue<Pair<String, Date>> messageQueue, final List<ReceiveListener> listeners)
	{
		super(name);
		this.messageQueue = messageQueue;
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
				Pair<String, Date> pair = messageQueue.poll(1000, TimeUnit.MILLISECONDS);
				if (pair != null)
				{ 
					String message = pair.getFirst();
					notifyReceivedListeners(message);
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
	
	public void notifyReceivedListeners(String message)
	{
		for (ReceiveListener listener : listeners)
		{
			try
			{
				logger.trace("Notifying receive listener");
				listener.onMessageReceived(message);
			}
			catch (Exception e)
			{
				logger.error("Listener thrown exception while handling the message", e);
			}
		}
	}
	
	public long getProcessed()
	{
		return processed.get();
	}
}
