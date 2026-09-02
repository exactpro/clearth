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

package com.exactprosystems.clearth.woodpecker.daemons.action;

import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;

import java.util.LinkedHashMap;

public class ActionTaskDescriptionsTable extends TaskDescriptionsTable<ActionTaskDescription>
{
	
	public ActionTaskDescriptionsTable(LinkedHashMap<String, ActionTaskDescription> descriptions)
	{
		super(descriptions);
	}
	
	
	public ActionTaskDescription nextTaskDescription() throws NoValidTasksAvailable
	{
		while (!table.isEmpty())
		{
			String taskDescId = distribution.nextElement();
			ActionTaskDescription taskDesc = table.get(taskDescId);
			if (taskDesc.isInvalidated())
				removeInvalidated(taskDescId);
			else 
				return taskDesc;
		}
		throw new NoValidTasksAvailable();
	}
	
	
	@Override
	public void dispose()
	{
		synchronized (table)
		{
			for (ActionTaskDescription description : table.values())
			{
				description.getAction().dispose();
			}
		}
	}
}
