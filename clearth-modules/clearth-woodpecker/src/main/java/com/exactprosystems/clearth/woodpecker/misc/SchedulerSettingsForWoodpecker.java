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

package com.exactprosystems.clearth.woodpecker.misc;

import com.exactprosystems.clearth.automation.Scheduler;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SchedulerSettingsForWoodpecker
{
	private Scheduler selectedScheduler;

	public void setSelectedScheduler(Scheduler selectedScheduler)
	{
		this.selectedScheduler = selectedScheduler;
	}
	
	public String getSchedulerName()
	{
		if(selectedScheduler == null)
			return null;
		return selectedScheduler.getName();
	}
	
	public Map<String, Boolean> getHolidays()
	{
		if(selectedScheduler == null)
			return new HashMap<>();
		return selectedScheduler.getSchedulerData().getHolidays();
	}
	
	public boolean useBusinessDay()
	{
		return (selectedScheduler != null) && !selectedScheduler.getSchedulerData().isUseCurrentDate();
	}

	public Date getBusinessDay()
	{
		if(selectedScheduler == null)
			return new Date();
		return selectedScheduler.getBusinessDay();
	}

	public Date getBaseTime()
	{
		if(selectedScheduler == null)
			return null;
		return selectedScheduler.getBaseTime();
	}

	public boolean isWeekendHoliday()
	{
		if(selectedScheduler == null)
			return false;
		return selectedScheduler.isWeekendHoliday();
	}
}
