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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.TimeoutAwaiter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties({ "error" })
public class ReportStatus
{
	public Date started = null, finished = null;
	public boolean passed;
	public FailReason failReason = FailReason.FAILED;
	public List<String> comments;
	public Throwable error;
	public long actualTimeout;
	public long waitBeforeAction;

	public ReportStatus()
	{
	}
	
	public ReportStatus(boolean passed)
	{
		this.passed = passed;
		this.comments = null;
		this.error = null;
	}
	
	public ReportStatus(boolean passed, List<String> comments)
	{
		this.passed = passed;
		this.comments = comments;
		this.error = null;
	}
	
	public ReportStatus(boolean passed, String comment)
	{
		this.passed = passed;
		if (comment != null)
		{
			this.comments = new ArrayList<String>();
			this.comments.add(comment);
		}
		this.error = null;
	}
	
	public ReportStatus(boolean passed, List<String> comments, Throwable error)
	{
		this.passed = passed;
		this.comments = comments;
		this.error = error;
	}
	
	public ReportStatus(boolean passed, String comment, Throwable error)
	{
		this.passed = passed;
		if (comment != null)
		{
			this.comments = new ArrayList<String>();
			this.comments.add(comment);
		}
		this.error = error;
	}
	
	public ReportStatus(Action action)
	{
		Result result = action.getResult();
		if (result != null)
		{
			this.passed = result.isSuccess();
			
			String comment = result.getComment();
			if (comment != null)
			{
				this.comments = new ArrayList<String>();
				this.comments.add(comment);
			}
			
			this.error = result.getError();
			this.failReason = result.getFailReason();
		}
		else
		{
			this.passed = action.isPassed();
		}
		this.started = action.getStarted();
		this.finished = action.getFinished();
		if (action instanceof TimeoutAwaiter && ((TimeoutAwaiter)action).isUsesTimeout())
			this.actualTimeout = ((TimeoutAwaiter) action).getAwaitedTimeout();
		else
			this.waitBeforeAction = action.getTimeOut();
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
	
	public boolean isPassed()
	{
		return passed;
	}
	
	public void setPassed(boolean passed)
	{
		this.passed = passed;
	}
	
	public FailReason getFailReason()
	{
		return failReason;
	}
	
	public void setFailReason(FailReason failReason)
	{
		this.failReason = failReason;
	}
	
	public List<String> getComments()
	{
		return comments;
	}
	
	public void setComments(List<String> comments)
	{
		this.comments = comments;
	}
	
	public Throwable getError()
	{
		return error;
	}
	
	public void setError(Throwable error)
	{
		this.error = error;
	}

	public long getActualTimeout()
	{
		return actualTimeout;
	}

	public void setActualTimeout(long actualTimeout)
	{
		this.actualTimeout = actualTimeout;
	}

	public long getWaitBeforeAction()
	{
		return waitBeforeAction;
	}

	public void setWaitBeforeAction(long waitBeforeAction)
	{
		this.waitBeforeAction = waitBeforeAction;
	}
}
