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

package com.exactprosystems.clearth.woodpecker.execution.loading.generator;

import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTaskState;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class LoadingGenerator
{
	public static final long LIFETIME_NS = MILLISECONDS.toNanos(100);
	
	
	public abstract int calculateCountInPeriod(long tick, int duration, TimeUnit durationUnit);

	public abstract ScheduledTaskState generateTasks(long tickNs, int count, long periodDurationNs)
			throws NoValidTasksAvailable, InterruptedException;

	public abstract void pause(long duration, TimeUnit durationUnit) throws InterruptedException;
	

	public long calculateTick(double rate, TimeUnit rateUnit)
	{
		return (long)(rateUnit.toNanos(1) / rate);
	}
}
