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
import com.exactprosystems.clearth.woodpecker.configuration.settings.ExecutionMode;
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.PriorityMode;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemonFactory;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingGenerator;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingPriorityStrategy;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingStrategy;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.TimePriorityStrategy;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.misc.Callback;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;

import static com.exactprosystems.clearth.woodpecker.configuration.settings.loading.PriorityMode.Time;
import static java.lang.String.format;

public class ManagerThreadFactory
{
	
	public BaseManagerThread create(WoodpeckerDaemonFactory daemonFactory, DaemonSettings settings,
	                                WoodpeckerNotifications notifications, WorkQueue workQueue,
	                                TaskDescriptionsTable table, Callback onStopped)
	{
		LoadingGenerator loadingGenerator = daemonFactory.createLoadingGenerator(settings, workQueue, table);
		return createManagerThread(settings, notifications, workQueue, loadingGenerator, onStopped);
	}
	

	private BaseManagerThread createManagerThread(DaemonSettings settings, WoodpeckerNotifications notifications,
	                                              WorkQueue workQueue, LoadingGenerator loadingGenerator, 
	                                              Callback onStopped)
	{
		ExecutionMode mode = settings.getExecutionMode();
		LoadingStrategy loadingStrategy;
		switch (mode)
		{
			case Simple:
				loadingStrategy = selectLoadingStrategy(Time, loadingGenerator);
				return new SimpleManagerThread(settings.getFullName(),
						settings, notifications, workQueue, loadingStrategy, onStopped);
			case Scheduled:
				loadingStrategy = selectLoadingStrategy(settings.getPriorityMode(), loadingGenerator);
				return new ScheduledManagerThread(settings.getFullName(),
						settings, notifications, workQueue, loadingStrategy, onStopped);
			default:
				throw new IllegalArgumentException(format("Execution mode '%s' is unknown.", mode));
		}
	}
	
	private LoadingStrategy selectLoadingStrategy(PriorityMode priorityMode, LoadingGenerator loadingGenerator)
	{
		switch (priorityMode)
		{
			case Time:      return new TimePriorityStrategy(loadingGenerator);
			case Loading:   return new LoadingPriorityStrategy(loadingGenerator);
			default:        throw new IllegalArgumentException(format("Unsupported priority mode '%s'.", priorityMode));
		}
	}
}
