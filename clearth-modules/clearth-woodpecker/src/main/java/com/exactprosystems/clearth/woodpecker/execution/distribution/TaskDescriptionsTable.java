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
package com.exactprosystems.clearth.woodpecker.execution.distribution;

import com.exactprosystems.clearth.woodpecker.misc.ProbabilityDistribution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TaskDescriptionsTable<D extends TaskDescription>
{
	protected final Map<String, D> table;

	protected ProbabilityDistribution<String> distribution;
	
	
	public abstract void dispose();
	
	
	public TaskDescriptionsTable(LinkedHashMap<String, D> descriptions)
	{
		table = descriptions;
		distribution = createDistribution(descriptions);
	}


	public Collection<D> getTaskDescriptions()
	{
		return table.values();
	}

	public Collection<String> getValidTaskDescIds()
	{
		return table.keySet();
	}

	public int size()
	{
		return table.size();
	}

	public boolean isEmpty()
	{
		return table.isEmpty();
	}
	
	
	protected void removeInvalidated(String taskDescId)
	{
		synchronized (table)
		{
			table.remove(taskDescId);
			distribution.remove(taskDescId);
		}
	}


	private ProbabilityDistribution<String> createDistribution(LinkedHashMap<String, D> descriptions)
	{
		double[] probabilities = new double[descriptions.size()];
		int i = 0;
		for (D desc : descriptions.values())
		{
			probabilities[i] = desc.getProbability();
			i++;
		}
		return new ProbabilityDistribution<>(new ArrayList<>(descriptions.keySet()), probabilities);
	}
}
