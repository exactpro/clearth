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

package com.exactprosystems.clearth.woodpecker.execution.loading.generator;

import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTaskState;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public final class LoadingPriorityStrategy extends LoadingStrategy
{
	private static final long MIN_PERIOD_NS = MILLISECONDS.toNanos(100);
	
	private ScheduledTaskState prevTaskState;

	
	public LoadingPriorityStrategy(LoadingGenerator loadingGenerator)
	{
		super(loadingGenerator);
	}

	
	@Override
	protected void generateLoading(long tickNs, int count, long periodDurationNs, boolean isLastPeriod)
			throws NoValidTasksAvailable, InterruptedException
	{
		int restFromPrevPeriod = (prevTaskState != null) ? prevTaskState.getRestOperationsCount() : 0;
		int rest = count + restFromPrevPeriod;
		do
		{
			ScheduledTaskState state = loadingGenerator.generateTasks(tickNs, rest, periodDurationNs);
			rest = state.getRestOperationsCount();
			prevTaskState = state;
			
			if (rest == 0)
				break;
			
			if (!isLastPeriod && ((rest * tickNs) < MIN_PERIOD_NS))
				break;
		}
		while (!currentThread().isInterrupted());
	}
}
