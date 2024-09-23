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

import com.exactprosystems.clearth.connectivity.connections.clients.BasicClearThClient;
import com.exactprosystems.clearth.connectivity.connections.clients.MessageReceiverThread;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.SettingsException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal {@link BasicClearThClient} implementation to check its behavior
 */
public class TestClearThClient extends BasicClearThClient
{
	private AtomicInteger readCounter;
	
	public TestClearThClient(TestMessageConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}
	
	@Override
	protected void connect() throws ConnectionException, SettingsException
	{
		readCounter = new AtomicInteger();
	}
	
	private TestConnectionSettings getStoredSettings()
	{
		return (TestConnectionSettings) storedSettings;
	}



	@Override
	protected boolean isNeedReceivedProcessorThread()
	{
		return ((TestConnectionSettings) storedSettings).isProcessReceived();
	}
	
	@Override
	protected void closeConnections() throws ConnectionException
	{
	}
	
	@Override
	protected boolean isNeedReceiverThread()
	{
		//It is created in connect() and its presence indicates that messages must be read by the connection.
		//If connect() and isNeedReceiverThread() are called in wrong order, readCounter will be null, messages will be not read and tests will fail
		return readCounter != null;
	}
	
	@Override
	protected MessageReceiverThread createReceiverThread()
	{
		return new TestReceiverThread(name, getStoredSettings().getSource(), readCounter, receivedMessageQueue);
	}
	
	@Override
	protected EncodedClearThMessage doSendMessage(Object message) throws IOException, ConnectivityException
	{
		getStoredSettings().getTarget().add(message.toString());
		return null;
	}
	
	@Override
	protected EncodedClearThMessage doSendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		getStoredSettings().getTarget().add(message.getPayload().toString());
		return null;
	}
}
