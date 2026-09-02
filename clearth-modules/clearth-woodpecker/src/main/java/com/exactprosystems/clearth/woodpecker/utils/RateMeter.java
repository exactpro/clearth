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

package com.exactprosystems.clearth.woodpecker.utils;

import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class RateMeter
{
	private final Deque<Entry> deque = new ConcurrentLinkedDeque<>();
	private final AtomicInteger dequeSize = new AtomicInteger();
	
	private final int capacity;
	
	
	public RateMeter(int capacity)
	{
		this.capacity = capacity;
	}

	
	public void update(int eventsCount)
	{
		deque.add(new Entry(currentTimeMillis(), eventsCount));
		
		if (dequeSize.incrementAndGet() > capacity)
			deque.pollFirst();
	}
	
	
	public double calculateRateInSecond()
	{
		return calculateRate(SECONDS, 1, SECONDS);
	}
	
	public double calculateRateInMinute()
	{
		return calculateRate(MINUTES, 5, SECONDS);
	}
	
	public double calculateRate(TimeUnit rateUnit, long timeWindow, TimeUnit timeWindowUnit)
	{
		return calculateRate(deque, currentTimeMillis(), rateUnit, timeWindowUnit.toMillis(timeWindow));
	}
	
	double calculateRate(Deque<Entry> deque, long currentTs, TimeUnit rateUnit, long timeWindowMs)
	{
		Entry firstEntry = null;
		Entry lastEntry = null;
		int eventsCount = 0;

		Iterator<Entry> descIterator = deque.descendingIterator();
		while (descIterator.hasNext())
		{
			Entry e = descIterator.next();

			if (lastEntry == null)
			{
				lastEntry = e;
				eventsCount += e.count;
			}
			else
			{
				if (firstEntry != null)
					eventsCount += firstEntry.count;
				firstEntry = e;
			}

			if (!isInTimeWindow(e, currentTs, timeWindowMs) && (firstEntry != null))
				break;
		}
		return calculateRate(currentTs, firstEntry, lastEntry, eventsCount, rateUnit);
	}
	
	private double calculateRate(long currentTs, Entry firstEntry, Entry lastEntry, int eventsCount, TimeUnit rateUnit)
	{
		if (lastEntry == null)
			return 0;
		
		long openPeriodMs = currentTs - lastEntry.timestamp;
		if (firstEntry == null)
			return calculateRate(openPeriodMs, lastEntry.count, rateUnit);
		
		long lastPeriodMs = lastEntry.timestamp - firstEntry.timestamp;
		if (openPeriodMs >= lastPeriodMs)
			return calculateRate(openPeriodMs, lastEntry.count, rateUnit);
		else 
			return calculateRate(lastPeriodMs, eventsCount, rateUnit);
	}
	
	private double calculateRate(long periodMs, int eventsCount, TimeUnit rateUnit)
	{
		if (periodMs <= 0)
			return eventsCount;
		else 
			return ((double) (eventsCount * rateUnit.toMillis(1))) / periodMs;
	}

	private boolean isInTimeWindow(Entry e, long currentTs, long timeWindowMs)
	{
		return e.timestamp > (currentTs - timeWindowMs);
	}
	
	
	static class Entry
	{
		private final long timestamp;
		private final int count;
		
		Entry(long timestamp)
		{
			this(timestamp, 1);
		}

		Entry(long timestamp, int count)
		{
			this.timestamp = timestamp;
			this.count = count;
		}
	}
}
