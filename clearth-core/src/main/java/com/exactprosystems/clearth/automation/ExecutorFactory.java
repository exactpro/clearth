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

package com.exactprosystems.clearth.automation;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.ValueGenerator;

public abstract class ExecutorFactory
{
	protected ValueGenerator valueGenerator;
	
	public ExecutorFactory(ValueGenerator valueGenerator)
	{
		this.valueGenerator = valueGenerator;
	}
	
	public abstract Executor createExecutor(Scheduler scheduler, List<Matrix> matrices, String startedByUser, Map<String, Preparable> preparableActions);
	public abstract Executor createExecutor(Scheduler scheduler, List<Step> steps, List<Matrix> matrices, GlobalContext globalContext, Map<String, Preparable> preparableActions);
	public abstract GlobalContext createGlobalContext(Date businessDay, Date baseTime, boolean weekendHoliday, Map<String, Boolean> holidays, String startedByUser);
	public abstract FailoverStatus createFailoverStatus();
	
	public ValueGenerator getValueGenerator()
	{
		return valueGenerator;
	}
}
