/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.daemons.action;

import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingGenerator;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTaskState;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.utils.TimeOperator;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class ActionLoadingGenerator extends LoadingGenerator
{
	private final WorkQueue workQueue;
	private final ActionTaskDescriptionsTable table;

	private final TimeOperator timeOperator;
	
	private long periodStartTime;


	public ActionLoadingGenerator(WorkQueue workQueue, ActionTaskDescriptionsTable table)
	{
		this(workQueue, table, new TimeOperator());
	}
	
	ActionLoadingGenerator(WorkQueue workQueue, ActionTaskDescriptionsTable table, TimeOperator timeOperator)
	{
		this.workQueue = workQueue;
		this.table = table;
		this.timeOperator = timeOperator;
	}

	
	@Override
	public int calculateCountInPeriod(long tick, int duration, TimeUnit durationUnit)
	{
		return max((int)(durationUnit.toNanos(duration) / tick), 1);
	}

	@Override
	public ScheduledTaskState generateTasks(long tickNs, int count, long periodDurationNs) 
			throws NoValidTasksAvailable, InterruptedException
	{
		workQueue.clear();

		if (periodStartTime == 0)
			periodStartTime = nanoTime();

		long periodEndTime = (periodDurationNs > 0)
				? periodStartTime + periodDurationNs
				: periodStartTime + tickNs * count;

		ScheduledTaskState state = new ScheduledTaskState(table.getValidTaskDescIds());
		generateAndSubmitTasks(state, tickNs, count, periodStartTime, periodEndTime);
		
		waitForPeriodEnd(periodEndTime);
		periodStartTime = periodEndTime;
		
		return state;
	}

	@Override
	public void pause(long duration, TimeUnit durationUnit) throws InterruptedException
	{
		if (periodStartTime == 0)
			periodStartTime = timeOperator.nanoTime();

		long periodEndTime = periodStartTime + durationUnit.toNanos(duration);
		waitForPeriodEnd(periodEndTime);

		periodStartTime = periodEndTime;
	}


	private void generateAndSubmitTasks(ScheduledTaskState state, long tick, int count,
	                                    long periodStartTime, long periodEndTime) throws NoValidTasksAvailable
	{
		long lifetimeDelta = (periodEndTime - periodStartTime) / count;
		for (int i = 0; i < count; i++)
		{
			ActionTaskDescription description = table.nextTaskDescription();

			long executeAfter = periodStartTime + (tick * i);
			long executeBefore = min(executeAfter + max(lifetimeDelta, LIFETIME_NS), periodEndTime);
			
			ActionScheduledTask task = new ActionScheduledTask(description, state, executeAfter, executeBefore, true);
			workQueue.submit(task);
			
			state.addExpectedOpsCount(description.getTaskId(), 1);
		}
	}

	private void waitForPeriodEnd(long periodEndTime) throws InterruptedException
	{
		timeOperator.sleep(periodEndTime - timeOperator.nanoTime(), NANOSECONDS);
	}
}
