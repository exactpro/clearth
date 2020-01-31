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

package com.exactprosystems.clearth.automation.schedulerinfo;

import com.exactprosystems.clearth.automation.Step;

public class SchedulerStepData
{
	private Step step;
	private String stepName; // Name for displaying step on page (corrected for HTML)
	
	public SchedulerStepData(Step step, String stepName)
	{
		this.step = step;
		this.stepName = stepName;
	}
	
	public Step getStep()
	{
		return step;
	}
	
	public String getStepName()
	{
		return stepName;
	}
}
