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

import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescription;

public class ActionTaskDescription extends TaskDescription
{
	private final DaemonAction action;

	public ActionTaskDescription(String taskId, int probability, int threadsCount, DaemonAction action)
	{
		super(taskId, probability, threadsCount);
		this.action = action;
	}

	public DaemonAction getAction()
	{
		return action;
	}

	@Override
	public boolean isInvalidated()
	{
		return action.isInvalidated();
	}
}
