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

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.StepImpl;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ParamDescription(description = "Specifies duration of sleep in seconds")
public class Sleep extends StepImpl
{
	private final static Logger logger = LoggerFactory.getLogger(Sleep.class);

	@Override
	public Result execute(Map<Matrix, StepContext> stepContexts, GlobalContext globalContext)
	{
		if (parameters==null)
			return null;
		String sleep = (String) parameters.get("sleep");
		if ((sleep==null) || (sleep.equals("")))
			return null;

		try
		{
			int toSleep = Integer.parseInt(sleep);
			Thread.sleep(toSleep*1000);
			return null;
		}
		catch (NumberFormatException e)
		{
			logger.error("Error while parsing parameter '"+sleep+"', it must be an integer", e);
			return null;
		}
		catch (InterruptedException e)
		{
			logger.error("Sleep interrupted", e);
			Result stepResult = new DefaultResult();
			stepResult.setSuccess(false);
			stepResult.setError(e);
			return stepResult;
		}
	}
}
