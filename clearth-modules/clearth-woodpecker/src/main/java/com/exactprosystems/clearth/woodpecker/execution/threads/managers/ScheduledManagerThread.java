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
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingSchedule;
import com.exactprosystems.clearth.woodpecker.execution.distribution.NoValidTasksAvailable;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingStrategy;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.misc.Callback;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;

import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerMathUtils.isInteger;

public final class ScheduledManagerThread extends BaseManagerThread
{
	private final LoadingSchedule loadingSchedule;
	
	public ScheduledManagerThread(String daemonName, DaemonSettings daemonSettings,
	                              WoodpeckerNotifications notifications, WorkQueue workQueue,
	                              LoadingStrategy loadingStrategy, Callback onStopped)
	{
		super(daemonName, daemonSettings,notifications, workQueue, loadingStrategy, onStopped);
		this.loadingSchedule = daemonSettings.getSchedule();
	}

	@Override
	protected void execute() throws NoValidTasksAvailable
	{
		try
		{
			int periodsCount = loadingSchedule.getPeriodsCount();
			for (int i = 0; i < periodsCount; i++)
			{
				if (isInterrupted())
					break;
				
				LoadingSchedule.Period period = loadingSchedule.getPeriod(i);
				boolean isLastPeriod = (i + 1) == periodsCount;
						
				generateLoadingForPeriod(period, isLastPeriod);
			}
		}
		catch (InterruptedException e)
		{
			interrupt();
		}
	}
	
	private void generateLoadingForPeriod(LoadingSchedule.Period period, boolean isLastPeriod) 
			throws InterruptedException, NoValidTasksAvailable
	{
		int duration = period.getDuration();
		double rate = period.getRate();
		
		if (rate != 0)
		{
			if (isInteger(rate))
			{
				for (int i = 0; i < duration; i++)
				{
					if (isInterrupted())
						return;
					
					boolean isLastStep = (i + 1) == duration;
					generateTasksForNextPeriod(rate, rateUnit, 1, rateUnit, isLastPeriod && isLastStep);
				}
			}
			else
				generateTasksForNextPeriod(rate, rateUnit, duration, rateUnit, isLastPeriod);
		}
		else
			pause(duration, rateUnit);
	}
}
