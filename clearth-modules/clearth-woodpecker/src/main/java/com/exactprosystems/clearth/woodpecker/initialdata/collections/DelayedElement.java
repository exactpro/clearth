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

package com.exactprosystems.clearth.woodpecker.initialdata.collections;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.lang.Long.compare;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 03 October 2018
 */
class DelayedElement implements Delayed
{
	private final String value;
	private final long takeAfterNs;

	public DelayedElement(String value, long takeAfterNs)
	{
		this.value = value;
		this.takeAfterNs = takeAfterNs;
	}

	public String getValue()
	{
		return value;
	}

	@Override
	public long getDelay(TimeUnit unit)
	{
		long delayNs = takeAfterNs - nanoTime();
		if (unit == NANOSECONDS)
			return delayNs;
		else
			return unit.convert(delayNs, NANOSECONDS);
	}

	@Override
	public int compareTo(Delayed o)
	{
		return compare(getDelay(NANOSECONDS), o.getDelay(NANOSECONDS));
	}
}
