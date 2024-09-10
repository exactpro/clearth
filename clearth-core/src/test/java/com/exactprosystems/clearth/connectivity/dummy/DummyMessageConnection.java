/*******************************************************************************
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

package com.exactprosystems.clearth.connectivity.dummy;

import com.exactprosystems.clearth.connectivity.ClearThClient;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.BasicClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.SettingsClass;
import com.exactprosystems.clearth.connectivity.listeners.factories.CustomMessageListenerFactory;
import com.exactprosystems.clearth.connectivity.listeners.factories.MessageListenerFactory;
import com.exactprosystems.clearth.utils.SettingsException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@SettingsClass(DummyConnectionSettings.class)
public class DummyMessageConnection extends BasicClearThMessageConnection
{
	public static final String TYPE = "Dummy";
	@XmlElement
	private boolean autoconnect = true;
	private boolean needReceiverProcessorThread;
	private String greetingMessage;

	public boolean isNeedReceiverProcessorThread()
	{
		return needReceiverProcessorThread;
	}

	public void setNeedReceiverProcessorThread(boolean needReceiverProcessorThread)
	{
		this.needReceiverProcessorThread = needReceiverProcessorThread;
	}

	public void setAutoconnect(boolean autoconnect)
	{
		this.autoconnect = autoconnect;
	}

	public Object getLastSentMessage()
	{
		return ((DummyClient) client).getLastSentMessage();
	}
	
	@Override
	public DummyConnectionSettings getSettings()
	{
		return (DummyConnectionSettings) settings;
	}
	
	public Object pollFirstFromReceivedMessages() throws InterruptedException
	{
		return ((DummyClient) client).pollFirstReceivedMessage();
	}
	
	public DummyClient getClient()
	{
		return (DummyClient) client;
	}

	@Override
	protected ClearThClient createClient() throws SettingsException, ConnectivityException
	{
		return new DummyClient(this);
	}

	@Override
	public boolean isAutoConnect()
	{
		return autoconnect;
	}

	@Override
	protected MessageListenerFactory createListenerFactory()
	{
		return new CustomMessageListenerFactory();
	}
	
	
	public String getGreetingMessage()
	{
		return greetingMessage;
	}
	
	public void setGreetingMessage(String greetingMessage)
	{
		this.greetingMessage = greetingMessage;
	}
}