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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

import static java.lang.Math.max;

public class ScheduledTaskState
{
	private final LongAdder restOpsCounter = new LongAdder();
	private final Map<String, LongAdder> restOpsCountByTaskDescIds;
	
	
	public ScheduledTaskState(Collection<String> taskDescIds)
	{
		restOpsCountByTaskDescIds = initRestCountByTaskDescIds(taskDescIds);
	}
	
	
	public void onTaskInProgress(String taskDescId, int opsCount)
	{
		updateCounters(taskDescId, -opsCount);
	}
	
	
	public int getRestOperationsCount()
	{
		return max(restOpsCounter.intValue(), 0);
	}
	
	public boolean isCompleted()
	{
		return restOpsCounter.sum() <= 0;
	}
	

	public Map<String, Integer> getRestOpsCountByTaskDescIds()
	{
		Map<String, Integer> result = new HashMap<>();
		for (Map.Entry<String, LongAdder> e : restOpsCountByTaskDescIds.entrySet())
		{
			String taskDescId = e.getKey();
			LongAdder counter = e.getValue();
			int count = counter.intValue();
			if (count > 0)
				result.put(taskDescId, count);
		}
		return result;
	}
	
	public void addExpectedOpsCount(String taskDescId, int count)
	{
		updateCounters(taskDescId, count);
	}
	
	
	private Map<String, LongAdder> initRestCountByTaskDescIds(Collection<String> taskDescIds)
	{
		Map<String, LongAdder> map = new HashMap<>();
		for (String taskDescId : taskDescIds)
		{
			map.put(taskDescId, new LongAdder());
		}
		return map;
	}
	
	
	private void updateCounters(String taskDescId, int count)
	{
		if (count != 0)
		{
			restOpsCounter.add(count);
			LongAdder counter = restOpsCountByTaskDescIds.get(taskDescId);
			counter.add(count);
		}
	}
}
