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

	@DataProvider(name = "trim")
	Object[][] createDataForTrimLeft()
	{
		return new Object[][]
				{
						// length (for trimleft) or offset (for trimright), string to trim, expected string
						{7, "abcdef", "abcdef"},
						{6, "abcdef", "abcdef"},
						{0, "abcdef", ""},
						{6, "abcdefgjklmnoabcdef", "abcdef"}
				};
	}

	@Test(dataProvider = "trim")
	public void checkTrimleft(int length, String stringToTrim, String expectedString)
	{
		String actualString = functionsWithHolidays.trimleft(stringToTrim, length);
		assertEquals(actualString, expectedString);
	}

	@Test(dataProvider = "trim")
	public void checkTrimright(int offset, String stringToTrim, String expectedString)
	{
		String actualString = functionsWithHolidays.trimright(stringToTrim, offset);
		assertEquals(actualString, expectedString);
	}

	@DataProvider(name = "add-zeros")
	Object[][] createDataForAddZeros()
	{
		return new Object[][]
				{
						// string to add zeros, expected string
						{"123456", "123456"},
						{"000000", "000000"},
						{"100.", "100."}
				};
	}

	@Test(dataProvider = "add-zeros")
	public void checkAddZeros(String stringToAddZeros, String expectedString)
	{
		String actualString = functionsWithHolidays.addZeros(stringToAddZeros);
		assertEquals(actualString, expectedString);
	}

	@DataProvider(name = "add-zeros-with-zeros")
	Object[][] createDataForAddZerosWithZeros()
	{
		return new Object[][]
				{
						// string to add zeros, expected string, number of zeros
						{"123456", "123456", 0},
						{"123456", "123456", -1},
						{"000000", "000000.000", 3},
						{"100.", "100.00", 2}
				};
	}

	@Test(dataProvider = "add-zeros-with-zeros")
	public void checkAddZerosWithZeros(String stringToAddZeros, String expectedString, int numberOfZeros)
	{
		String actualString = functionsWithHolidays.addZeros(stringToAddZeros, numberOfZeros);
		assertEquals(actualString, expectedString);
	}

	@DataProvider(name = "add-zeros-with-delimiter")
	Object[][] createDataForAddZerosWithDelimiter()
	{
		return new Object[][]
				{
						// string to add zeros, expected string, number of zeros, delimiter
						{"123456", "123456", 0, "."},
						{"123456", "123456", -1, "."},
						{"000000", "000000.000", 3, "."},
						{"00000", "00000/00", 2, "/"},
						{"100.", "100.00", 2, "."}
				};
	}

	@Test(dataProvider = "add-zeros-with-delimiter")
	public void checkAddZerosWithDelimiter(String stringToAddZeros, String expectedString, int numberOfZeros,
	                                       String delimiter)
	{
		String actualString = functionsWithHolidays.addZeros(stringToAddZeros, numberOfZeros, delimiter);
		assertEquals(actualString, expectedString);
	}

	@DataProvider(name = "trim-zeros")
	Object[][] createDataForTrimZeros()
	{
		return new Object[][]
				{
						// string to trim, expected string
						{"123456000", "123456000"},
						{"123456.000", "123456"},
						{"12345.6000", "12345.6"},
						{"12345.6", "12345.6"},
						{"12345.6070", "12345.607"},
						{"12345.", "12345"}
				};
	}

	@Test(dataProvider = "trim-zeros")
	public void checkTrimZeros(String stringToTrim, String expectedString)
	{
		String actualString = functionsWithHolidays.trimZeros(stringToTrim);
		assertEquals(actualString, expectedString);
	}

}
