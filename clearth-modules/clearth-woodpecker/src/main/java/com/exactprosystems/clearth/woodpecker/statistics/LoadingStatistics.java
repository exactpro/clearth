/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.statistics;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;

public abstract class LoadingStatistics
{
	private static final Logger logger = LoggerFactory.getLogger(LoadingStatistics.class);
	
	private final LoadingStatisticsUpdater updater;
	
	private long startTimestamp;
	private volatile boolean completed;
	
	
	protected abstract void update(TimeUnit periodUnit);
	
	
	public LoadingStatistics()
	{
		updater = new LoadingStatisticsUpdater(this);
	}

	
	public final void start()
	{
		updater.start();
		startTimestamp = currentTimeMillis();
	}
	
	/**
	 * Stops work of statistics collector. Optionally waits for it to finish
	 * @param waitMillis maximum number of milliseconds to wait for statistics collector to finish its work. 0 means infinite wait
	 */
	public final void stop(long waitMillis)
	{
		updater.interrupt();
		if (waitMillis > -1)
			waitToStop(waitMillis);
	}
	

	public boolean isCompleted()
	{
		return completed;
	}
	
	public long getStartTimestamp()
	{
		return startTimestamp;
	}


	void setCompleted()
	{
		completed = true;
	}
	
	
	protected void waitToStop(long millis)
	{
		try
		{
			updater.join(millis);
		}
		catch (InterruptedException e)
		{
			logger.warn("Wait for updater to stop interrupted", e);
			Thread.currentThread().interrupt();
			return;
		}
	}
}
