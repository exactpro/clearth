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

package com.exactprosystems.clearth.config;

import javax.xml.bind.annotation.XmlType;

import static com.exactprosystems.memorymonitor.MemoryMonitor.*;

@XmlType(name = "monitor")
public class MemoryMonitorCfg
{
	private long sleep = DEFAULT_SLEEP, largeDiff = DEFAULT_LARGEDIFF, lowMemory = DEFAULT_LOWMEMORY;
	public MemoryMonitorCfg(){}

	public void setSleep(long sleep)
	{
		this.sleep = sleep;
	}

	public long getSleep()
	{
		return sleep;
	}

	public void setLargeDiff(long largeDiff)
	{
		this.largeDiff = largeDiff;
	}

	public long getLargeDiff()
	{
		return largeDiff;
	}

	public void setLowMemory(long lowMemory)
	{
		this.lowMemory = lowMemory;
	}

	public long getLowMemory()
	{
		return lowMemory;
	}

	@Override
	public String toString()
	{
			return "sleep = " + this.getSleep() +
					"; largeDiff = " + this.getLargeDiff() +
					"; lowMemory = " + this.getLowMemory();
	}
}
