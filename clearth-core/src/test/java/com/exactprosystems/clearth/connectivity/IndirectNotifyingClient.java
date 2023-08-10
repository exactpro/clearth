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
import java.nio.file.Path;
import java.nio.file.Paths;

//This is example of client that returns EncodedClearThMessage as sending outcome.
//BasicClearThClient must use this EncodedClearThMessage as sending result, not generate another one, with different HandledMessageId.
public class IndirectNotifyingClient extends BasicClearThClient
{
	public IndirectNotifyingClient(IndirectNotifyingConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}
	
	@Override
	protected Path createUnhandledMessagesFilePath(String name)
	{
		return Paths.get("never-existing file");  //It doesn't use rootRelative() and thus doesn't require ClearThCore to be initialized
	}
	
	@Override
	protected void connect() throws ConnectivityException, SettingsException
	{
	}
	
	@Override
	protected void closeConnections() throws ConnectivityException
	{
	}
	
	@Override
	protected MessageReceiverThread createReceiverThread()
	{
		return null;
	}
	
	@Override
	protected boolean isNeedReceiverThread()
	{
		return false;
	}
	
	@Override
	protected boolean isNeedReceivedProcessorThread()
	{
		return false;
	}
	
	@Override
	protected boolean isNeedNotifySendListeners()
	{
		return false;  //This client will notify send listeners on its own, BasicClearThClient will not do this
	}
	
	@Override
	protected EncodedClearThMessage doSendMessage(Object message) throws IOException, ConnectivityException
	{
		EncodedClearThMessage result = createUpdatedMessage(message, null);
		notifySendListenersIndirectly(result);
		return result;
	}
	
	@Override
	protected EncodedClearThMessage doSendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		EncodedClearThMessage result = createUpdatedMessage(message.getPayload(), message.getMetadata());
		notifySendListenersIndirectly(result);
		return result;
	}
}
