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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;

import javax.xml.bind.annotation.*;

import static java.lang.Math.max;

@XmlRootElement(name="MessageDaemonSettings")
@XmlAccessorType(XmlAccessType.NONE)
public class MessageDaemonSettings extends DaemonSettings
{
	@XmlElement
	private int minBatchSize = 1;
	
	@XmlElement
	private int maxBatchSize = 1;
	
	@XmlElement 
	private boolean useStrictMinBatchSize;
	
	@XmlElement
	private boolean preciseDelay = false;

	
	public MessageDaemonSettings(String daemonType, String daemonName)
	{
		super(daemonType, daemonName);
	}
	
	public MessageDaemonSettings() {}
	

	public int getMinBatchSize()
	{
		return max(minBatchSize, 1);
	}

	public void setMinBatchSize(int minBatchSize)
	{
		this.minBatchSize = minBatchSize;
	}

	public int getMaxBatchSize()
	{
		return max(maxBatchSize, 1);
	}

	public void setMaxBatchSize(int maxBatchSize)
	{
		this.maxBatchSize = maxBatchSize;
	}

	public boolean isUseStrictMinBatchSize()
	{
		return useStrictMinBatchSize;
	}

	public void setUseStrictMinBatchSize(boolean useStrictMinBatchSize)
	{
		this.useStrictMinBatchSize = useStrictMinBatchSize;
	}
	
	
	public boolean isPreciseDelay()
	{
		return preciseDelay;
	}
	
	public void setPreciseDelay(boolean preciseDelay)
	{
		this.preciseDelay = preciseDelay;
	}
}
