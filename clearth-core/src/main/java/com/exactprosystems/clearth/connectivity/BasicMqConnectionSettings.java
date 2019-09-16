/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Common settings for connections to Message Queues: IBM MQ, ActiveMQ, etc.
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class BasicMqConnectionSettings<S extends BasicMqConnectionSettings<S>> extends ClearThConnectionSettings<S>
{
	@XmlElement
	public String hostname;
	@XmlElement
	public int port;
	
	@XmlElement
	public String sendQueue;
	@XmlElement
	public String receiveQueue;
	@XmlElement
	public boolean useReceiveQueue;


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
}
