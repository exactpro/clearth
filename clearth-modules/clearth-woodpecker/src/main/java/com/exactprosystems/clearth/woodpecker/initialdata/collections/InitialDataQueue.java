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

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import static java.lang.System.nanoTime;

/**
 * 24 July 2018
 */
public class InitialDataQueue implements InitialDataCollection
{
	private final DelayQueue<DelayedElement> values;
	private final int capacity;
	private final long delayInNs;
	
	public InitialDataQueue(int capacity, int delayInSeconds)
	{
		this.values = new DelayQueue<>();
		this.delayInNs = TimeUnit.SECONDS.toNanos(delayInSeconds);
		this.capacity = capacity;
	}

	@Override
	public boolean addValue(String value)
	{
		if (values.size() < capacity)   // Capacity is approximate and is used only to prevent large collections in memory
		{
			long takeAfterNs = nanoTime() + delayInNs;
			values.offer(new DelayedElement(value, takeAfterNs));
			return true;
		}
		else 
		 	return false;
	}

	@Override
	public String nextValue()
	{
		DelayedElement e = values.poll();
		return (e != null) ? e.getValue() : null;
	}
}
