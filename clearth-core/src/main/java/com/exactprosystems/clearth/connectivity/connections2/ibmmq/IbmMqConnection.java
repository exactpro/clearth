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

package com.exactprosystems.clearth.connectivity.connections2.ibmmq;

import com.exactprosystems.clearth.connectivity.ClearThClient;
import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.connections2.BasicClearThMessageConnection;
import com.exactprosystems.clearth.utils.SettingsException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class IbmMqConnection extends BasicClearThMessageConnection
{
	@Override
	public IbmMqConnectionSettings getSettings()
	{
		return (IbmMqConnectionSettings) settings;
	}
	public int getReadDelay()
	{
		return getSettings().getReadDelay();
	}

	public void setReadDelay(int readDelay)
	{
		getSettings().setReadDelay(readDelay);
	}


	public int getCharset()
	{
		return getSettings().getCharset();
	}

	public void setCharset(int charset)
	{
		getSettings().setCharset(charset);
	}


	public String getReceiveQueue()
	{
		return getSettings().getReceiveQueue();
	}

	public void setReceiveQueue(String receiveQueue)
	{
		getSettings().setReceiveQueue(receiveQueue);
	}


	public String getSendQueue()
	{
		return getSettings().getSendQueue();
	}

	public void setSendQueue(String sendQueue)
	{
		getSettings().setSendQueue(sendQueue);
	}
	
	public boolean isUseReceiveQueue()
	{
		return getSettings().isUseReceiveQueue();
	}

	public void setUseReceiveQueue(boolean useReceiveQueue)
	{
		getSettings().setUseReceiveQueue(useReceiveQueue);
	}


	public String getHostname()
	{
		return getSettings().getHostname();
	}

	public void setHostname(String hostname)
	{
		getSettings().setHostname(hostname);
	}


	public int getPort()
	{
		return getSettings().getPort();
	}

	public void setPort(int port)
	{
		getSettings().setPort(port);
	}


	public String getQueueManager()
	{
		return getSettings().getQueueManager();
	}

	public void setQueueManager(String queueManager)
	{
		getSettings().setQueueManager(queueManager);
	}


	public String getChannel()
	{
		return getSettings().getChannel();
	}

	public void setChannel(String channel)
	{
		getSettings().setChannel(channel);
	}


	public boolean isAutoReconnect()
	{
		return getSettings().isAutoReconnect();
	}

	public void setAutoReconnect(boolean autoReconnect)
	{
		getSettings().setAutoReconnect(autoReconnect);
	}

	public int getRetryAttemptCount()
	{
		return getSettings().getRetryAttemptCount();
	}

	public void setRetryAttemptCount(int retryAttemptCount)
	{
		getSettings().setRetryAttemptCount(retryAttemptCount);
	}

	public long getRetryTimeout() 
	{
		return getSettings().getRetryTimeout();
	}

	public void setRetryTimeout(long retryTimeout) 
	{
		getSettings().setRetryTimeout(retryTimeout);
	}

	@Override
	public boolean isAutoConnect()
	{
		return getSettings().isAutoConnect();
	}

	public void setAutoConnect(boolean autoConnect)
	{
		getSettings().setAutoConnect(autoConnect);
	}
	
	@Override
	protected ClearThClient createClient() throws SettingsException, ConnectionException
	{
		return new DefaultIbmMqClient(this);
	}
	
	@Override
	public IbmMqConnection copy()
	{
		IbmMqConnection other = new IbmMqConnection();
		other.copyFrom(this);
		return other;
	}

	@Override
	protected IbmMqConnectionSettings createSettings()
	{
		return new IbmMqConnectionSettings();
	}
}