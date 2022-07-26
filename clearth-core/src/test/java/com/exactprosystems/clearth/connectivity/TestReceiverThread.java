/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;

public class TestReceiverThread extends MessageReceiverThread
{
	private static final Logger logger = LoggerFactory.getLogger(TestReceiverThread.class);
	
	private final BlockingQueue<String> sourceQueue;
	
	public TestReceiverThread(String name, BlockingQueue<String> sourceQueue, BlockingQueue<EncodedClearThMessage> messageQueue)
	{
		super(name, null, null, messageQueue, 0);
		
		this.sourceQueue = sourceQueue;
	}
	
	@Override
	protected void doRun()
	{
		while (!terminated.get())
		{
			try
			{
				String msg = sourceQueue.poll(1, TimeUnit.SECONDS);
				if (msg != null)
					messageQueue.add(EncodedClearThMessage.newReceivedMessage(msg));
			}
			catch (InterruptedException e)
			{
				interrupt();
				logger.error("Wait for next message interrupted", e);
				return;
			}
		}
	}
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}
}
