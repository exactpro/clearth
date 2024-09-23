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

package com.exactprosystems.clearth.connectivity;

import com.exactprosystems.clearth.connectivity.connections.clients.MessageReceiverThread;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestReceiverThread extends MessageReceiverThread
{
	private final AtomicInteger counter;
	private final BlockingQueue<String> sourceQueue;
	
	public TestReceiverThread(String name, BlockingQueue<String> sourceQueue, AtomicInteger counter, BlockingQueue<EncodedClearThMessage> messageQueue)
	{
		super(name, null, messageQueue, 0);
		
		this.counter = counter;
		this.sourceQueue = sourceQueue;
	}

	@Override
	protected void getAndHandleMessage() throws Exception
	{
		String msg = sourceQueue.poll(1, TimeUnit.SECONDS);
		if (msg != null)
		{
			counter.incrementAndGet();
			receivedMessageQueue.add(EncodedClearThMessage.newReceivedMessage(msg));
		}
	}
}
