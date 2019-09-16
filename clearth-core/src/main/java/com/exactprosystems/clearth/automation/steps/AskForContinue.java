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

package com.exactprosystems.clearth.automation.steps;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.SchedulerSuspension;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.StepImpl;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;

public class AskForContinue extends StepImpl
{
	private final static Logger logger = LoggerFactory.getLogger(AskForContinue.class);
	
	@Override
	public Result execute(Map<Matrix, StepContext> stepContexts, GlobalContext globalContext)
	{
		try
		{
			SchedulerSuspension suspension = (SchedulerSuspension)parameters.get("suspension");
			if (suspension==null)
				return null;
			synchronized (suspension)
			{
				suspension.setReplayStep(false);
				suspension.setSuspended(true);
				suspension.wait();
			}
			return null;
		}
		catch (InterruptedException e)
		{
			logger.error("Wait interrupted", e);
			Result stepResult = new DefaultResult();
			stepResult.setSuccess(false);
			stepResult.setError(e);
			return stepResult;
		}
	}
}
