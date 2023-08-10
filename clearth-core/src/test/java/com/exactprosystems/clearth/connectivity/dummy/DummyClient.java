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

package com.exactprosystems.clearth.connectivity.dummy;

import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.clients.BasicClearThClient;
import com.exactprosystems.clearth.connectivity.connections.clients.MessageReceiverThread;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.MessageHandler;
import com.exactprosystems.clearth.utils.SettingsException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.TimeUnit.SECONDS;

public class DummyClient extends BasicClearThClient
{
	final BlockingQueue<Object> messagesToReceive = new LinkedBlockingQueue<>();
	LinkedList<Object> sentMessagesHistory = new LinkedList<>();
	private boolean needReceivedProcessorThread;
	
	public DummyClient(DummyMessageConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
		needReceivedProcessorThread = owner.isNeedReceiverProcessorThread();
	}

	@Override
	protected boolean isNeedReceivedProcessorThread()
	{
		return needReceivedProcessorThread;
	}
	
	public DummyMessageConnection getOwner()
	{
		return (DummyMessageConnection) owner;
	}
	
	public Object pollFirstReceivedMessage() throws InterruptedException
	{
		EncodedClearThMessage message = receivedMessageQueue.poll(1, SECONDS);
		return message == null ? null : message.getPayload();
	}
	
	public Object getLastSentMessage()
	{
		return sentMessagesHistory.getLast();
	}

	public void putDirectlyToReceivedMessages(Object o) throws InterruptedException
	{
		receivedMessageQueue.put(EncodedClearThMessage.newReceivedMessage(o));
	}
	
	public MessageHandler getMessageHandler()
	{
		return messageHandler;
	}
	
	@Override
	protected void connect() throws ConnectionException, SettingsException
	{
		
	}
	
	@Override
	protected void closeConnections() throws ConnectionException
	{
		
	}

	@Override
	protected boolean isNeedReceiverThread()
	{
		return true;
	}

	@Override
	protected MessageReceiverThread createReceiverThread()
	{
		return new DummyMessageReceiverThread(getOwner());
	}

	@Override
	protected EncodedClearThMessage doSendMessage(Object message) throws IOException, ConnectivityException
	{
		messagesToReceive.add(message);
		sentMessagesHistory.add(message);
		return null;
	}

	@Override
	protected EncodedClearThMessage doSendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		return doSendMessage(message.getPayload());
	}

	private class DummyMessageReceiverThread extends MessageReceiverThread
	{
		public DummyMessageReceiverThread(DummyMessageConnection owner)
		{
			super(owner.getName(), owner, DummyClient.this.receivedMessageQueue, 0);
		}

		@Override
		protected void getAndHandleMessage() throws Exception
		{
			Object messageToReceive = messagesToReceive.poll();
			if (messageToReceive != null)
				receivedMessageQueue.add(EncodedClearThMessage.newReceivedMessage(messageToReceive));
		}

		@Override
		public void terminate()
		{
			super.terminate();
			List<Object> messages = new ArrayList<>();
			messagesToReceive.drainTo(messages);
			for (Object messageToReceive : messages)
				receivedMessageQueue.add(EncodedClearThMessage.newReceivedMessage(messageToReceive));
		}
	}
}