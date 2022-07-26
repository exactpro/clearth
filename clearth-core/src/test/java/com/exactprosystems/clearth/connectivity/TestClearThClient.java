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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.SettingsException;

/**
 * Minimal {@link BasicClearThClient} implementation to check its behavior
 */
public class TestClearThClient extends BasicClearThClient<TestMessageConnection, TestConnectionSettings>
{
	private static final Logger logger = LoggerFactory.getLogger(TestClearThClient.class);
	
	public TestClearThClient(TestMessageConnection owner) throws ConnectionException, SettingsException
	{
		super(owner);
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
	protected Logger getLogger()
	{
		return logger;
	}
	
	@Override
	protected ValueGenerator getValueGenerator()
	{
		return null;
	}
	
	@Override
	protected boolean isNeedReceiverThread()
	{
		return true;
	}
	
	@Override
	protected MessageReceiverThread createReceiverThread()
	{
		return new TestReceiverThread(name, storedSettings.getSource(), receivedMessageQueue);
	}
	
	@Override
	protected boolean isNeedReceivedProcessorThread()
	{
		return storedSettings.isProcessReceived();
	}
	
	@Override
	protected boolean isConnectionBrokenError(Throwable error)
	{
		return false;
	}
	
	@Override
	protected Object doSendMessage(Object message) throws IOException, ConnectivityException
	{
		storedSettings.getTarget().add(message.toString());
		return null;
	}
	
	@Override
	protected Object doSendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		storedSettings.getTarget().add(message.getPayload().toString());
		return null;
	}
}
