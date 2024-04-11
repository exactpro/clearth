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

package com.exactprosystems.clearth.automation;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.data.TestExecutionHandler;

public class DefaultExecutorFactory extends ExecutorFactory
{
	public DefaultExecutorFactory(ValueGenerator valueGenerator)
	{
		super(valueGenerator);
	}

	@Override
	public SimpleExecutor createExecutor(Scheduler scheduler, List<Matrix> matrices, String startedByUser, Map<String, Preparable> preparableActions,
			TestExecutionHandler executionHandler)
	{
		GlobalContext globalContext = 
				createGlobalContext(scheduler.getBusinessDay(), scheduler.getBaseTime(), scheduler.isWeekendHoliday(), scheduler.getHolidays(), startedByUser, executionHandler);
		if (scheduler.isTestMode())
			globalContext.setLoadedContext(GlobalContext.TEST_MODE, true);
		SimpleExecutor result = new DefaultSimpleExecutor(scheduler, scheduler.getSteps(), matrices,
				globalContext, createFailoverStatus(), preparableActions, scheduler.getCurrentReportsConfig());
		return result;
	}
	
	@Override
	public SimpleExecutor createExecutor(Scheduler scheduler, List<Step> steps, List<Matrix> matrices, GlobalContext globalContext, Map<String, Preparable> preparableActions,
			ReportsConfig reportsConfig)
	{
		SimpleExecutor result = new DefaultSimpleExecutor(scheduler, steps, matrices, globalContext, createFailoverStatus(), preparableActions, reportsConfig);
		return result;
	}
	
	@Override
	public GlobalContext createGlobalContext(Date businessDay, Date baseTime, boolean weekendHoliday, Map<String, Boolean> holidays, String startedByUser, 
			TestExecutionHandler executionHandler)
	{
		GlobalContext globalContext = new GlobalContext(businessDay, weekendHoliday, holidays,
				ClearThCore.getInstance().createMatrixFunctions(holidays, businessDay, baseTime, weekendHoliday, valueGenerator),
				startedByUser,
				executionHandler);
		return globalContext;
	}
	
	@Override
	public FailoverStatus createFailoverStatus()
	{
		return new FailoverStatus();
	}
}
