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

package com.exactprosystems.clearth.woodpecker.execution.workQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class WorkQueue
{
	private static final Logger log = LoggerFactory.getLogger(WorkQueue.class);

	private final Map<String, DelayQueue<ScheduledTask>> queuesByTaskDescIds = new HashMap<>();
	private final LongAdder notExecutableTasksCounter = new LongAdder();
	private final LongAdder outdatedTasksCounter = new LongAdder();
	
	
	public void registerTaskDescId(String taskDescId)
	{
		DelayQueue<ScheduledTask> wq = queuesByTaskDescIds.get(taskDescId);
		if (wq == null)
		{
			wq = new DelayQueue<>();
			queuesByTaskDescIds.put(taskDescId, wq);
		}
	}
	
	public void submit(ScheduledTask task)
	{
		DelayQueue<ScheduledTask> workQueue = findForTaskDescId(task.getTaskDescId());
		workQueue.put(task);
	}

	public ScheduledTask nextTask(String taskDescId, long timeout, TimeUnit timeUnit) throws InterruptedException
	{
		DelayQueue<ScheduledTask> workQueue = findForTaskDescId(taskDescId);
		ScheduledTask task = workQueue.poll(timeout, timeUnit);
		
		if (task == null)
			return null;
		
		boolean notExecutable = !task.isExecutable();
		boolean outdated = task.isOutdated();
		
		if (notExecutable)
			notExecutableTasksCounter.increment();
		if (outdated)
			outdatedTasksCounter.increment();
		
		return (notExecutable || outdated) ? null : task;
	}
	
	public void clear()
	{
		for (DelayQueue<ScheduledTask> wq : queuesByTaskDescIds.values())
		{
			wq.clear();
		}
		
		logStatistics();
	}
	
	private void logStatistics()
	{
		if (!log.isDebugEnabled())
			return;
		
		long notExecutableCount = notExecutableTasksCounter.sumThenReset();
		long outDatedCount = outdatedTasksCounter.sumThenReset();
		
		if ((notExecutableCount == 0) && (outDatedCount == 0))
			return;
		
		log.debug("Not executable tasks count - {}; outdated tasks count - {}.", notExecutableCount, outDatedCount);
	}
	

	private DelayQueue<ScheduledTask> findForTaskDescId(String taskDescId)
	{
		DelayQueue<ScheduledTask> wq = queuesByTaskDescIds.get(taskDescId);
		if (wq != null)
			return wq;
		else
			throw new IllegalStateException(format("Work queue for task description '%s' wasn't initialized.", taskDescId));
	}
}
