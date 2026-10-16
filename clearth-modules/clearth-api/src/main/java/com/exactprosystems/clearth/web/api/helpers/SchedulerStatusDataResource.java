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

package com.exactprosystems.clearth.web.api.helpers;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.Step;

@XmlRootElement(name = "schedulerStatus")
@XStreamAlias("schedulerStatus")
public class SchedulerStatusDataResource
{
	@XmlElement
	private boolean running;
	@XmlElement
	private boolean suspended;
	@XmlElement
	private boolean stoppedByUser;
	@XmlElement
	private boolean successful;
	@XmlElement
	private List<GlobalStepDataResource> globalSteps;
	
	public SchedulerStatusDataResource(Scheduler scheduler)
	{
		running = scheduler.isRunning();
		suspended = scheduler.isSuspended();
		stoppedByUser = scheduler.isStoppedByUser();
		successful = scheduler.isSuccessful();
		globalSteps = getSteps(scheduler);
	}

	public boolean isRunning()
	{
		return running;
	}

	public void setRunning(boolean running)
	{
		this.running = running;
	}

	public boolean isSuspended()
	{
		return suspended;
	}

	public void setSuspended(boolean suspended)
	{
		this.suspended = suspended;
	}

	public boolean isStoppedByUser()
	{
		return stoppedByUser;
	}

	public void setStoppedByUser(boolean stoppedByUser)
	{
		this.stoppedByUser = stoppedByUser;
	}

	public boolean isSuccessful()
	{
		return successful;
	}

	public void setSuccessful(boolean successful)
	{
		this.successful = successful;
	}
	
	
	protected List<GlobalStepDataResource> getSteps(Scheduler scheduler)
	{
		List<Step> steps = scheduler.getSteps();
		List<GlobalStepDataResource> result = new ArrayList<>(steps.size());
		for (Step step : steps)
			result.add(createStepData(step));
		return result;
	}
	
	protected GlobalStepDataResource createStepData(Step step)
	{
		return new GlobalStepDataResource(step);
	}
}