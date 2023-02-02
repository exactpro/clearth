/*******************************************************************************
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

package com.exactprosystems.clearth.connectivity.connections.clients;

import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MessageReceiverThread extends Thread
{
	private final static Logger logger = LoggerFactory.getLogger(MessageReceiverThread.class);
	protected final ClearThMessageConnection owner;
	protected final BlockingQueue<EncodedClearThMessage> receivedMessageQueue;
	protected final long readDelay;
	protected volatile LocalDateTime startTime, endTime;
	protected AtomicBoolean terminated = new AtomicBoolean(false);

	public MessageReceiverThread(String name, ClearThMessageConnection owner,
	                             BlockingQueue<EncodedClearThMessage> receivedMessageQueue, long readDelay)
	{
		super(name);

		this.owner = owner;
		this.receivedMessageQueue = receivedMessageQueue;
		this.readDelay = readDelay;
	}

	@Override
	public void run()
	{
		terminated.set(false);
		startTime = LocalDateTime.now();
		beforeRun();
		try
		{
			while (!terminated.get())
			{
				try
				{
					getAndHandleMessage();
					
					if (!terminated.get())
						sleep(readDelay);
				}
				catch (Exception e)
				{
					handleException(e);
				}
			}
		}
		finally
		{
			endTime = LocalDateTime.now();
			afterRun();
		}
	}
	
	public LocalDateTime getStarted()
	{
		return startTime;
	}
	
	public LocalDateTime getStopped()
	{
		return endTime;
	}

	protected abstract void getAndHandleMessage() throws Exception;

	protected void handleException(Exception e)
	{
		if (e != null)
		{
			if (e instanceof InterruptedException)
				logger.warn("Read delay interrupted, stopping receiver");
			else if (logger.isDebugEnabled())
				logger.warn("Stopping receiver thread '" + getName() + "' due to error", e);
			else
				logger.warn("Stopping receiver thread '" + getName() + "' due to error with message: " + e.getMessage());

			terminate();
		}
	}

	protected void beforeRun()
	{
	}

	protected void afterRun()
	{
	}

	public void terminate()
	{
		terminated.set(true);
	}
}