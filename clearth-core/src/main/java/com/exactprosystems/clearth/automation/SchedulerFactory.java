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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunches;

public abstract class SchedulerFactory
{
	protected final ExecutorFactory executorFactory;
	protected final StepFactory stepFactory;
	
	public SchedulerFactory(ExecutorFactory executorFactory, StepFactory stepFactory)
	{
		this.executorFactory = executorFactory;
		this.stepFactory = stepFactory;
	}
	
	public abstract Scheduler createScheduler(String name, String configsRoot, String schedulerDirName) throws Exception;
	
	public abstract XmlSchedulerLaunchInfo createSchedulerLaunchInfo();
	
	public abstract XmlSchedulerLaunches createSchedulerLaunches();
	
	public ExecutorFactory getExecutorFactory()
	{
		return executorFactory;
	}
	
	public StepFactory getStepFactory()
	{
		return stepFactory;
	}
}
