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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 27 July 2018
 */
public class InitialDataSet implements InitialDataCollection
{
	private final List<String> values;
	private final int capacity;

	public InitialDataSet(int capacity)
	{
		this.values = new LinkedList<>();
		this.capacity = capacity;
	}

	@Override
	public boolean addValue(String value)
	{
		if (values.size() < capacity)
		{
			synchronized (values)
			{
				if (values.size() < capacity)
				{
					values.add(value);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String nextValue()
	{
		synchronized (values)
		{
			int size = values.size();
			if (size > 0)
			{
				int index = (size == 1) ? 0 : ThreadLocalRandom.current().nextInt(size);
				return values.remove(index);
			}
		}
		return null;
	}
}
