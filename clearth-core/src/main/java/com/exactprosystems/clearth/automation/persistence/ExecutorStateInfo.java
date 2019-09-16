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

package com.exactprosystems.clearth.automation.persistence;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.automation.ReportsInfo;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("StateInfo")
public class ExecutorStateInfo
{
	private List<StepState> steps;
	private List<String> matrices;
	private boolean weekendHoliday;
	private Map<String, Boolean> holidays;
	private Date businessDay;
	private String startedByUser;
	private Date started, ended;
	private ReportsInfo reportsInfo;
	
	
	public List<StepState> getSteps()
	{
		return steps;
	}
	
	public void setSteps(List<StepState> steps)
	{
		this.steps = steps;
	}
	
	
	public List<String> getMatrices()
	{
		return matrices;
	}
	
	public void setMatrices(List<String> matrices)
	{
		this.matrices = matrices;
	}
	
	
	public boolean isWeekendHoliday()
	{
		return weekendHoliday;
	}
	
	public void setWeekendHoliday(boolean weekendHoliday)
	{
		this.weekendHoliday = weekendHoliday;
	}
	
	
	public Map<String, Boolean> getHolidays()
	{
		return holidays;
	}
	
	public void setHolidays(Map<String, Boolean> holidays)
	{
		this.holidays = holidays;
	}
	
	
	public Date getBusinessDay()
	{
		return businessDay;
	}
	
	public void setBusinessDay(Date businessDay)
	{
		this.businessDay = businessDay;
	}
	
	
	public String getStartedByUser()
	{
		return startedByUser;
	}
	
	public void setStartedByUser(String startedByUser)
	{
		this.startedByUser = startedByUser;
	}
	
	
	public Date getStarted()
	{
		return started;
	}
	
	public void setStarted(Date started)
	{
		this.started = started;
	}
	
	
	public Date getEnded()
	{
		return ended;
	}
	
	public void setEnded(Date ended)
	{
		this.ended = ended;
	}
	
	
	public ReportsInfo getReportsInfo()
	{
		return reportsInfo;
	}
	
	public void setReportsInfo(ReportsInfo reportsInfo)
	{
		this.reportsInfo = reportsInfo;
	}
}
