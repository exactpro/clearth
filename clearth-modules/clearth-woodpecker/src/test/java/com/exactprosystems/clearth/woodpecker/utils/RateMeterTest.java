/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

import com.exactprosystems.clearth.woodpecker.utils.RateMeter.Entry;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

public class RateMeterTest
{
	private final RateMeter rateMeter = new RateMeter(1000);
	
	@DataProvider
	public Object[][] dataToCheckRate()
	{
		return new Object[][]
				{
						{
								2050, deque(900, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000),
								10, TimeUnit.SECONDS
						},
						{
								2050,
								deque(new Entry(900, 5),
										new Entry(1000, 15),
										new Entry(1100, 5),
										new Entry(1200, 15),
										new Entry(1300, 5),
										new Entry(1400, 15),
										new Entry(1500, 5),
										new Entry(1600, 15),
										new Entry(1700, 5),
										new Entry(1800, 15),
										new Entry(1900, 5),
										new Entry(2000, 15)),
								100, TimeUnit.SECONDS
						},
						{
								25_000, deque(0, 10_000, 20_000),
								6, TimeUnit.MINUTES
						},
						{
								80_000, deque(0, 10_000, 20_000),
								1, TimeUnit.MINUTES
						}
				};
	}
	
	
	@Test(dataProvider = "dataToCheckRate")
	public void checkRate(long currentTs, Deque<Entry> deque, double expectedRate, TimeUnit rateUnit)
	{
		double actualRate = rateMeter.calculateRate(deque, currentTs, rateUnit, 1000);
		assertEquals(actualRate, expectedRate, 0.001);
	}
	
	
	private static Deque<Entry> deque(Entry... entries)
	{
		LinkedList<Entry> deque = new LinkedList<>();
		for (Entry entry : entries)
			deque.add(entry);
		return deque;
	}
	
	private static Deque<Entry> deque(long... timestamps)
	{
		LinkedList<Entry> deque = new LinkedList<>();
		for (long timestamp : timestamps)
		{
			deque.add(new Entry(timestamp));
		}
		return deque;
	}
}
