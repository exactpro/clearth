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

package com.exactprosystems.clearth.web.misc;

/*
 * Flags in this class show what properties of the MQConnections should be edited while changing multiple connections
 * */

public class MqConPropsToEdit
{
	private boolean host, port, queueManager, channel, receiveQueue, sendQueue, readDelay, autoConnect, autoReconnect;

	
	public MqConPropsToEdit()
	{
		host = false;
		port = false;
		queueManager = false;
		channel = false;
		receiveQueue = false;
		sendQueue = false;
		readDelay = false;
		autoConnect = false;
		autoReconnect = false;
	}
	
	
	public boolean isHost()
	{
		return host;
	}

	public void setHost(boolean host)
	{
		this.host = host;
	}

	
	public boolean isPort()
	{
		return port;
	}

	public void setPort(boolean port)
	{
		this.port = port;
	}

	
	public boolean isQueueManager()
	{
		return queueManager;
	}

	public void setQueueManager(boolean queueManager)
	{
		this.queueManager = queueManager;
	}

	
	public boolean isChannel()
	{
		return channel;
	}

	public void setChannel(boolean channel)
	{
		this.channel = channel;
	}

	
	public boolean isReceiveQueue()
	{
		return receiveQueue;
	}

	public void setReceiveQueue(boolean receiveQueue)
	{
		this.receiveQueue = receiveQueue;
	}

	
	public boolean isSendQueue()
	{
		return sendQueue;
	}

	public void setSendQueue(boolean sendQueue)
	{
		this.sendQueue = sendQueue;
	}

	
	public boolean isReadDelay()
	{
		return readDelay;
	}

	public void setReadDelay(boolean readDelay)
	{
		this.readDelay = readDelay;
	}

	
	public boolean isAutoConnect()
	{
		return autoConnect;
	}

	public void setAutoConnect(boolean autoConnect)
	{
		this.autoConnect = autoConnect;
	}

	
	public boolean isAutoReconnect()
	{
		return autoReconnect;
	}

	public void setAutoReconnect(boolean autoReconnect)
	{
		this.autoReconnect = autoReconnect;
	}
}
