/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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
package com.exactprosystems.clearth.automation.actions.woodpecker;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;

import static com.exactprosystems.clearth.automation.actions.woodpecker.WoodpeckerActionUtils.*;
import static java.lang.Math.min;
import static java.lang.String.format;

public class WaitForDaemonLoadEnd extends Action implements TimeoutAwaiter
{
	private static final long PERIOD_MS = 200;
	
	private long awaitedTimeout;
	

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		String daemonType = handler.getRequiredString(DAEMON_TYPE);
		String daemonName = handler.getString(DAEMON_NAME, DEFAULT_DAEMON_NAME);
		String runId = handler.getRequiredString(RUN_ID);
		handler.check();

		WoodpeckerDaemon daemon = findDaemon(daemonType, daemonName);
		
		return waitForLoadEnd(daemon, runId, daemonType, daemonName);
	}
	
	private Result waitForLoadEnd(WoodpeckerDaemon daemon, String runId, String daemonType, String daemonName)
	{
		Stopwatch stopwatch = Stopwatch.createAndStart(timeout);
		try
		{
			while (true)
			{
				if (!daemon.isRunning() || !runId.equals(daemon.getRunId()))
					return DefaultResult.passed(format("Load of daemon '%s - %s' has been ended.", 
							daemonType, daemonName));
				
				if (stopwatch.isExpired())
					return DefaultResult.failed(format("Load of daemon '%s - %s' hasn't been ended in %d ms.", 
							daemonType, daemonName, timeout));
				
				long sleepTime = min((timeout - stopwatch.getElapsedMillis()), PERIOD_MS);
				Thread.sleep(sleepTime);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return DefaultResult.failed("Wait for load end has been interrupted before the timeout expiration.");
		}
		finally
		{
			awaitedTimeout = stopwatch.stop();
		}
	}


	@Override
	public long getAwaitedTimeout()
	{
		return awaitedTimeout;
	}
}
