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

package com.exactprosystems.clearth.woodpecker.execution.threads.workers;

import com.exactprosystems.clearth.woodpecker.WoodpeckerObjectsFactory;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescription;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;
import com.exactprosystems.clearth.woodpecker.initialdata.InitialData;
import com.exactprosystems.clearth.woodpecker.misc.Callback;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;

import java.util.List;

import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;
import static java.lang.String.format;

public class WorkerThreadsFactory
{
	public void createWorkerThreads(TaskDescriptionsTable<? extends TaskDescription> table, InitialData initialData,
	                                Callback onStopped, List<WorkerThread> workerThreads, WorkQueue workQueue)
	{
		WoodpeckerObjectsFactory factory = woodpecker().getObjectsFactory();
		
		for (TaskDescription desc : table.getTaskDescriptions())
		{
			String descId = desc.getTaskId();
			int workersCount = desc.getThreadsCount();

			workQueue.registerTaskDescId(descId);

			for (int i = 1; i <= workersCount; i++)
			{
				String threadId = format("w-worker-%s-%d", desc.getTaskId(), i);
				
				WoodpeckerFunctions functions = factory.createFunctions(woodpecker().getSchedulerSettings(), initialData);
				
				WorkerThread wt = new WorkerThread(threadId, workQueue, descId, functions, onStopped);
				workerThreads.add(wt);
			}
		}
	}
}
