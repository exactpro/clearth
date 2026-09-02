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

package com.exactprosystems.clearth.woodpecker.execution.threads.managers;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingStrategy;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.misc.Callback;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public abstract class BaseManagerThread extends Thread
{
	private static final Logger log = LoggerFactory.getLogger(BaseManagerThread.class);

	protected final String daemonName;
	protected final DaemonSettings daemonSettings;
	protected final WoodpeckerNotifications notifications;
	protected final WorkQueue workQueue;
	protected final LoadingStrategy loadingStrategy;
	protected final Callback onStopped;
	protected final TimeUnit rateUnit;
	
	
	protected abstract void execute() throws NoValidTasksAvailable;
	
	public BaseManagerThread(String daemonName, DaemonSettings daemonSettings, WoodpeckerNotifications notifications, 
	                         WorkQueue workQueue, LoadingStrategy loadingStrategy, Callback onStopped)
	{
		super(format("%s-manager", daemonName));
		this.daemonName = daemonName;
		this.daemonSettings = daemonSettings;
		this.notifications = notifications;
		this.workQueue = workQueue;
		this.loadingStrategy = loadingStrategy;
		this.onStopped = onStopped;
		this.rateUnit = daemonSettings.getRateUnit();
	}

	public void terminate()
	{
		interrupt();
	}

	@Override
	public final void run()
	{
		try
		{
			execute();
		}
		catch (NoValidTasksAvailable e)
		{
			notifications.addWarning(daemonName, log, "Too many errors. Stopping daemon...");
		}
		finally
		{
			onStopped.execute();
		}
	}

	protected void generateTasksForNextPeriod(double rate, TimeUnit rateUnit, int periodDuration, TimeUnit durationUnit,
	                                          boolean isLastPeriod) 
			throws InterruptedException, NoValidTasksAvailable
	{
		loadingStrategy.generateLoading(rate, rateUnit, periodDuration, durationUnit, isLastPeriod);
	}
	
	protected void pause(long duration, TimeUnit durationUnit) throws InterruptedException
	{
		loadingStrategy.pause(duration, durationUnit);
	}
}
