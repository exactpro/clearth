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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;

import java.util.*;

public class MessageTaskDescriptionsTable extends TaskDescriptionsTable<MessageTaskDescription>
{

	public MessageTaskDescriptionsTable(LinkedHashMap<String, MessageTaskDescription> descriptions)
	{
		super(descriptions);
	}
	
	
	public MessageTaskDescription nextTaskDescription(int suggestedBatchSize) throws NoValidTasksAvailable
	{
		while (!table.isEmpty())
		{
			String taskDescId = distribution.nextElement(0);
			MessageTaskDescription taskDesc = table.get(taskDescId);
			if (taskDesc.isInvalidated())
				removeInvalidated(taskDescId);
			else
			{
				int usagesCount = taskDesc.isBatchEnabled() ? suggestedBatchSize : 1;
				distribution.updateCurrentDistribution(taskDescId, usagesCount);
				return taskDesc;
			}
		}
		throw new NoValidTasksAvailable();
	}


	@Override
	public void dispose()
	{
		synchronized (table)
		{
			for (MessageTaskDescription description : table.values())
			{
				description.getProvider().dispose();
				description.getSender().dispose();
			}
		}
	}
}
