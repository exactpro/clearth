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

package com.exactprosystems.clearth.connectivity.ibmmq;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSetting;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSettings;
import com.exactprosystems.clearth.utils.LineBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@ConnectionSettings(order = {"hostname", "port", "queueManager", "channel", "retryAttemptCount", "retryTimeout",
		"receiveQueue", "useReceiveQueue", "sendQueue", "readDelay", "autoConnect", "autoReconnect"},
		columns = {"hostname", "queueManager", "channel", "sendQueue", "receiveQueue"})
public class IbmMqConnectionSettings extends ClearThBasicMqConnectionSettings
{
	@XmlElement
	@ConnectionSetting(name = "Read delay")
	private int readDelay;
	@XmlElement
	private int charset;

	@XmlElement
	@ConnectionSetting(name = "Queue manager")
	private String queueManager;
	@XmlElement
	@ConnectionSetting
	private String channel;

	@XmlElement
	@ConnectionSetting(name = "Auto-reconnect")
	private boolean autoReconnect;
	@XmlElement
	@ConnectionSetting(name = "Auto-connect")
	private boolean autoConnect;

	@XmlElement
	@ConnectionSetting(name = "Retry attempt count")
	private int retryAttemptCount;
	@XmlElement
	@ConnectionSetting(name = "Retry timeout")
	private long retryTimeout;

	public IbmMqConnectionSettings()
	{
		super();
		readDelay = 1000;
		charset = 850;
		queueManager = "";
		channel = null;

		autoReconnect = false;
		autoConnect = false;

		retryAttemptCount = 10;
		retryTimeout = 5000;
	}

	public IbmMqConnectionSettings(IbmMqConnectionSettings settings)
	{
		copyFrom(settings);
	}

	public int getReadDelay()
	{
		return readDelay;
	}

	public void setReadDelay(int readDelay)
	{
		this.readDelay = readDelay;
	}

	public int getCharset()
	{
		return charset;
	}

	public void setCharset(int charset)
	{
		this.charset = charset;
	}

	public String getQueueManager()
	{
		return queueManager;
	}

	public void setQueueManager(String queueManager)
	{
		this.queueManager = queueManager;
	}

	public String getChannel()
	{
		return channel;
	}

	public void setChannel(String channel)
	{
		this.channel = channel;
	}

	public boolean isAutoReconnect()
	{
		return autoReconnect;
	}

	public void setAutoReconnect(boolean autoReconnect)
	{
		this.autoReconnect = autoReconnect;
	}

	public boolean isAutoConnect()
	{
		return autoConnect;
	}

	public void setAutoConnect(boolean autoConnect)
	{
		this.autoConnect = autoConnect;
	}

	public int getRetryAttemptCount()
	{
		return retryAttemptCount;
	}

	public void setRetryAttemptCount(int retryAttemptCount)
	{
		this.retryAttemptCount = retryAttemptCount;
	}

	public long getRetryTimeout()
	{
		return retryTimeout;
	}

	public void setRetryTimeout(long retryTimeout)
	{
		this.retryTimeout = retryTimeout;
	}

	@Override
	public String toString()
	{
		LineBuilder sb = new LineBuilder();
		sb.append(super.toString()).eol();

		sb.add("Read delay = ").append(readDelay);
		sb.add("Charset = ").append(charset);
		sb.add("Queue manager = ").append(queueManager);
		sb.add("Channel = " ).append(channel).eol();

		sb.add("Auto-reconnect = ").append(autoReconnect);
		sb.add("Auto-connect = ").append(autoConnect);
		sb.add("Retry attempt count = ").append(retryAttemptCount);
		sb.add("Retry timeout = ").append(retryTimeout);

		return sb.toString();
	}

	@Override
	public IbmMqConnectionSettings copy()
	{
		return new IbmMqConnectionSettings(this);
	}

	@Override
	public void copyFrom(ClearThConnectionSettings settings1)
	{
		super.copyFrom(settings1);
		IbmMqConnectionSettings settings = (IbmMqConnectionSettings) settings1; 
		
		this.readDelay = settings.readDelay;
		this.charset = settings.charset;
		this.queueManager = settings.queueManager;
		this.channel = settings.channel;
		this.autoReconnect = settings.autoReconnect;
		this.autoConnect = settings.autoConnect;
		this.retryAttemptCount = settings.retryAttemptCount;
		this.retryTimeout = settings.retryTimeout;
	}
}