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

package com.exactprosystems.clearth.connectivity.mq;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSetting;
import com.exactprosystems.clearth.utils.LineBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import static java.lang.String.format;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class ClearThBasicMqConnectionSettings implements ClearThConnectionSettings
{
	@XmlElement
	@ConnectionSetting(name = "Host")
	private String hostname;
	@XmlElement
	@ConnectionSetting
	private int port;

	@XmlElement
	@ConnectionSetting(name = "Send queue")
	private String sendQueue;
	@XmlElement
	@ConnectionSetting(name = "Receive queue")
	private String receiveQueue;
	@XmlElement
	@ConnectionSetting(name = "Use receive queue")
	private boolean useReceiveQueue;
	
	@XmlElement
	@ConnectionSetting(name = "Read delay")
	private int readDelay;

	public ClearThBasicMqConnectionSettings()
	{
		receiveQueue = "";
		sendQueue = "";
		useReceiveQueue = true;
		hostname = null;
		port = -1;
		readDelay = 1000;
	}


	public String getHostname()
	{
		return hostname;
	}

	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}


	public int getPort()
	{
		return port;
	}

	public void setPort(int port)
	{
		this.port = port;
	}


	public String getSendQueue()
	{
		return sendQueue;
	}

	public void setSendQueue(String sendQueue)
	{
		this.sendQueue = sendQueue;
	}


	public String getReceiveQueue()
	{
		return receiveQueue;
	}

	public void setReceiveQueue(String receiveQueue)
	{
		this.receiveQueue = receiveQueue;
	}


	public boolean isUseReceiveQueue()
	{
		return useReceiveQueue;
	}

	public void setUseReceiveQueue(boolean useReceiveQueue)
	{
		this.useReceiveQueue = useReceiveQueue;
	}

	public int getReadDelay()
	{
		return readDelay;
	}

	public void setReadDelay(int readDelay)
	{
		this.readDelay = readDelay;
	}

	@Override
	public String toString()
	{
		LineBuilder sb = new LineBuilder();
		sb.add("Receive queue = ").append(receiveQueue);
		sb.add("Send queue = ").append(sendQueue);
		sb.add("Use receive queue = ").append(useReceiveQueue).eol();

		sb.add("Host = ").append(hostname);
		sb.add("Port = ").append(port);
		sb.add("Read delay = ").append(readDelay);
		
		return sb.toString();
	}
	
	@Override
	public void copyFrom(ClearThConnectionSettings settings1)
	{
		// ensure class of this is the same or super to settings1's class
		if (!this.getClass().isAssignableFrom(settings1.getClass()))
		{
			throw new IllegalArgumentException(format("Could not copy settings. " +
							"Expected settings of class '%s', got settings of class '%s'",
					this.getClass().getSimpleName(), settings1.getClass().getSimpleName()));
		}

		ClearThBasicMqConnectionSettings settings = (ClearThBasicMqConnectionSettings) settings1;

		this.receiveQueue = settings.receiveQueue;
		this.sendQueue = settings.sendQueue;
		this.useReceiveQueue = settings.useReceiveQueue;

		this.hostname = settings.hostname;
		this.port = settings.port;
		this.readDelay = settings.readDelay;
	}
}