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

package com.exactprosystems.clearth.automation;

public class DefaultSchedulerData extends SchedulerData
{
	public static final String[] CONFIG_HEADER = new String[]{"Global step", "Step kind", "Start at", "Start at type",
			"Wait next day", "Parameter", "Ask for continue", "Ask if failed", "Execute", "Comment"};
	
	public DefaultSchedulerData(String name, String configsRoot, String schedulerDirName, String matricesDir,
	                            String lastExecutedDataDir, StepFactory stepFactory) throws Exception
	{
		super(name, configsRoot, schedulerDirName, matricesDir, lastExecutedDataDir, stepFactory);
	}
	
	@Override
	public String[] getConfigHeader()
	{
		return CONFIG_HEADER;
	}
}
