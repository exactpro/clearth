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

package com.exactprosystems.clearth.automation.async;

import java.util.Date;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.report.Result;

public class AsyncActionData
{
	private final Action action;
	private final StepContext stepContext;
	private final MatrixContext matrixContext;
	private Result result;
	private Date started, 
			finished;
	
	public AsyncActionData(Action action, StepContext stepContext, MatrixContext matrixContext)
	{
		this.action = action;
		this.stepContext = stepContext;
		this.matrixContext = matrixContext;
	}

	
	public Action getAction()
	{
		return action;
	}

	public StepContext getStepContext()
	{
		return stepContext;
	}

	public MatrixContext getMatrixContext()
	{
		return matrixContext;
	}


	public Result getResult()
	{
		return result;
	}

	public void setResult(Result result)
	{
		this.result = result;
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
}
