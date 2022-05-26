/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.Map;

import com.exactprosystems.clearth.ValueGenerator;

public abstract class MatrixFunctionsFactory<T extends MatrixFunctions> {

	public MatrixFunctions createMatrixFunctions(Map<String, Boolean> holidays,
												 Date businessDay,
												 Date baseTime,
												 boolean weekendHoliday,
												 ValueGenerator valueGenerator)
	{
		Class<T> functions = getRealClass();
		try
		{
			Constructor<T> constructor = functions.getConstructor(Map.class, Date.class, Date.class, boolean.class, ValueGenerator.class);
			return constructor.newInstance(holidays, businessDay, baseTime, weekendHoliday, valueGenerator);
		}
		catch (Exception e)
		{
			throw new RuntimeException("Cannot create Matrix Functions", e);
		}
	}

	public MatrixFunctions createMatrixFunctions(Map<String, Boolean> holidays,
												 Date businessDay,
												 Date baseTime,
												 boolean weekendHoliday,
												 SchedulerFactory schedulerFactory)
	{
		return createMatrixFunctions(holidays, businessDay, baseTime, weekendHoliday, schedulerFactory.getExecutorFactory().getValueGenerator());
	}

	public MatrixFunctions createMatrixFunctions(Scheduler scheduler, SchedulerFactory schedulerFactory)
	{
		return createMatrixFunctions(scheduler.getHolidays(),
									 scheduler.getBusinessDay(),
									 scheduler.getBaseTime(),
									 scheduler.isWeekendHoliday(),
									 schedulerFactory.getExecutorFactory().getValueGenerator());
	}

	public abstract Class<T> getRealClass();
}
