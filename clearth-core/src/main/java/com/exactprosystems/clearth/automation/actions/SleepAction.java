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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.Stopwatch;

/**
 * Created by alexey.karpukhin on 9/27/17.
 */
public class SleepAction extends Action implements TimeoutAwaiter
{
	protected long awaitedTimeout;

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException, FailoverException
	{
		if (timeout <= 0)
			return null;
		
		Stopwatch sw = Stopwatch.createAndStart();
		try
		{
			Thread.sleep(timeout);
		}
		catch (InterruptedException e)
		{
			return DefaultResult.failed("Interrupted");
		}
		finally
		{
			awaitedTimeout = sw.stop();
		}

		return null;
	}
	
	@Override
	public long getAwaitedTimeout()
	{
		return awaitedTimeout;
	}
}
