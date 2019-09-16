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

import com.exactprosystems.clearth.automation.exceptions.FunctionException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.testng.Assert.*;

/**
 * 03 June 2019
 */
public class MatrixFunctionsTest
{
	private static final String DD_MM_YY_PATTERN = "ddMMyy";
	
	private volatile MatrixFunctions functionsWithHolidays;
	
	
	@BeforeClass
	void prepare()
	{
		initFunctions();
	}
	
	private void initFunctions()
	{
		Date businessDay = new GregorianCalendar(2019, Calendar.APRIL, 30).getTime();

		Map<String, Boolean> holidays = new HashMap<>();
		holidays.put("20190501", true);
		holidays.put("20190502", true);
		holidays.put("20190503", true);
		holidays.put("20190507", true);
		holidays.put("20190509", true);
		holidays.put("20190510", true);
		
		functionsWithHolidays = new MatrixFunctions(holidays, 
		                                            businessDay, 
		                                            null,
		                                            true, 
		                                            null);
	}
	
	
	
	@DataProvider(name = "time")
	Object[][] createDataForTime()
	{
		return new Object[][]
				{
						// day offset, month offset, year offset, expected date
						{0, 0, 0, "300419"},
						{1, 0, 0, "060519"},
						{2, 0, 0, "080519"},
						{3, 0, 0, "130519"}
				};
	}
	
	
	@Test(dataProvider = "time", invocationCount = 100, threadPoolSize = 10)
	void checkTime(int dayOffset, int monthOffset, int yearOffset, String expectedDate) throws FunctionException
	{
		long timestamp = functionsWithHolidays.time(dayOffset, monthOffset, yearOffset);
		
		SimpleDateFormat format = new SimpleDateFormat(DD_MM_YY_PATTERN);
		String actualDate = format.format(timestamp);
		
		assertEquals(actualDate, expectedDate);
	}
}
