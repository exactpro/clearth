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

import com.exactprosystems.clearth.automation.*;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class StepState
{
	private String name = null, kind = null, startAt = null, parameter = null;
	private boolean askForContinue = false, askIfFailed = false, execute = true;
	private String comment;
	private Date started = null, finished = null;
	private ActionsExecutionProgress executionProgress;
	private boolean successful = true;  //interrupted flag can't be set without interrupting a step, no need to store its value
	private Map<String, StepContext> stepContexts = null;
	private String statusComment = null;
	private Throwable error = null;
	private StartAtType startAtType = StartAtType.DEFAULT;
	private boolean waitNextDay = false;
	
	public StepState()
	{
	}
	
	public StepState(Step step)
	{
		this.name = step.getName();
		this.kind = step.getKind();
		this.startAt = step.getStartAt();
		this.parameter = step.getParameter();

		this.askForContinue = step.isAskForContinue();
		this.askIfFailed = step.isAskIfFailed();
		this.execute = step.isExecute();
		
		this.comment = step.getComment();
		
		this.started = step.getStarted();
		this.finished = step.getFinished();
		
		this.executionProgress = step.getExecutionProgress();
		
		this.successful = step.isSuccessful();
		if (step.getStepContexts() != null)
		{
			this.stepContexts = new LinkedHashMap<String, StepContext>();
			for (Matrix m : step.getStepContexts().keySet())
				this.stepContexts.put(m.getName(), step.getStepContexts().get(m));
		}
		this.statusComment = step.getStatusComment();
		this.error = step.getError();

		this.startAtType = StartAtType.DEFAULT;
		this.waitNextDay = step.isWaitNextDay();
	}
	
	public StepState(StepState stepState)
	{
		this.name = stepState.getName();
		this.kind = stepState.getKind();
		this.startAt = stepState.getStartAt();
		this.parameter = stepState.getParameter();
		
		this.askForContinue = stepState.isAskForContinue();
		this.askIfFailed = stepState.isAskIfFailed();
		this.execute = stepState.isExecute();
		
		this.comment = stepState.getComment();
		
		this.started = stepState.getStarted();
		this.finished = stepState.getFinished();
		
		this.executionProgress = stepState.getExecutionProgress();
		
		this.successful = stepState.isSuccessful();
		this.stepContexts = stepState.getStepContexts();
		this.statusComment = stepState.getStatusComment();
		this.error = stepState.getError();
		this.startAtType = stepState.getStartAtType();
		this.waitNextDay = stepState.isWaitNextDay();
	}
	
	public Step stepFromState(StepFactory stepFactory)
	{
		Step result = stepFactory.createStep(name, kind, startAt, startAtType, waitNextDay, parameter,
				askForContinue, askIfFailed, execute, comment);
		
		result.setStarted(started);
		result.setFinished(finished);
		
		result.setExecutionProgress(executionProgress);
		
		result.setSuccessful(successful);
		result.setStatusComment(statusComment);
		result.setError(error);
		
		initStep(result);
		
		return result;
	}
	
	
	protected abstract void initStep(Step step);
	
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public String getKind()
	{
		return kind;
	}
	
	public void setKind(String kind)
	{
		this.kind = kind;
	}
	
	
	public String getStartAt()
	{
		return startAt;
	}
	
	public void setStartAt(String startAt)
	{
		this.startAt = startAt;
	}
	
	
	public String getParameter()
	{
		return parameter;
	}
	
	public void setParameter(String parameter)
	{
		this.parameter = parameter;
	}


	public boolean isAskForContinue()
	{
		return askForContinue;
	}

	public void setAskForContinue(boolean askForContinue)
	{
		this.askForContinue = askForContinue;
	}


	public boolean isAskIfFailed()
	{
		return askIfFailed;
	}

	public void setAskIfFailed(boolean askIfFailed)
	{
		this.askIfFailed = askIfFailed;
	}


	public boolean isExecute()
	{
		return execute;
	}
	
	public void setExecute(boolean execute)
	{
		this.execute = execute;
	}

	
	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	
	public Date getStarted()
	{
		return started;
	}
	
	public void setStarted(Date started)
	{
		this.started = started;
	}
	
	
	public Date getFinished()
	{
		return finished;
	}
	
	public void setFinished(Date finished)
	{
		this.finished = finished;
	}
	
	
	public ActionsExecutionProgress getExecutionProgress()
	{
		return executionProgress;
	}
	
	public void setExecutionProgress(ActionsExecutionProgress executionProgress)
	{
		this.executionProgress = executionProgress;
	}
	
	
	public boolean isSuccessful()
	{
		return successful;
	}
	
	public void setSuccessful(boolean successful)
	{
		this.successful = successful;
	}
	
	
	public Map<String, StepContext> getStepContexts()
	{
		return stepContexts;
	}

	public void setStepContexts(Map<String, StepContext> stepContexts)
	{
		this.stepContexts = stepContexts;
	}
	

	public String getStatusComment()
	{
		return statusComment;
	}
	
	public void setStatusComment(String statusComment)
	{
		this.statusComment = statusComment;
	}
	
	
	public StartAtType getStartAtType()
	{
		return startAtType;
	}

	public void setStartAtType(StartAtType startAtType)
	{
		this.startAtType = startAtType;
	}
	
	public String getStartAtTypeString()
	{
		if (startAtType != null)
			return startAtType.getStringType();
		return StartAtType.DEFAULT.getStringType();
	}

	public void setStartAtTypeString(String startAtType)
	{
		this.startAtType = StartAtType.getValue(startAtType);
	}
	
	
	public boolean isWaitNextDay()
	{
		return this.waitNextDay;
	}
	
	public void setWaitNextDay(boolean waitNextDay)
	{
		this.waitNextDay = waitNextDay;
	}
	
	
	public Throwable getError()
	{
		return error;
	}
	
	public void setError(Throwable error)
	{
		this.error = error;
	}
}
