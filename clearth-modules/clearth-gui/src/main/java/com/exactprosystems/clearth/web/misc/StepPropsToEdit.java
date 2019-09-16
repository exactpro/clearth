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

package com.exactprosystems.clearth.web.misc;

/*
 * Flags in this class show what properties of the steps should be edited while changing multiple steps
 * */

public class StepPropsToEdit
{
	private boolean kind, startAt, startAtType, waitNextDay, parameter, askForContinue, askIfFailed, execute, comment;

	public StepPropsToEdit()
	{
		kind = false;
		startAt = false;
		startAtType = false;
		waitNextDay = false;
		parameter = false;
		askForContinue = false;
		askIfFailed = false;
		execute = false;
		comment = false;
	}
	
	
	public boolean isKind()
	{
		return kind;
	}

	public void setKind(boolean kind)
	{
		this.kind = kind;
	}

	
	public boolean isStartAt()
	{
		return startAt;
	}

	public void setStartAt(boolean startAt)
	{
		this.startAt = startAt;
	}
	
	
	public boolean isStartAtType()
	{
		return startAtType;
	}

	public void setStartAtType(boolean startAtType)
	{
		this.startAtType = startAtType;
	}
	
	
	public boolean isWaitNextDay()
	{
		return this.waitNextDay;
	}
	
	public void setWaitNextDay(boolean waitNextDay)
	{
		this.waitNextDay = waitNextDay;
	}


	public boolean isParameter()
	{
		return parameter;
	}

	public void setParameter(boolean parameter)
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

	
	public boolean isComment()
	{
		return comment;
	}

	public void setComment(boolean comment)
	{
		this.comment = comment;
	}
}
