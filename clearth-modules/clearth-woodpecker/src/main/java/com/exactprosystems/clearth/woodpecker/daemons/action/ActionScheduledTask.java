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

import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTask;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTaskState;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;

public class ActionScheduledTask extends ScheduledTask
{
	private final DaemonAction action;
	
	public ActionScheduledTask(ActionTaskDescription taskDescription, ScheduledTaskState taskState,
	                           long executeAfterNs, long executeBeforeNs, boolean availableImmediately)
	{
		super(taskDescription, taskState, executeAfterNs, executeBeforeNs, availableImmediately);
		this.action = taskDescription.getAction();
	}
	
	@Override
	public void execute(WoodpeckerFunctions functions)
	{
		if (!isExecutable())
			return;
		
		if (action.isInvalidated())
			return;

		taskState.onTaskInProgress(taskDescription.getTaskId(), 1);
		
		action.execute(functions);
	}
}
