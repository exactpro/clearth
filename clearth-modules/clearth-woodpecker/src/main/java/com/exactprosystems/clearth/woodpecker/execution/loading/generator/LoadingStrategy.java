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

import java.util.concurrent.TimeUnit;

import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerMathUtils.isInteger;

public abstract class LoadingStrategy
{
	protected final LoadingGenerator loadingGenerator;
	
	protected abstract void generateLoading(long tickNs, int count, long periodDurationNs, boolean isLastPeriod)
			throws NoValidTasksAvailable, InterruptedException;
	
	protected LoadingStrategy(LoadingGenerator loadingGenerator)
	{
		this.loadingGenerator = loadingGenerator;
	}

	public final void generateLoading(double rateInOps, TimeUnit rateUnit, int periodDuration, TimeUnit durationUnit,
	                                  boolean isLastPeriod) throws NoValidTasksAvailable, InterruptedException
	{
		long tick = loadingGenerator.calculateTick(rateInOps, rateUnit);
		
		int count = loadingGenerator.calculateCountInPeriod(tick, periodDuration, durationUnit);

		long periodDurationNs = isInteger(rateInOps) ? -1 : durationUnit.toNanos(periodDuration);
		
		generateLoading(tick, count, periodDurationNs, isLastPeriod);
	}
	
	public final void pause(long duration, TimeUnit durationUnit) throws InterruptedException
	{
		loadingGenerator.pause(duration, durationUnit);
	}
}
