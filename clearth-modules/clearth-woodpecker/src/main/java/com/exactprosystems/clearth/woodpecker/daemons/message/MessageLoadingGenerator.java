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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingGenerator;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTaskState;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.utils.TimeOperator;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.*;

public final class MessageLoadingGenerator extends LoadingGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(MessageLoadingGenerator.class);
	
	private final MessageDaemonSettings daemonSettings;
	private final WorkQueue workQueue;
	private final MessageTaskDescriptionsTable table;

	private final TimeOperator timeOperator;

	private long periodStartTime;

	
	public MessageLoadingGenerator(MessageDaemonSettings daemonSettings, WorkQueue workQueue, 
	                               MessageTaskDescriptionsTable table)
	{
		this(daemonSettings, workQueue, table, new TimeOperator());
	}

	MessageLoadingGenerator(MessageDaemonSettings daemonSettings, WorkQueue workQueue,
	                        MessageTaskDescriptionsTable table, TimeOperator timeOperator)
	{
		this.daemonSettings = daemonSettings;
		this.workQueue = workQueue;
		this.table = table;
		this.timeOperator = timeOperator;
		
		logger.info(daemonSettings.isPreciseDelay() ? "Generator with precise delay" : "Generator with regular delay");
	}
	
	
	@Override
	public int calculateCountInPeriod(long tick, int duration, TimeUnit durationUnit)
	{
		int minCount = daemonSettings.isUseStrictMinBatchSize() ? daemonSettings.getMinBatchSize() : 1;
		return max((int)(durationUnit.toNanos(duration) / tick), minCount);
	}


	@Override
	public ScheduledTaskState generateTasks(long tickNs, int count, long periodDurationNs) 
			throws NoValidTasksAvailable, InterruptedException
	{
		workQueue.clear();

		if (periodStartTime == 0)
			periodStartTime = timeOperator.nanoTime();

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
		int minBatchSize = daemonSettings.getMinBatchSize();
		int maxBatchSize = daemonSettings.getMaxBatchSize();
		int rest = count;
		long batchStartTime = periodStartTime;
		long lifetimeDelta = (periodEndTime - periodStartTime) / count;
		while (rest > 0)
		{
			int batchSize = randomBatchSize(rest, minBatchSize, maxBatchSize);
			MessageTaskDescription description = table.nextTaskDescription(batchSize);
			if (!description.isBatchEnabled())
				batchSize = 1;

			long executeBefore = min(batchStartTime + max(lifetimeDelta, LIFETIME_NS), periodEndTime);

			MessageScheduledTask task = new MessageScheduledTask(description, state, 
					batchStartTime, executeBefore, batchSize, daemonSettings.isPreciseDelay());
			workQueue.submit(task);

			state.addExpectedOpsCount(description.getTaskId(), batchSize);

			rest -= batchSize;
			batchStartTime += batchSize * tick;
		}
	}

	private void waitForPeriodEnd(long periodEndTime) throws InterruptedException
	{
		timeOperator.sleep(periodEndTime - timeOperator.nanoTime(), NANOSECONDS);
	}


	private int randomBatchSize(int rest, int minBatchSize, int maxBatchSize)
	{
		int batchSize = (minBatchSize < maxBatchSize) 
				? ThreadLocalRandom.current().nextInt(minBatchSize, maxBatchSize + 1)
				: maxBatchSize;
		return min(batchSize, rest);
	}
}
