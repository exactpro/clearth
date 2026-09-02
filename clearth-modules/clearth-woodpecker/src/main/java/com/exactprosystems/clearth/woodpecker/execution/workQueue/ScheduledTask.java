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

package com.exactprosystems.clearth.woodpecker.execution.workQueue;

import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescription;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.lang.Long.compare;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public abstract class ScheduledTask implements Delayed
{
	protected final TaskDescription taskDescription;
	protected final ScheduledTaskState taskState;
	
	protected final long executeAfterNs;
	protected final long executeBeforeNs;
	protected final boolean availableImmediately;


	public abstract void execute(WoodpeckerFunctions functions);

	
	public ScheduledTask(TaskDescription taskDescription, ScheduledTaskState taskState, 
	                     long executeAfterNs, long executeBeforeNs,
	                     boolean availableImmediately)
	{
		this.taskDescription = taskDescription;
		this.taskState = taskState;
		this.executeAfterNs = executeAfterNs;
		this.executeBeforeNs = executeBeforeNs;
		this.availableImmediately = availableImmediately;
	}
	
	
	public String getTaskDescId()
	{
		return taskDescription.getTaskId();
	}
	
	
	public boolean isOutdated()
	{
		return nanoTime() >= executeBeforeNs;
	}
	
	
	public boolean isExecutable()
	{
		return !taskState.isCompleted();
	}
	

	@Override
	public long getDelay(TimeUnit unit)
	{
		if (availableImmediately)
			return 0;
		
		long delayNs = executeAfterNs - nanoTime();
		if (unit == NANOSECONDS)
			return delayNs;
		else
			return unit.convert(delayNs, NANOSECONDS);
	}

	@Override
	public int compareTo(Delayed o)
	{
		if (availableImmediately)
			return compare(executeAfterNs, ((ScheduledTask)o).getExecuteAfterNs());
		return compare(getDelay(NANOSECONDS), o.getDelay(NANOSECONDS));
	}
	
	public long getExecuteAfterNs()
	{
		return executeAfterNs;
	}
	
	public void awaitExecution()
	{
		while (executeAfterNs > System.nanoTime())
			continue;
	}
}
