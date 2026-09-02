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

public abstract class TaskDescription
{
	private final String taskId;
	private final int probability;	
	private final int threadsCount;

	public abstract boolean isInvalidated();	

	public TaskDescription(String taskId, int probability, int threadsCount)
	{
		this.taskId = taskId;
		this.probability = probability;
		this.threadsCount = threadsCount;
	}

	public String getTaskId()
	{
		return taskId;
	}

	public int getProbability()
	{
		return probability;
	}

	public int getThreadsCount()
	{
		return threadsCount;
	}
}
