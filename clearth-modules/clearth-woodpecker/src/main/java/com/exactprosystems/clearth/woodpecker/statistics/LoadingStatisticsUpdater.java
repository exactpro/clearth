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

package com.exactprosystems.clearth.woodpecker.statistics;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.*;

class LoadingStatisticsUpdater extends Thread
{
	private static final long MS_IN_MIN = MINUTES.toMillis(1);
	private static final long MS_IN_SEC = SECONDS.toMillis(1);
	
	private final LoadingStatistics statistics;
	
	LoadingStatisticsUpdater(LoadingStatistics statistics)
	{
		super("woodpecker-loading-statistics");
		this.statistics = statistics;
	}

	@Override
	public void run()
	{
		try
		{
			long now = currentTimeMillis();
			long nextSecUpdateTs = now + MS_IN_SEC;
			long nextMinUpdateTs = now + MS_IN_MIN;
			long toWaitMs = MS_IN_SEC;
			do
			{
				try
				{
					MILLISECONDS.sleep(toWaitMs);
				}
				catch (InterruptedException e)
				{
					currentThread().interrupt();
				}

				if (currentTimeMillis() >= nextMinUpdateTs)
				{
					statistics.update(MINUTES);
					nextMinUpdateTs += MS_IN_MIN;
				}
				else 
					statistics.update(SECONDS);

				nextSecUpdateTs += MS_IN_SEC;
				toWaitMs = nextSecUpdateTs - currentTimeMillis();
			}
			while (!currentThread().isInterrupted());
		}
		finally
		{
			statistics.setCompleted();
		}
	}
}
