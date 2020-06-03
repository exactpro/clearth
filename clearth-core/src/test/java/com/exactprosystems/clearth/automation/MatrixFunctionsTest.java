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

import com.exactprosystems.clearth.*;
import com.exactprosystems.clearth.automation.exceptions.FunctionException;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.util.Arrays.asList;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

/**
 * 03 June 2019
 */
public class MatrixFunctionsTest extends BasicTestNgTest
{
	private static final String DD_MM_YY_PATTERN = "ddMMyy";

	public static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));
	public static final Path MATRIX_FUNCTIONS_TEST_OUTPUT_DIR = Paths.get("testOutput/MatrixFunctions");
	private static final String MATRIX_FUNCTIONS_TEST_RESOURCE_DIR = "MatrixFunctionsTest";
	public static final String TEST_GENERATOR_FILE = USER_DIR + "/value_generator_test.txt";
	public static final String DEFAULT_GENERATOR_FILE = "value_generator.txt";

	private volatile MatrixFunctions functionsWithHolidays;

	@BeforeClass
	void prepare() throws IOException
	{
		initFunctions();
	}

	private void prepareFilesForValueGenerators() throws IOException
	{
		Files.createDirectories(MATRIX_FUNCTIONS_TEST_OUTPUT_DIR);
		Path originalDefaultGeneratorFile =
				Paths.get(resourceToAbsoluteFilePath(MATRIX_FUNCTIONS_TEST_RESOURCE_DIR)).resolve(
						DEFAULT_GENERATOR_FILE);
		Path copiedDefaultGeneratorFile = MATRIX_FUNCTIONS_TEST_OUTPUT_DIR.resolve(DEFAULT_GENERATOR_FILE);
		Files.copy(originalDefaultGeneratorFile, copiedDefaultGeneratorFile, StandardCopyOption.REPLACE_EXISTING);

		Path testGeneratorFile = Paths.get(TEST_GENERATOR_FILE);
		if (Files.exists(testGeneratorFile))
		{
			Files.delete(testGeneratorFile);
		}
	}

	private void initFunctions() throws IOException
	{
		Date businessDay = new GregorianCalendar(2019, Calendar.APRIL, 30).getTime();

		Map<String, Boolean> holidays = new HashMap<>();
		holidays.put("20190501", true);
		holidays.put("20190502", true);
		holidays.put("20190503", true);
		holidays.put("20190507", true);
		holidays.put("20190509", true);
		holidays.put("20190510", true);

		prepareFilesForValueGenerators();
		ValueGenerator valueGenerator =
				new ValueGenerator(MATRIX_FUNCTIONS_TEST_OUTPUT_DIR.resolve(DEFAULT_GENERATOR_FILE).toString(),
						"default");

		functionsWithHolidays = new MatrixFunctions(holidays,
				businessDay,
				null,
				true,
				valueGenerator);
	}

	@Override
	protected void mockOtherApplicationFields(ClearThCore application)
	{
		ValueGenerators valueGenerators = new ValueGenerators();
		when(application.getValueGenerators()).thenReturn(valueGenerators);
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


	@DataProvider(name = "avg")
	Object[][] createDataForAvg()
	{
		return new Object[][]
				{
						// a, b, result
						{"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111",
								"333333333333333333333333333333333333333333333333333333333333333333333333333333333333333333",
								"222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222"
						},
						{
								"9.99",
								"3.33",
								"6.66"
						},
						{
								"00000000000000000000000000.00",
								"88888888888888888888888888.88",
								"44444444444444444444444444.44"
						},
				};
	}

	@Test(invocationCount = 30, threadPoolSize = 10)
	public void checkAvg()
	{
		double a = Math.random();
		double b = Math.random();
		BigDecimal expectedResult = (BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).
				divide(BigDecimal.valueOf(2)));
		BigDecimal actualResult = functionsWithHolidays.avg(a, b);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "avg")
	public void checkAvg(String a, String b, String expectedResult)
	{
		BigDecimal actualResult = functionsWithHolidays.avg(new BigDecimal(a), new BigDecimal(b));
		assertEquals(actualResult, new BigDecimal(expectedResult));
	}


	@DataProvider(name = "add-sub")
	Object[][] createDataForAdd()
	{
		return new Object[][]
				{
						// a, b, result, scale
						//result, b, a, scale
						{
								"33.33333333333333333333333333333333333333333333333333333333333333333333333333333333333",
								"11.11111111111111111111111111111111111111111111111111111111111111111111111111111111111",
								"44.44444444444444444444444444444444444444444444444444444444444444444444444444444444444",
								-1
						},
						{
								"9.99",
								"3.33",
								"13.32",
								-1
						},
						{
								"88.88",
								"00.00",
								"88.88",
								-1
						},
						{
								"88888888888.777777222222",
								"11111111111.111111222222",
								"99999999999.888888444444",
								12
						},
				};
	}

	@Test(dataProvider = "add-sub")
	public void checkAddWithScale(String a, String b, String expectedResult, int scale)
	{
		if (scale >= 0)
		{
			BigDecimal bd = new BigDecimal(expectedResult);
			expectedResult = bd.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
		}
		String actualResult = functionsWithHolidays.add(a, b, scale);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "add-sub")
	public void checkAdd(String a, String b, String expectedResult, int scale)
	{
		BigDecimal bd = new BigDecimal(expectedResult);
		expectedResult = bd.setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

		String actualResult = functionsWithHolidays.add(a, b);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "add-sub")
	public void checkSubWithScale(String expectedResult, String b, String a, int scale)
	{
		if (scale >= 0)
		{
			BigDecimal bd = new BigDecimal(expectedResult);
			expectedResult = bd.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
		}
		String actualResult = functionsWithHolidays.sub(a, b, scale);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "add-sub")
	public void checkSub(String expectedResult, String b, String a, int scale)
	{
		BigDecimal bd = new BigDecimal(expectedResult);
		expectedResult = bd.setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

		String actualResult = functionsWithHolidays.sub(a, b);
		assertEquals(actualResult, expectedResult);
	}


	@DataProvider(name = "mul-div")
	Object[][] createDataForMul()
	{
		return new Object[][]
				{
						// a, b, result, scale
						{
								"3333333333333333333333333333333333333333333333333",
								"1111111111111111111111111111111111111111111111111",
								"3703703703703703703703703703703703703703703703702962962962962962962962962962962962962962962962963",
								-1
						},
						{
								"00000000000000000000000000000000000000000000000000000000000000000000000000.0",
								"888888888888888888888888888",
								"0.0",
								-1
						},
						{
								"1122.0000112212122211111111122211111111111111111111111111111112222222212121212121",
								"0002.0000000000000000000000000000000000000000000000000000000000000000000000000000",
								"2244.0000224424244422222222244422222222222222222222222222222224444444424242424242",
								12
						},
						{
								"132.12",
								"234.45",
								"30975.534",
								3
						},
						{
								"232.1",
								"234.45",
								"54415.845",
								-1
						}
				};
	}

	@Test(dataProvider = "mul-div")
	public void checkMulWithScale(String a, String b, String expectedResult, int scale)
	{
		if (scale >= 0)
		{
			BigDecimal bd = new BigDecimal(expectedResult);
			expectedResult = bd.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
		}

		String actualResult = functionsWithHolidays.mul(a, b, scale);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "mul-div")
	public void checkMul(String a, String b, String expectedResult, int scale)
	{
		BigDecimal bd = new BigDecimal(expectedResult);
		expectedResult = bd.setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

		String actualResult = functionsWithHolidays.mul(a, b);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "mul-div")
	public void checkDivWithScale(String expectedResult, String b, String a, int scale)
	{
		BigDecimal bd = new BigDecimal(expectedResult);
		if (scale >= 0)
		{
			bd = bd.setScale(scale, RoundingMode.HALF_UP);
		}
		expectedResult = bd.stripTrailingZeros().toPlainString();

		String actualResult = functionsWithHolidays.div(a, b, scale);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "mul-div")
	public void checkDiv(String expectedResult, String b, String a, int scale)
	{
		expectedResult =
				new BigDecimal(expectedResult).setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

		String actualResult = functionsWithHolidays.div(a, b);
		assertEquals(actualResult, expectedResult);
	}


	@DataProvider(name = "min-max")
	Object[][] createDataForMinMax()
	{
		return new Object[][]
				{
						// a (min value), b (max value), minResult (using right type), maxResult (using right type)
						{
								new BigDecimal(
										"111.11"),
								new BigDecimal(
										"333.33"),
								new BigDecimal(
										"111.11"),
								new BigDecimal(
										"333.33"),
						},
						{
								-1,
								0,
								(long) -1,
								0L
						},
						{
								12345,
								23456,
								(long) 12345,
								(long) 23456
						},
						{
								1.999,
								2,
								BigDecimal.valueOf(1.999),
								new BigDecimal(2)
						},
						{
								-10.12,
								-5.13,
								BigDecimal.valueOf(-10.12),
								BigDecimal.valueOf(-5.13)
						},
						{
								5.13,
								10.12,
								BigDecimal.valueOf(5.13),
								BigDecimal.valueOf(10.12)
						}
				};
	}

	@Test(dataProvider = "min-max")
	public void checkMin(Number a, Number b, Object expectedResult, Object max)
	{
		Number actualResult = functionsWithHolidays.min(a, b);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "min-max")
	public void checkMax(Number a, Number b, Object min, Object expectedResult)
	{
		Number actualResult = functionsWithHolidays.max(a, b);
		assertEquals(actualResult, expectedResult);
	}

	@DataProvider(name = "to-big-dec-from-string")
	Object[][] createDataForToBigDecimal()
	{
		return new Object[][]
				{
						// string to parse, expected result
						{
								"11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111.1",
								new BigDecimal(
										"11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111.1")
						},
						{
								"123456789123456789123456789456123456789456132467.894561231",
								new BigDecimal(
										"123456789123456789123456789456123456789456132467.894561231"),
						},
						{
								"3",
								new BigDecimal(
										3),
						},
						{
								"0",
								new BigDecimal(
										0),
						},
						{
								"-1",
								new BigDecimal(
										-1),
						},
				};
	}

	@Test(dataProvider = "to-big-dec-from-string")
	public void checkToBigDecimal(String number, BigDecimal expectedBd) throws ParseException
	{
		BigDecimal actualBd = functionsWithHolidays.toBigDecimal(number);
		assertEquals(actualBd, expectedBd);
	}

	@DataProvider(name = "to-big-dec-from-string-with-dec-sep")
	Object[][] createDataForToBigDecimalWithDecSeparator()
	{
		return new Object[][]
				{
						// string to parse, expected result, decimal separator
						{
								"11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111.1",
								new BigDecimal(
										"11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111.1"),
								"."
						},
						{
								"1111111," +
										"11111111111111111111111111111111111111111111111111111111111111111111111111111111111",
								new BigDecimal(
										"1111111." +
												"11111111111111111111111111111111111111111111111111111111111111111111111111111111111"),
								",",
						},
						{
								"2222222222222222222222222222222222222222222222222222222222222222222222222222222222222222.1",
								new BigDecimal(
										"2222222222222222222222222222222222222222222222222222222222222222222222222222222222222222.1"),
								null,
						},
						{
								"3,1",
								new BigDecimal(
										"3.1"),
								","
						},
						{
								"0",
								new BigDecimal(
										0),
								null
						},
				};
	}

	@Test(dataProvider = "to-big-dec-from-string-with-dec-sep")
	public void checkToBigDecimalWithDecSeparator(String number, BigDecimal expectedBd, String decimalSeparator)
			throws ParseException
	{
		BigDecimal actualBd = functionsWithHolidays.toBigDecimal(number, decimalSeparator);
		assertEquals(actualBd, expectedBd);
	}

	@DataProvider(name = "to-big-dec-from-string-with-group-sep")
	Object[][] createDataForToBigDecimalWithGroupSeparator()
	{
		return new Object[][]
				{
						// string to parse, expected result, decimal separator, group separator
						{
								"111 " +
										"111 " +
										"111 " +
										"111 " +
										"111 " +
										"111 111 111 " +
										"111 111 111 111 111 111 111 111 111 111 111 111 " +
										"111 111 111 111 111 111 111 111 111 111.1",
								new BigDecimal(
										"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111.1"),
								".",
								" "
						},
						{
								"1111111," +
										"11111111111111111111111111111111111111111111111111111111111111111111111111111111111",
								new BigDecimal(
										"1111111." +
												"11111111111111111111111111111111111111111111111111111111111111111111111111111111111"),
								",",
								null
						},
						{
								"222" +
										"/222" +
										"/222" +
										"/222" +
										"/222" +
										"/222/222/222" +
										"/222/222/222/222/222/222/222/222/222/222/222/222" +
										"/222/222/222/222/222/222/222/222/222/222.1",
								new BigDecimal(
										"222222222222222222222222222222222222222222222222222222222222222222222222222222222222222222.1"),
								".",
								"/"

						},
						{
								"3,1",
								new BigDecimal(
										"3.1"),
								",",
								null
						},
						{
								"0",
								new BigDecimal(
										0),
								null,
								null
						},
				};
	}

	@Test(dataProvider = "to-big-dec-from-string-with-group-sep")
	public void checkToBigDecimalWithGroupSeparator(String number, BigDecimal expectedBd, String decimalSeparator,
	                                                String groupSeparator) throws ParseException
	{
		BigDecimal actualBd = functionsWithHolidays.toBigDecimal(number, decimalSeparator, groupSeparator);
		assertEquals(actualBd, expectedBd);
	}

	@DataProvider(name = "round")
	Object[][] createDataForRound()
	{
		return new Object[][]
				{
						// string to parse and round, int scale, round result, roundUp result, roundDown result
						{
								"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111.1",
								0,
								"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111",
								"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111112",
								"111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"
						},
						{
								"11.555",
								2,
								"11.56",
								"11.56",
								"11.55",
						},
						{
								"11.33",
								1,
								"11.3",
								"11.4",
								"11.3",
						},
						{
								"-11.55",
								2,
								"-11.55",
								"-11.55",
								"-11.55",
						},
				};
	}

	@Test(dataProvider = "round")
	public void checkRound(String number, int scale, String expectedRoundResult, String expectedRoundUpResult,
	                       String expectedRoundDownResult)
	{
		String actualResult = functionsWithHolidays.round(number, scale);
		assertEquals(actualResult, expectedRoundResult);
	}

	@Test(dataProvider = "round")
	public void checkRoundUp(String number, int scale, String expectedRoundResult, String expectedRoundUpResult,
	                         String expectedRoundDownResult)
	{
		String actualResult = functionsWithHolidays.roundUp(number, scale);
		assertEquals(actualResult, expectedRoundUpResult);
	}

	@Test(dataProvider = "round")
	public void checkRoundDown(String number, int scale, String expectedRoundResult, String expectedRoundUpResult,
	                           String expectedRoundDownResult)
	{
		String actualResult = functionsWithHolidays.roundDown(number, scale);
		assertEquals(actualResult, expectedRoundDownResult);
	}

	@DataProvider(name = "value-generator")
	Object[][] createDataForIdGenerator()
	{
		return new Object[][]
				{
						// generatorId, pattern, expectedResults
						//Tests for existing prevValue-file
						{
								MatrixFunctions.DEFAULT_VALUE_GENERATOR,
								"Value_gggg",
								asList("Value_t124", "Value_t125", "Value_t126", "Value_t127", "Value_t128")
						},
						//Tests for case then prevValue-file doesn't exist
						{
								"test",
								"test_g",
								asList("test_0", "test_1", "test_2", "test_3", "test_4")
						},
				};
	}

	@Test(dataProvider = "value-generator")
	public void checkGenerate(String generatorId, String pattern, List<String> expectedResults)
	{
		String actualResult;
		if (generatorId.equals(MatrixFunctions.DEFAULT_VALUE_GENERATOR))
		{
			for (String expectedResult : expectedResults)
			{
				actualResult = functionsWithHolidays.generate(pattern);
				assertEquals(actualResult, expectedResult);
			}
		}
		else
		{
			for (String expectedResult : expectedResults)
			{
				actualResult = functionsWithHolidays.generate(generatorId, pattern);
				assertEquals(actualResult, expectedResult);
			}
		}
	}


	private long parseDateAndTimeToMillis(String value, String format) throws ParseException
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setLenient(false);
		Date date = sdf.parse(value);
		return date.getTime();
	}

	@DataProvider(name = "date-format")
	Object[][] createDataForDateTimeFormat()
	{
		return new Object[][]
				{
						// Format mask, formatted time, reformat mask, reformatted time,
						// date8-formatted time, time6-formatted time
						{
								"yyyy/MM/dd",
								"2020/06/03",
								"dd-MM-yyyy",
								"03-06-2020",
								"20200603",
								"000000"
						},
						{
								"yyyy-MM-dd-hh",
								"2020-06-03-02",
								"dd MM yyyy hh",
								"03 06 2020 02",
								"20200603",
								"020000"
						},
						{
								"yyyy.MM.dd-hh:mm",
								"2020.06.03-02:14",
								"dd.MM.yyyy hh:mm",
								"03.06.2020 02:14",
								"20200603",
								"021400"
						},
						{
								"yyyy/MM/dd/hh/mm/ss",
								"2020/06/03/02/14/04",
								"dd.MM.yyyy hh-mm:ss",
								"03.06.2020 02-14:04",
								"20200603",
								"021404"
						},
				};
	}

	@Test(dataProvider = "date-format")
	public void checkFormat(String formatStr, String expectedFormatResult, String reformatStr,
	                        String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		String actualResult = functionsWithHolidays.format(millis, formatStr);
		assertEquals(actualResult, expectedFormatResult);
	}

	@Test(dataProvider = "date-format")
	public void checkReformat(String formatStr, String expectedFormatResult, String reformatStr,
	                          String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws FunctionException
	{
		String actualResult = functionsWithHolidays.reformat(expectedFormatResult, formatStr, reformatStr);
		assertEquals(actualResult, expectedReformatResult);
	}

	@Test(dataProvider = "date-format")
	public void checkParseDate(String formatStr, String expectedFormatResult, String reformatStr,
	                           String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws FunctionException, ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		long actualResult = functionsWithHolidays.parseDate(expectedFormatResult, formatStr);
		assertEquals(actualResult, millis);
	}


	@Test(dataProvider = "date-format")
	public void checkDate8(String formatStr, String expectedFormatResult, String reformatStr,
	                       String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		String actualResult = functionsWithHolidays.date8(millis);
		assertEquals(actualResult, expectedDate8Result);
	}

	@Test(dataProvider = "date-format")
	public void checkTime6(String formatStr, String expectedFormatResult, String reformatStr,
	                       String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		String actualResult = functionsWithHolidays.time6(millis);
		assertEquals(actualResult, expectedTime6Result);
	}

	@Test(dataProvider = "date-format")
	public void checkMilliseconds(String formatStr, String expectedFormatResult, String reformatStr,
	                              String expectedReformatResult, String expectedDate8Result,
	                              String expectedTime6Result)
			throws FunctionException, ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		String actualResult = functionsWithHolidays.milliseconds(expectedFormatResult, formatStr);
		String expectedResult = String.valueOf(millis);
		assertEquals(actualResult, expectedResult);
	}


	@AfterMethod
	public void tearDown() throws IOException
	{
		FileUtils.cleanDirectory(MATRIX_FUNCTIONS_TEST_OUTPUT_DIR.toFile());
	}
}