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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.Utils;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public abstract class MQConnectionSettings extends BasicMqConnectionSettings<MQConnectionSettings>
{
	@XmlElement
	public int readDelay;
	@XmlElement
	public int charset;
	
	@XmlElement
	public String queueManager;
	@XmlElement
	public String channel;

	@XmlElement
	public boolean autoReconnect;
	@XmlElement
	public boolean autoConnect;

	@XmlElement
	public int retryAttemptCount;
	@XmlElement
	public long retryTimeout;

	public MQConnectionSettings()
	{
		readDelay = 1000;
		charset = 850;
		receiveQueue = "";
		sendQueue = "";
		useReceiveQueue = true;
		
		hostname = null;
		port = -1;
		queueManager = "";
		channel = null;
		
		autoReconnect = false;
		autoConnect = false;

		retryAttemptCount = 10;
		retryTimeout = 5000;
	}
	
	public MQConnectionSettings(MQConnectionSettings settings)
	{
		this.readDelay = settings.readDelay;
		this.charset = settings.charset;
		this.receiveQueue = settings.receiveQueue;
		this.sendQueue = settings.sendQueue;
		this.useReceiveQueue = settings.useReceiveQueue;
		
		this.hostname = settings.hostname;
		this.queueManager = settings.queueManager;
		this.port = settings.port;
		this.channel = settings.channel;

		this.autoReconnect = settings.autoReconnect;
		this.autoConnect = settings.autoConnect;

		this.retryAttemptCount = settings.retryAttemptCount;
		this.retryTimeout = settings.retryTimeout;
	}
	
	public abstract MQConnectionSettings copy();

	@Override
	public String toString()
	{
		LineBuilder sb = new LineBuilder();

		sb.append("Read delay="+readDelay);
		sb.append("Charset="+charset);
		sb.append("Receive queue="+receiveQueue);
		sb.append("Send queue="+sendQueue);
		sb.append("Use receive queue="+useReceiveQueue+Utils.EOL);
		
		sb.append("Host="+hostname);
		sb.append("Port="+port);
		sb.append("Queue manager="+queueManager);
		sb.append("Channel="+channel+Utils.EOL);

		sb.append("Auto-reconnect="+autoReconnect);
		sb.append("Auto-connect="+autoConnect);

		sb.append("Retry attempt count="+retryAttemptCount);
		sb.append("Retry timeout="+retryTimeout);

		return sb.toString();
	}
}
