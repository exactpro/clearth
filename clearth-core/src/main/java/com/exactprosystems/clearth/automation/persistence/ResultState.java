/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("ResultState")
public class ResultState
{
	private Class<?> resultClass = null;
	
	private boolean success = true, crashed = false;
	private Throwable error = null;
	private String message = null, comment = null;
	private FailReason failReason = FailReason.FAILED;
	//Result details are cleaned after action end, no need to store them
	
	public ResultState()
	{
	}
	
	public ResultState(Result result)
	{
		this.resultClass = result.getClass();
		
		this.success = result.isSuccess();
		this.crashed = result.isCrashed();
		this.error = result.getError();
		this.message = result.getMessage();
		this.comment = result.getComment();
		this.failReason = result.getFailReason();
	}
	
	public Result resultFromState()
	{
		Result result = createResult();
		
		result.setSuccess(this.success);
		result.setCrashed(this.crashed);
		result.setError(this.error);
		result.setMessage(this.message);
		result.setComment(this.comment);
		result.setFailReason(this.failReason);
		
		initResult(result);
		
		return result;
	}
	
	
	protected Result createResult()
	{
		return new DefaultResult();
	}
	
	protected void initResult(Result result)
	{
	}
	
	
	public Class<?> getResultClass()
	{
		return resultClass;
	}
	
	public void setResultClass(Class<?> resultClass)
	{
		this.resultClass = resultClass;
	}
	
	
	public boolean isSuccess()
	{
		return success;
	}
	
	public void setSuccess(boolean success)
	{
		this.success = success;
	}
	
	
	public boolean isCrashed()
	{
		return crashed;
	}
	
	public void setCrashed(boolean crashed)
	{
		this.crashed = crashed;
	}
	
	
	public Throwable getError()
	{
		return error;
	}
	
	public void setError(Throwable error)
	{
		this.error = error;
	}
	
	
	public String getMessage()
	{
		return message;
	}
	
	public void setMessage(String message)
	{
		this.message = message;
	}
	
	
	public String getComment()
	{
		return comment;
	}
	
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	
	
	public FailReason getFailReason()
	{
		return failReason;
	}
	
	public void setFailReason(FailReason failReason)
	{
		this.failReason = failReason;
	}
}
