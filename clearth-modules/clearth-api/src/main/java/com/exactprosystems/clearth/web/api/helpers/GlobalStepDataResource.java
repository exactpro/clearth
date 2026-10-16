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

import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.exactprosystems.clearth.automation.ActionsExecutionProgress;
import com.exactprosystems.clearth.automation.Step;

@XmlRootElement(name = "globalStep")
@XStreamAlias("globalStep")
public class GlobalStepDataResource
{
	protected static final String STATUS_NOT_STARTED = "not started",
			STATUS_RUNNING = "running",
			STATUS_PAUSED = "paused",
			STATUS_FINISHED = "finished";
	
	@XmlAttribute
	private String name,
			kind,
			comment;
	@XmlAttribute
	private boolean execute,
			askForContinue,
			askIfFailed;
	
	@XmlAttribute
	private String startAt;
	@XmlAttribute
	private Date started,
			finished;
	@XmlAttribute
	private int actionsDone,
			actionsSuccessful;
	@XmlAttribute
	private String status;
	
	public GlobalStepDataResource(Step step)
	{
		init(step);
	}
	
	protected void init(Step step)
	{
		name = step.getName();
		kind = step.getKind();
		comment = step.getComment();
		execute = step.isExecute();
		askForContinue = step.isAskForContinue();
		askIfFailed = step.isAskIfFailed();
		startAt = step.getStartAt();
		started = step.getStarted();
		finished = step.getFinished();
		
		ActionsExecutionProgress progress = step.getExecutionProgress();
		actionsDone = progress.getDone();
		actionsSuccessful = progress.getSuccessful();
		
		status = getStatus(step);
	}
	
	protected String getStatus(Step step)
	{
		if (step.getStarted() == null)
			return STATUS_NOT_STARTED;
		if (step.getFinished() != null)
			return STATUS_FINISHED;
		if (step.isPaused())
			return STATUS_PAUSED;
		return STATUS_RUNNING;
	}
	
	
	public String getName()
	{
		return name;
	}
	
	public String getKind()
	{
		return kind;
	}
	
	public String getComment()
	{
		return comment;
	}
	
	public boolean isExecute()
	{
		return execute;
	}
	
	public boolean isAskForContinue()
	{
		return askForContinue;
	}
	
	public boolean isAskIfFailed()
	{
		return askIfFailed;
	}
	
	public String getStartAt()
	{
		return startAt;
	}
	
	public Date getStarted()
	{
		return started;
	}
	
	public Date getFinished()
	{
		return finished;
	}
	
	public int getActionsDone()
	{
		return actionsDone;
	}
	
	public int getActionsSuccessful()
	{
		return actionsSuccessful;
	}
	
	public String getStatus()
	{
		return status;
	}
}