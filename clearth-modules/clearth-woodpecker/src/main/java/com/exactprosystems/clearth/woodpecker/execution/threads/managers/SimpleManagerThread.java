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

package com.exactprosystems.clearth.woodpecker.execution.threads.managers;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingStrategy;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.misc.Callback;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;

import static java.util.concurrent.TimeUnit.SECONDS;

public final class SimpleManagerThread extends BaseManagerThread
{
	public SimpleManagerThread(String daemonName, DaemonSettings settings, WoodpeckerNotifications notifications,
	                           WorkQueue workQueue, LoadingStrategy loadingStrategy, Callback onStopped)
	{
		super(daemonName, settings, notifications, workQueue, loadingStrategy, onStopped);
	}
	
	@Override
	protected void execute() throws NoValidTasksAvailable
	{
		try
		{
			while (!isInterrupted())
			{
				long rate = daemonSettings.getRate();
				if (rate > 0)
					generateTasksForNextPeriod(rate, rateUnit, 1, SECONDS, false);
				else 
					pause(1, SECONDS);
			}
		}
		catch (InterruptedException e)
		{
			interrupt();
		}
	}
}
