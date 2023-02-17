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

package com.exactprosystems.clearth.connectivity.jms;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSetting;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSettings;
import com.exactprosystems.clearth.connectivity.ibmmq.ClearThBasicMqConnectionSettings;
import com.exactprosystems.clearth.utils.LineBuilder;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@ConnectionSettings(order = {"hostname", "port", "receiveQueue", "useReceiveQueue", "sendQueue", "readDelay"},
		columns = {"hostname", "sendQueue", "receiveQueue"})
public class JmsConnectionSettings extends ClearThBasicMqConnectionSettings
{
	@XmlElement
	@ConnectionSetting(name = "Read delay")
	private long readDelay;
	
	public JmsConnectionSettings()
	{
		readDelay = 1000;
	}
	
	public long getReadDelay()
	{
		return readDelay;
	}

	public void setReadDelay(long readDelay)
	{
		this.readDelay = readDelay;
	}

	@Override
	public String toString()
	{
		LineBuilder lb = new LineBuilder();
		lb.append(super.toString());
		lb.add("Read delay=").append(readDelay);

		return lb.toString();
	}
	
	@Override
	public void copyFrom(ClearThConnectionSettings settings)
	{
		super.copyFrom(settings);
		this.readDelay = ((JmsConnectionSettings) settings).getReadDelay();
	}
}
