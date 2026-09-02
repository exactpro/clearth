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

import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;
import com.exactprosystems.clearth.woodpecker.misc.Callback;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTask;

import java.util.concurrent.TimeUnit;

public class WorkerThread extends Thread
{
	private final WorkQueue workQueue;
	private final String taskDescId;
	private final WoodpeckerFunctions functions;
	private final Callback onStopped;
	
	private volatile boolean terminated;

	
	public WorkerThread(String name, WorkQueue workQueue, String taskDescId, 
	                    WoodpeckerFunctions functions, Callback onStopped)
	{
		super(name);
		this.workQueue = workQueue;
		this.taskDescId = taskDescId;
		this.onStopped = onStopped;
		this.functions = functions;
	}
	
	/**
	 * Asks the worker to complete current iteration and stop.
	 * The flag terminated is used here instead of standard Thread interrupted status because of the following reasons:
	 *  * Interrupted status can be reset inside internal calls, f.e. in IBM MQ library;
	 *  * Sending of message can be interrupted and throw InterruptedIOException.
	 */
	public void terminate()
	{
		this.terminated = true;
	}
	
	private boolean isTerminated()
	{
		return terminated || isInterrupted();
	}

	
	@Override
	public void run()
	{
		try
		{
			while (!isTerminated())
			{
				try
				{
					ScheduledTask task = workQueue.nextTask(taskDescId,1, TimeUnit.SECONDS);
					if (task != null)
					{
						task.awaitExecution();
						task.execute(functions);
					}
				}
				catch (InterruptedException e)
				{
					interrupt();
					break;
				}
			}
		}
		finally
		{
			onStopped.execute();
		}
	}
}
