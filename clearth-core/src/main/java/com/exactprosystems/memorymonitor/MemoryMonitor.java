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

package com.exactprosystems.memorymonitor;

import com.exactprosystems.clearth.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryMonitor extends Thread
{
	private static final Logger logger = LoggerFactory.getLogger(MemoryMonitor.class);
	public static final long DEFAULT_SLEEP = 10000,
			DEFAULT_LARGEDIFF = 50000000,
			DEFAULT_LOWMEMORY = 100000000;

	protected final Runtime rm = Runtime.getRuntime();
	protected final long sleep,
			largeDiff,
			lowMemory;

	//TODO after adding config:
	//init parameters from config
	public MemoryMonitor(String name)
	{
		this(name, DEFAULT_SLEEP, DEFAULT_LARGEDIFF, DEFAULT_LOWMEMORY);
	}

	public MemoryMonitor(String name, long sleep, long largeDiff, long lowMemory)
	{
		super(name);
		this.sleep = sleep;
		this.largeDiff = largeDiff;
		this.lowMemory = lowMemory;

		logger.info("Created memory monitor with settings: 'sleep'={}, 'largeDiff'={}, 'lowMemory'={}",
				sleep, largeDiff, lowMemory);
	}

	public void halt()
	{
		this.interrupt();
	}

	@Override
	public void run()
	{
		long max = rm.maxMemory(),
				maxMb = toMegabytes(max),
				total = rm.totalMemory(),
				totalMb = toMegabytes(total),
				free = rm.freeMemory(),
				freeMb = toMegabytes(free);
		logger.info("Maximum available memory: {} Mb ({})", maxMb, max);
		logger.info("Currently available memory: {} Mb ({})", totalMb, total);
		logger.info("Free memory: {} Mb ({}){}", freeMb, free, Utils.EOL);
		while (true)
		{
			try
			{
				Thread.sleep(sleep);
			}
			catch (InterruptedException e)
			{
				logger.info("Wait for next iteration interrupted, monitoring stopped");
				return;
			}

			long currentTotal = rm.totalMemory(),
					currentTotalMb = toMegabytes(currentTotal),
					currentFree = rm.freeMemory(),
					currentFreeMb = toMegabytes(currentFree);
			logger.info("Available: {} Mb ({})", currentTotalMb, currentTotal);
			logger.info("Free: {} Mb ({}){}", currentFreeMb, currentFree, Utils.EOL);

			if (logger.isWarnEnabled())
			{
				if (free-currentFree >= largeDiff)
					logger.warn("Large memory consumption detected!{}", Utils.EOL);
				if ((max-currentTotal <= lowMemory) && (currentTotal-currentFree <= lowMemory))
					logger.warn("MEMORY LOW! Already allocated {} Mb of maximum {} Mb, {} Mb left{}",
							currentTotalMb, maxMb, currentFreeMb, Utils.EOL);
			}

			total = currentTotal;
			totalMb = currentTotalMb;
			free = currentFree;
			freeMb = currentFreeMb;
		}
	}


	private long toMegabytes(long bytes)
	{
		return Math.round((double) bytes/1024/1024);
	}
}
