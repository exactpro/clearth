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

import com.exactprosystems.clearth.connectivity.connections.clients.BasicClearThClient;
import com.exactprosystems.clearth.connectivity.connections.clients.MessageReceiverThread;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.SettingsException;

import java.io.IOException;

/**
 * Minimal {@link BasicClearThClient} implementation to check its behavior
 */
public class TestClearThClient extends BasicClearThClient
{
	public TestClearThClient(TestMessageConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}
	
	@Override
	protected void connect() throws ConnectionException, SettingsException
	{
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
		return true;
	}
	
	@Override
	protected MessageReceiverThread createReceiverThread()
	{
		return new TestReceiverThread(name, getStoredSettings().getSource(), receivedMessageQueue);
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
