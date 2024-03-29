/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

public class DefaultSchedulerFactory extends SchedulerFactory
{
	public DefaultSchedulerFactory(ExecutorFactory executorFactory, StepFactory stepFactory, ActionGeneratorResources generatorResources)
	{
		super(executorFactory, stepFactory, generatorResources);
	}
	
	
	@Override
	public Scheduler createScheduler(String name, String configsRoot, String schedulerDirName) throws Exception
	{
		return new DefaultScheduler(name, configsRoot, schedulerDirName, executorFactory, stepFactory, generatorResources);
	}

	@Override
	public XmlSchedulerLaunchInfo createSchedulerLaunchInfo()
	{
		return new XmlSchedulerLaunchInfo();
	}

	@Override
	public XmlSchedulerLaunches createSchedulerLaunches()
	{
		return new XmlSchedulerLaunches();
	}
}
