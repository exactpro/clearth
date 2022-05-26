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

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.ValueGenerators;
import com.exactprosystems.clearth.automation.exceptions.FunctionException;
import com.exactprosystems.clearth.generators.LegacyValueGenerator;
import com.exactprosystems.clearth.generators.LegacyValueGenerators;
import com.exactprosystems.clearth.utils.ObjectWrapper;
import com.exactprosystems.clearth.utils.javaFunction.FunctionWithException;
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

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * 03 June 2019
 */
public class MatrixFunctionsTest extends BasicTestNgTest
{
	public static final Path USER_DIR = Paths.get(System.getProperty("user.dir"));
	public static final Path MATRIX_FUNCTIONS_TEST_OUTPUT_DIR = Paths.get("testOutput/MatrixFunctions");
	private static final String MATRIX_FUNCTIONS_TEST_RESOURCE_DIR = "MatrixFunctionsTest";
	public static final String TEST_GENERATOR_FILE = USER_DIR + "/value_generator_test.txt";
	public static final String DEFAULT_GENERATOR_FILE = "value_generator.txt";
	public static final String EXCEPTION_MESSAGE_REGEX = "Unable to get next holiday: holidays are not specified in " +
			"current scheduler";

	private volatile MatrixFunctions funWithHolidaysWithWeekends;
	private volatile MatrixFunctions funWithHolidaysWithoutWeekends;

	private volatile MatrixFunctions funWithoutHolidaysWithWeekends;
	private volatile MatrixFunctions funWithoutHolidaysWithoutWeekends;

	private volatile MatrixFunctions funWithHolidaysWithWeekendsBaseTime;
	private volatile MatrixFunctions funWithHolidaysWithoutWeekendsBaseTime;

	private volatile MatrixFunctions funWithoutHolidaysWithWeekendsBaseTime;
	private volatile MatrixFunctions funWithoutHolidaysWithoutWeekendsBaseTime;

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
		prepareFilesForValueGenerators();
		ValueGenerator valueGenerator =
				new LegacyValueGenerator(MATRIX_FUNCTIONS_TEST_OUTPUT_DIR.resolve(DEFAULT_GENERATOR_FILE).toString(),
						"default");

		Map<String, Boolean> holidays = new HashMap<>();
		holidays.put("20190501", true);
		holidays.put("20190502", true);
		holidays.put("20190503", true);
		holidays.put("20190507", true);
		holidays.put("20190509", true);
		holidays.put("20190510", true);
		holidays.put("20190525", false);
		holidays.put("20190526", false);
		holidays.put("20190519", false);
		holidays.put("20190531", true);

		Date businessDay = new GregorianCalendar(2019, Calendar.APRIL, 30, 2, 0, 0).getTime();
		Date baseTime = new GregorianCalendar(2019, Calendar.APRIL, 30, 0, 0, 0).getTime();

		funWithHolidaysWithWeekends = new MatrixFunctions(holidays,
				businessDay,
				null,
				true,
				valueGenerator);
		funWithHolidaysWithoutWeekends = new MatrixFunctions(holidays,
				businessDay,
				null,
				false,
				null);
		funWithoutHolidaysWithWeekends = new MatrixFunctions(null,
				businessDay,
				null,
				true,
				null);
		funWithoutHolidaysWithoutWeekends = new MatrixFunctions(null,
				businessDay,
				null,
				false,
				null);
		funWithHolidaysWithWeekendsBaseTime = new MatrixFunctions(holidays,
				businessDay,
				baseTime,
				true,
				null);
		funWithHolidaysWithoutWeekendsBaseTime = new MatrixFunctions(holidays,
				businessDay,
				baseTime,
				false,
				null);
		funWithoutHolidaysWithWeekendsBaseTime = new MatrixFunctions(null,
				businessDay,
				baseTime,
				true,
				null);
		funWithoutHolidaysWithoutWeekendsBaseTime = new MatrixFunctions(null,
				businessDay,
				baseTime,
				false,
				null);
	}

	@Override
	protected void mockOtherApplicationFields(ClearThCore application)
	{
		ValueGenerators valueGenerators = new LegacyValueGenerators();
		when(application.getValueGenerators()).thenReturn(valueGenerators);
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
		String actualString = funWithHolidaysWithWeekends.trimleft(stringToTrim, length);
		assertEquals(actualString, expectedString);
	}

	@Test(dataProvider = "trim")
	public void checkTrimright(int offset, String stringToTrim, String expectedString)
	{
		String actualString = funWithHolidaysWithWeekends.trimright(stringToTrim, offset);
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
		String actualString = funWithHolidaysWithWeekends.addZeros(stringToAddZeros);
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
		String actualString = funWithHolidaysWithWeekends.addZeros(stringToAddZeros, numberOfZeros);
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
		String actualString = funWithHolidaysWithWeekends.addZeros(stringToAddZeros, numberOfZeros, delimiter);
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
		String actualString = funWithHolidaysWithWeekends.trimZeros(stringToTrim);
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
						{"9.99", "3.33", "6.66"},
						{"00000000000000000000000000.00",
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
		BigDecimal actualResult = funWithHolidaysWithWeekends.avg(a, b);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "avg")
	public void checkAvg(String a, String b, String expectedResult)
	{
		BigDecimal actualResult = funWithHolidaysWithWeekends.avg(new BigDecimal(a), new BigDecimal(b));
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
		String actualResult = funWithHolidaysWithWeekends.add(a, b, scale);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "add-sub")
	public void checkAdd(String a, String b, String expectedResult, int scale)
	{
		BigDecimal bd = new BigDecimal(expectedResult);
		expectedResult = bd.setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

		String actualResult = funWithHolidaysWithWeekends.add(a, b);
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
		String actualResult = funWithHolidaysWithWeekends.sub(a, b, scale);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "add-sub")
	public void checkSub(String expectedResult, String b, String a, int scale)
	{
		BigDecimal bd = new BigDecimal(expectedResult);
		expectedResult = bd.setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

		String actualResult = funWithHolidaysWithWeekends.sub(a, b);
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

		String actualResult = funWithHolidaysWithWeekends.mul(a, b, scale);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "mul-div")
	public void checkMul(String a, String b, String expectedResult, int scale)
	{
		BigDecimal bd = new BigDecimal(expectedResult);
		expectedResult = bd.setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

		String actualResult = funWithHolidaysWithWeekends.mul(a, b);
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

		String actualResult = funWithHolidaysWithWeekends.div(a, b, scale);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "mul-div")
	public void checkDiv(String expectedResult, String b, String a, int scale)
	{
		expectedResult =
				new BigDecimal(expectedResult).setScale(5, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();

		String actualResult = funWithHolidaysWithWeekends.div(a, b);
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
		Number actualResult = funWithHolidaysWithWeekends.min(a, b);
		assertEquals(actualResult, expectedResult);
	}

	@Test(dataProvider = "min-max")
	public void checkMax(Number a, Number b, Object min, Object expectedResult)
	{
		Number actualResult = funWithHolidaysWithWeekends.max(a, b);
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
		BigDecimal actualBd = funWithHolidaysWithWeekends.toBigDecimal(number);
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
		BigDecimal actualBd = funWithHolidaysWithWeekends.toBigDecimal(number, decimalSeparator);
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
		BigDecimal actualBd = funWithHolidaysWithWeekends.toBigDecimal(number, decimalSeparator, groupSeparator);
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
		String actualResult = funWithHolidaysWithWeekends.round(number, scale);
		assertEquals(actualResult, expectedRoundResult);
	}

	@Test(dataProvider = "round")
	public void checkRoundUp(String number, int scale, String expectedRoundResult, String expectedRoundUpResult,
	                         String expectedRoundDownResult)
	{
		String actualResult = funWithHolidaysWithWeekends.roundUp(number, scale);
		assertEquals(actualResult, expectedRoundUpResult);
	}

	@Test(dataProvider = "round")
	public void checkRoundDown(String number, int scale, String expectedRoundResult, String expectedRoundUpResult,
	                           String expectedRoundDownResult)
	{
		String actualResult = funWithHolidaysWithWeekends.roundDown(number, scale);
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
						//Tests for case when prevValue-file doesn't exist
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
				actualResult = funWithHolidaysWithWeekends.generate(pattern);
				assertEquals(actualResult, expectedResult);
			}
		}
		else
		{
			for (String expectedResult : expectedResults)
			{
				actualResult = funWithHolidaysWithWeekends.generate(generatorId, pattern);
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
		String actualResult = funWithHolidaysWithWeekends.format(millis, formatStr);
		assertEquals(actualResult, expectedFormatResult);
	}

	@Test(dataProvider = "date-format")
	public void checkReformat(String formatStr, String expectedFormatResult, String reformatStr,
	                          String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws FunctionException
	{
		String actualResult = funWithHolidaysWithWeekends.reformat(expectedFormatResult, formatStr, reformatStr);
		assertEquals(actualResult, expectedReformatResult);
	}

	@Test(dataProvider = "date-format")
	public void checkParseDate(String formatStr, String expectedFormatResult, String reformatStr,
	                           String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws FunctionException, ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		long actualResult = funWithHolidaysWithWeekends.parseDate(expectedFormatResult, formatStr);
		assertEquals(actualResult, millis);
	}


	@Test(dataProvider = "date-format")
	public void checkDate8(String formatStr, String expectedFormatResult, String reformatStr,
	                       String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		String actualResult = funWithHolidaysWithWeekends.date8(millis);
		assertEquals(actualResult, expectedDate8Result);
	}

	@Test(dataProvider = "date-format")
	public void checkTime6(String formatStr, String expectedFormatResult, String reformatStr,
	                       String expectedReformatResult, String expectedDate8Result, String expectedTime6Result)
			throws ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		String actualResult = funWithHolidaysWithWeekends.time6(millis);
		assertEquals(actualResult, expectedTime6Result);
	}

	@Test(dataProvider = "date-format")
	public void checkMilliseconds(String formatStr, String expectedFormatResult, String reformatStr,
	                              String expectedReformatResult, String expectedDate8Result,
	                              String expectedTime6Result)
			throws FunctionException, ParseException
	{
		long millis = parseDateAndTimeToMillis(expectedFormatResult, formatStr);
		String actualResult = funWithHolidaysWithWeekends.milliseconds(expectedFormatResult, formatStr);
		String expectedResult = String.valueOf(millis);
		assertEquals(actualResult, expectedResult);
	}

	@DataProvider(name = "end-of-month")
	Object[][] createDataForEndOfMonth()
	{
		return new Object[][]
				{
						// Functions to test, date, expected end of month
						{funWithHolidaysWithWeekends, "2020/06/03", "2020/06/30"},
						{funWithHolidaysWithoutWeekends, "2020/06/03", "2020/06/30"},
						{funWithoutHolidaysWithWeekends, "2020/06/03", "2020/06/30"},
						{funWithoutHolidaysWithoutWeekends, "2020/06/03", "2020/06/30"},

						{funWithHolidaysWithWeekends, "2019/05/29", "2019/05/30"},
						{funWithHolidaysWithoutWeekends, "2019/05/29", "2019/05/30"},
						{funWithoutHolidaysWithWeekends, "2019/05/29", "2019/05/31"},
						{funWithoutHolidaysWithoutWeekends, "2019/05/29", "2019/05/31"},

						{funWithHolidaysWithWeekends, "2020/05/01", "2020/05/29"},
						{funWithHolidaysWithoutWeekends, "2020/05/01", "2020/05/31"},
						{funWithoutHolidaysWithWeekends, "2020/05/01", "2020/05/29"},
						{funWithoutHolidaysWithoutWeekends, "2020/05/01", "2020/05/31"},
				};
	}

	@Test(dataProvider = "end-of-month")
	public void checkEndOfMonth(MatrixFunctions mf, String date, String expectedResult)
			throws FunctionException, ParseException
	{
		long dateInMillis = parseDateAndTimeToMillis(date, "yyyy/MM/dd");
		long actualLong;
		actualLong = mf.endOfMonth(dateInMillis);
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		String actualDate = format.format(actualLong);
		assertEquals(actualDate, expectedResult);
	}

	@DataProvider(name = "get-time")
	Object[][] createDataForGetTime()
	{
		return new Object[][]
				{
						//Functions to test, date to set, days, months, years, hours offset, expected result
						{funWithHolidaysWithWeekends, "2019/04/30 02:28:15", 0, 0, 0, 0, "2019/04/30 02:28:15"},
						{funWithHolidaysWithoutWeekends, "2019/04/30 02:28:15", 0, 0, 0, 0, "2019/04/30 02:28:15"},
						{funWithoutHolidaysWithWeekends, "2019/04/30 02:28:15", 0, 0, 0, 0, "2019/04/30 02:28:15"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/30 02:28:15", 0, 0, 0, 0, "2019/04/30 02:28:15"},

						{funWithHolidaysWithWeekends, "2019/04/30 02:28:15", 1, 0, 0, 0, "2019/05/06 02:28:15"},
						{funWithHolidaysWithoutWeekends, "2019/04/30 02:28:15", 1, 0, 0, 0, "2019/05/04 02:28:15"},
						{funWithoutHolidaysWithWeekends, "2019/04/30 02:28:15", 1, 0, 0, 0, "2019/05/01 02:28:15"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/30 02:28:15", 1, 0, 0, 0, "2019/05/01 02:28:15"},

						{funWithHolidaysWithWeekends, "2019/04/30 02:28:15", 2, 3, 0, 0, "2019/08/01 02:28:15"},
						{funWithHolidaysWithoutWeekends, "2019/04/30 02:28:15", 2, 3, 0, 0, "2019/08/01 02:28:15"},
						{funWithoutHolidaysWithWeekends, "2019/04/30 02:28:15", 2, 3, 0, 0, "2019/08/01 02:28:15"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/30 02:28:15", 2, 3, 0, 0, "2019/08/01 02:28:15"},

						{funWithHolidaysWithWeekends, "2019/04/30 02:28:15", 4, 5, 1, 0, "2020/10/06 02:28:15"},
						{funWithHolidaysWithoutWeekends, "2019/04/30 02:28:15", 4, 5, 1, 0, "2020/10/04 02:28:15"},
						{funWithoutHolidaysWithWeekends, "2019/04/30 02:28:15", 4, 5, 1, 0, "2020/10/06 02:28:15"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/30 02:28:15", 4, 5, 1, 0, "2020/10/04 02:28:15"},

						{funWithHolidaysWithWeekends, "2019/05/21 02:28:15", 5, 2, 1, 1, "2020/07/28 03:28:15"},
						{funWithHolidaysWithoutWeekends, "2019/05/21 02:28:15", 5, 2, 1, 1, "2020/07/26 03:28:15"},
						{funWithoutHolidaysWithWeekends, "2019/05/21 02:28:15", 5, 2, 1, 1, "2020/07/28 03:28:15"},
						{funWithoutHolidaysWithoutWeekends, "2019/05/21 02:28:15", 5, 2, 1, 1, "2020/07/26 03:28:15"},
				};
	}

	@Test(dataProvider = "get-time")
	public void checkGetTime(MatrixFunctions mf, String dateToSet, int days, int months, int years, int hours,
	                         String expectedDate) throws FunctionException, ParseException
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		Date date = sdf.parse(dateToSet);
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		long timestamp = mf.getTime(calendar, days, months, years, hours);

		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		String actualDate = format.format(timestamp);
		assertEquals(actualDate, expectedDate);
	}

	@DataProvider(name = "time")
	Object[][] createDataForTime()
	{
		return new Object[][]
				{
						// Functions to test
						// days, months, years, hours offset
						// sysTime (or time) with day offset
						// sysTime (or time) with day and month offset
						// sysTime (or time) with day, month and year offset
						// sysTime (or time) with day, month, year and hour offset
						/****  SysTime set to mocked calendar in checkSysTime methods is 30-04-2019 02:28:15  ****/

						{funWithHolidaysWithWeekends, 0, 0, 0, 0, "2019/04/30 02:28:15", "2019/04/30 02:28:15",
								"2019/04/30 02:28:15", "2019/04/30 02:28:15"},
						{funWithHolidaysWithoutWeekends, 0, 0, 0, 0, "2019/04/30 02:28:15", "2019/04/30 02:28:15",
								"2019/04/30 02:28:15", "2019/04/30 02:28:15"},
						{funWithoutHolidaysWithWeekends, 0, 0, 0, 0, "2019/04/30 02:28:15", "2019/04/30 02:28:15",
								"2019/04/30 02:28:15", "2019/04/30 02:28:15"},
						{funWithoutHolidaysWithoutWeekends, 0, 0, 0, 0, "2019/04/30 02:28:15", "2019/04/30 02:28:15",
								"2019/04/30 02:28:15", "2019/04/30 02:28:15"},

						{funWithHolidaysWithWeekends, 1, 4, 1, 3, "2019/05/06 02:28:15", "2019/09/02 02:28:15",
										"2020/08/31 02:28:15", "2020/08/31 05:28:15"},
						{funWithHolidaysWithoutWeekends, 1, 4, 1, 3, "2019/05/04 02:28:15", "2019/08/31 02:28:15",
								"2020/08/31 02:28:15", "2020/08/31 05:28:15"},
						{funWithoutHolidaysWithWeekends, 1, 4, 1, 3, "2019/05/01 02:28:15", "2019/09/02 02:28:15",
								"2020/08/31 02:28:15", "2020/08/31 05:28:15"},
						{funWithoutHolidaysWithoutWeekends, 1, 4, 1, 3, "2019/05/01 02:28:15", "2019/08/31 02:28:15",
								"2020/08/31 02:28:15", "2020/08/31 05:28:15"},

						{funWithHolidaysWithWeekends, 2, 3, 2, 12, "2019/05/08 02:28:15", "2019/08/01 02:28:15",
								"2021/08/03 02:28:15", "2021/08/03 02:28:15"},
						{funWithHolidaysWithoutWeekends, 2, 3, 2, 12, "2019/05/05 02:28:15", "2019/08/01 02:28:15",
								"2021/08/01 02:28:15", "2021/08/01 02:28:15"},
						{funWithoutHolidaysWithWeekends, 2, 3, 2, 12, "2019/05/02 02:28:15", "2019/08/01 02:28:15",
								"2021/08/03 02:28:15", "2021/08/03 02:28:15"},
						{funWithoutHolidaysWithoutWeekends, 2, 3, 2, 12, "2019/05/02 02:28:15", "2019/08/01 02:28:15",
								"2021/08/01 02:28:15", "2021/08/01 02:28:15"},

						{funWithHolidaysWithWeekends, -1, 1, -1, 20, "2019/04/29 02:28:15", "2019/05/29 02:28:15",
								"2018/05/29 02:28:15", "2018/05/29 10:28:15"},
						{funWithHolidaysWithoutWeekends, -1, 1, -1, 20, "2019/04/29 02:28:15", "2019/05/29 02:28:15",
								"2018/05/29 02:28:15", "2018/05/29 10:28:15"},
						{funWithoutHolidaysWithWeekends,-1, 1, -1, 20, "2019/04/29 02:28:15", "2019/05/29 02:28:15",
								"2018/05/29 02:28:15", "2018/05/29 10:28:15"},
						{funWithoutHolidaysWithoutWeekends, -1, 1, -1, 20, "2019/04/29 02:28:15", "2019/05/29 02:28:15",
								"2018/05/29 02:28:15", "2018/05/29 10:28:15"},
				};
	}

	private void sysTimeTest(FunctionWithException<MatrixFunctions, Long, FunctionException> methodToTest,
	                        MatrixFunctions mf,
	                        String expected)
			throws FunctionException
	{
		MatrixFunctions functionsWithOwnCalendar = spy(mf);
		Calendar calendar = new GregorianCalendar(2019, Calendar.APRIL, 30, 2, 28, 15);
		when(functionsWithOwnCalendar.getCalendar()).thenAnswer(i -> calendar.clone());

		long actualInMillis = methodToTest.apply(functionsWithOwnCalendar);
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		String actualWithDays = format.format(actualInMillis);
		assertEquals(actualWithDays, expected);
	}


	@Test(dataProvider = "time")
	public void checkSysTimeWithDays(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                 String expectedWithDays,
	                                 String expectedWithDaysMonths,
	                                 String expectedWithDaysMonthsYears,
	                                 String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		sysTimeTest((functions) -> functions.sysTime(dayOffset), mf, expectedWithDays);
	}

	@Test(dataProvider = "time")
	public void checkSysTimeWithDaysMonths(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                       String expectedWithDays,
	                                       String expectedWithDaysMonths,
	                                       String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		sysTimeTest((functions) -> functions.sysTime(dayOffset, monthOffset), mf, expectedWithDaysMonths);
	}

	@Test(dataProvider = "time")
	public void checkSysTimeWithDaysMonthsYears(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                            String expectedWithDays,
	                                            String expectedWithDaysMonths,
	                                            String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		sysTimeTest((functions) -> functions.sysTime(dayOffset, monthOffset, yearOffset), mf, expectedWithDaysMonthsYears);
	}

	@Test(dataProvider = "time")
	public void checkSysTimeWithDaysMonthsYearsHours(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                                 String expectedWithDays,
	                                                 String expectedWithDaysMonths,
	                                                 String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		sysTimeTest((functions) -> functions.sysTime(dayOffset, monthOffset, yearOffset, hourOffset), mf,
				expectedWithDaysMonthsYearsHours);
	}

	private void timeTest(FunctionWithException<MatrixFunctions, Long, FunctionException> methodToTest,
	                         MatrixFunctions mf,
	                         String expected)
			throws FunctionException
	{
		Calendar calendar = new GregorianCalendar(2019, Calendar.APRIL, 30, 2, 28, 15);
		MatrixFunctions functionsWithOwnCalendar = new MatrixFunctions(mf.holidays,
				calendar.getTime(),
				null,
				mf.weekendHoliday,
				null);
		functionsWithOwnCalendar.setCurrentTime(calendar);
		long timestamp = methodToTest.apply(functionsWithOwnCalendar);

		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		String actualDate = format.format(timestamp);
		assertEquals(actualDate, expected);
	}


	@Test(dataProvider = "time")
	public void checkTimeWithDays(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                              String expectedWithDays,
	                              String expectedWithDaysMonths,
	                              String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		timeTest((functions) -> functions.time(dayOffset), mf, expectedWithDays);
	}

	@Test(dataProvider = "time")
	public void checkTimeWithDaysMonths(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                    String expectedWithDays,
	                                    String expectedWithDaysMonths,
	                                    String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		timeTest((functions) -> functions.time(dayOffset, monthOffset), mf, expectedWithDaysMonths);
	}

	@Test(dataProvider = "time")
	public void checkTimeWithDaysMonthsYears(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                         String expectedWithDays,
	                                         String expectedWithDaysMonths,
	                                         String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		timeTest((functions) -> functions.time(dayOffset, monthOffset, yearOffset), mf, expectedWithDaysMonthsYears);
	}

	@Test(dataProvider = "time")
	public void checkTimeWithDaysMonthsYearsHours(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                              String expectedWithDays,
	                                              String expectedWithDaysMonths,
	                                              String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		timeTest((functions) -> functions.time(dayOffset, monthOffset, yearOffset, hourOffset), mf,
				expectedWithDaysMonthsYearsHours);
	}

	@DataProvider(name = "time-without-business-day")
	Object[][] createDataForTimeWithoutBusinessDay()
	{
		return new Object[][]
				{
						// This method checks time method for MF without business day set in constructor
						// Days, months, years, hours offset
						// time with day offset
						// time with day and month offset
						// time with day, month and year offset
						// time with day, month, year and hour offset
						{0, 0, 0, 0, "2019/04/30 02:28:15", "2019/04/30 02:28:15",
								"2019/04/30 02:28:15", "2019/04/30 02:28:15"},
						{1, 4, 1, 3, "2019/05/01 02:28:15", "2019/09/02 02:28:15",
								"2020/08/31 02:28:15", "2020/08/31 05:28:15"},
						{2, 3, 2, 12, "2019/05/02 02:28:15", "2019/08/01 02:28:15",
								"2021/08/03 02:28:15", "2021/08/03 02:28:15"},
						{-1, 1, -1, 20, "2019/04/29 02:28:15", "2019/05/29 02:28:15",
								"2018/05/29 02:28:15", "2018/05/29 10:28:15"},
				};
	}

	private void timeWithoutBusinessDayTest(FunctionWithException<MatrixFunctions, Long, FunctionException> methodToTest,
	                      String expected)
			throws FunctionException
	{
		Calendar calendar = new GregorianCalendar(2019, Calendar.APRIL, 30, 2, 28, 15);
		MatrixFunctions mf = new MatrixFunctions(null, null, null,
				true, null);
		MatrixFunctions functionsWithOwnCalendar = spy(mf);
		when(functionsWithOwnCalendar.getCalendar()).thenAnswer(i -> calendar.clone());
		long timestamp = methodToTest.apply(functionsWithOwnCalendar);

		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		String actualDate = format.format(timestamp);
		assertEquals(actualDate, expected);
	}


	@Test(dataProvider = "time-without-business-day")
	public void checkTimeWithoutBusinessDayDays(int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                        String expectedWithDays,
	                                        String expectedWithDaysMonths,
	                                        String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		timeWithoutBusinessDayTest((functions) -> functions.time(dayOffset), expectedWithDays);
	}

	@Test(dataProvider = "time-without-business-day")
	public void checkTimeWithoutBusinessDayDaysMonths(int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                        String expectedWithDays,
	                                        String expectedWithDaysMonths,
	                                        String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		timeWithoutBusinessDayTest((functions) -> functions.time(dayOffset, monthOffset), expectedWithDaysMonths);
	}

	@Test(dataProvider = "time-without-business-day")
	public void checkTimeWithoutBusinessDayMonthsYears(int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                              String expectedWithDays,
	                                              String expectedWithDaysMonths,
	                                              String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		timeWithoutBusinessDayTest((functions) -> functions.time(dayOffset, monthOffset, yearOffset),
				expectedWithDaysMonthsYears);
	}

	@Test(dataProvider = "time-without-business-day")
	public void checkTimeWithoutBusinessDayDaysMonthsYearsHours(int dayOffset, int monthOffset, int yearOffset,
	                                                      int hourOffset,
	                                        String expectedWithDays,
	                                        String expectedWithDaysMonths,
	                                        String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException
	{
		timeWithoutBusinessDayTest((functions) -> functions.time(dayOffset, monthOffset, yearOffset, hourOffset),
				expectedWithDaysMonthsYearsHours);
	}

	@DataProvider(name = "time-BT")
	Object[][] createDataForTimeBT()
	{
		return new Object[][]
				{
						// Functions to test
						// days, months, years, hours offset
						// date to emulate "now"
						// time with day offset
						// time with day and month offset
						// time with day, month and year offset
						// time with day, month, year and hour offset

						{funWithHolidaysWithWeekendsBaseTime, 0, 0, 0, 0, "2019/05/01 02:00:00",
								"2019/04/30 04:00:00", "2019/04/30 04:00:00", "2019/04/30 04:00:00",
								"2019/04/30 04:00:00"},
						{funWithHolidaysWithoutWeekendsBaseTime, 0, 0, 0, 0, "2019/05/01 02:00:00",
								"2019/04/30 04:00:00", "2019/04/30 04:00:00", "2019/04/30 04:00:00",
								"2019/04/30 04:00:00"},
						{funWithoutHolidaysWithWeekendsBaseTime, 0, 0, 0, 0, "2019/05/01 02:00:00",
								"2019/04/30 04:00:00", "2019/04/30 04:00:00", "2019/04/30 04:00:00",
								"2019/04/30 04:00:00"},
						{funWithoutHolidaysWithoutWeekendsBaseTime, 0, 0, 0, 0, "2019/05/01 02:00:00",
								"2019/04/30 04:00:00", "2019/04/30 04:00:00", "2019/04/30 04:00:00",
								"2019/04/30 04:00:00"},

						{funWithHolidaysWithWeekendsBaseTime, 2, 3, 2, 12, "2019/05/01 02:00:00",
								"2019/05/08 04:00:00", "2019/08/01 04:00:00", "2021/08/03 04:00:00",
								"2021/08/03 04:00:00"},
						{funWithHolidaysWithoutWeekendsBaseTime, 2, 3, 2, 12, "2019/05/01 12:00:00",
								"2019/05/05 02:00:00", "2019/08/01 02:00:00", "2021/08/01 02:00:00",
								"2021/08/02 02:00:00"},
						{funWithoutHolidaysWithWeekendsBaseTime, 2, 3, 2, 12, "2019/05/01 12:00:00",
								"2019/05/02 02:00:00", "2019/08/01 02:00:00", "2021/08/03 02:00:00",
								"2021/08/03 02:00:00"},
						{funWithoutHolidaysWithoutWeekendsBaseTime, 2, 3, 2, 12, "2019/05/01 12:00:00",
								"2019/05/02 02:00:00", "2019/08/01 02:00:00", "2021/08/01 02:00:00",
								"2021/08/02 02:00:00"},

						{funWithHolidaysWithWeekendsBaseTime, -1, 1, -1, 20, "2019/05/01 05:00:00",
								"2019/04/29 07:00:00", "2019/05/29 07:00:00", "2018/05/29 07:00:00",
								"2018/05/30 03:00:00"},
						{funWithHolidaysWithoutWeekendsBaseTime, -1, 1, -1, 20, "2019/05/01 05:00:00",
								"2019/04/29 07:00:00", "2019/05/29 07:00:00", "2018/05/29 07:00:00",
								"2018/05/30 03:00:00"},
						{funWithoutHolidaysWithWeekendsBaseTime, -1, 1, -1, 20, "2019/05/01 05:00:00",
								"2019/04/29 07:00:00", "2019/05/29 07:00:00", "2018/05/29 07:00:00",
								"2018/05/30 03:00:00"},
						{funWithoutHolidaysWithoutWeekendsBaseTime, -1, 1, -1, 20, "2019/05/01 05:00:00",
								"2019/04/29 07:00:00", "2019/05/29 07:00:00", "2018/05/29 07:00:00",
								"2018/05/30 03:00:00"},

						{funWithHolidaysWithWeekendsBaseTime, 5, 0, -3, 13, "2019/05/01 11:00:00",
								"2019/05/15 01:00:00", "2019/05/15 01:00:00", "2016/05/06 01:00:00",
								"2016/05/06 02:00:00"},
						{funWithHolidaysWithoutWeekendsBaseTime, 5, 0, -3, 13, "2019/05/01 11:00:00",
								"2019/05/11 01:00:00", "2019/05/11 01:00:00", "2016/05/05 01:00:00",
								"2016/05/05 02:00:00"},
						{funWithoutHolidaysWithWeekendsBaseTime, 5, 0, -3, 13, "2019/05/01 11:00:00",
								"2019/05/07 01:00:00", "2019/05/07 01:00:00", "2016/05/06 01:00:00",
								"2016/05/06 02:00:00"},
						{funWithoutHolidaysWithoutWeekendsBaseTime, 5, 0, -3, 13, "2019/05/01 11:00:00",
								"2019/05/05 01:00:00", "2019/05/05 01:00:00", "2016/05/05 01:00:00",
								"2016/05/05 02:00:00"},
				};
	}

	private void timeBtTest(FunctionWithException<MatrixFunctions, Long, FunctionException> methodToTest,
	                        MatrixFunctions mf,
	                        String now,
	                        String expected)
			throws FunctionException, ParseException
	{
		MatrixFunctions functionsWithOwnCalendar = spy(mf);
		Calendar calendar = new GregorianCalendar(2019, Calendar.APRIL, 30, 10, 0, 0);

		when(functionsWithOwnCalendar.getCalendar()).thenAnswer(i -> calendar.clone());
		Date baseTime = new GregorianCalendar(2019, Calendar.APRIL, 30, 0, 0, 0).getTime();
		functionsWithOwnCalendar.setBaseTime(baseTime);

		long nowInMillis = parseDateAndTimeToMillis(now, "yyyy/MM/dd hh:mm:ss");
		calendar.setTimeInMillis(nowInMillis);
		when(functionsWithOwnCalendar.getCalendar()).thenAnswer(i -> calendar.clone());

		long timestamp = methodToTest.apply(functionsWithOwnCalendar);

		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		String actualDate = format.format(timestamp);
		assertEquals(actualDate, expected);
	}


	@Test(dataProvider = "time-BT")
	public void checkTimeWithDaysBT(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                String now,
	                                String expectedWithDays,
	                                String expectedWithDaysMonths,
	                                String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException, ParseException
	{
		timeBtTest((functions) -> functions.time(dayOffset), mf, now, expectedWithDays);
	}

	@Test(dataProvider = "time-BT")
	public void checkTimeWithDaysMonthsBT(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                      String now,
	                                      String expectedWithDays,
	                                      String expectedWithDaysMonths,
	                                      String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException, ParseException
	{
		timeBtTest((functions) -> functions.time(dayOffset, monthOffset), mf, now, expectedWithDaysMonths);
	}

	@Test(dataProvider = "time-BT")
	public void checkTimeWithDaysMonthsYearsBT(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                           String now,
	                                           String expectedWithDays,
	                                           String expectedWithDaysMonths,
	                                           String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException, ParseException
	{
		timeBtTest((functions) -> functions.time(dayOffset, monthOffset, yearOffset), mf, now, expectedWithDaysMonthsYears);
	}

	@Test(dataProvider = "time-BT")
	public void checkTimeWithDaysMonthsYearsHoursBT(MatrixFunctions mf, int dayOffset, int monthOffset, int yearOffset, int hourOffset,
	                                                String now,
	                                                String expectedWithDays,
	                                                String expectedWithDaysMonths,
	                                                String expectedWithDaysMonthsYears, String expectedWithDaysMonthsYearsHours)
			throws FunctionException, ParseException
	{
		timeBtTest((functions) -> functions.time(dayOffset, monthOffset, yearOffset, hourOffset), mf, now,
				expectedWithDaysMonthsYearsHours);
	}

	@DataProvider(name = "time-including-holidays")
	Object[][] createDataForTimeIncludingHolidays()
	{
		return new Object[][]
				{
						// Functions to test, date to set, day offset, expected date with offset
						{funWithHolidaysWithWeekends, "2019/04/30", 0, "2019/04/30"},
						{funWithHolidaysWithoutWeekends, "2019/04/30", 0, "2019/04/30"},
						{funWithoutHolidaysWithWeekends, "2019/04/30", 0, "2019/04/30"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/30", 0, "2019/04/30"},

						{funWithHolidaysWithWeekends, "2019/04/30", 1, "2019/05/06"},
						{funWithHolidaysWithoutWeekends, "2019/04/30", 1, "2019/05/06"},
						{funWithoutHolidaysWithWeekends, "2019/04/30", 1, "2019/05/01"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/30", 1, "2019/05/01"},

						{funWithHolidaysWithWeekends, "2019/04/30", 4, "2019/05/14"},
						{funWithHolidaysWithoutWeekends, "2019/04/30", 4, "2019/05/14"},
						{funWithoutHolidaysWithWeekends, "2019/04/30", 4, "2019/05/06"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/30", 4, "2019/05/06"},

						{funWithHolidaysWithWeekends, "2019/05/18", 2, "2019/05/20"},
						{funWithHolidaysWithoutWeekends, "2019/05/18", 2, "2019/05/20"},
						{funWithoutHolidaysWithWeekends, "2019/05/18", 2, "2019/05/21"},
						{funWithoutHolidaysWithoutWeekends, "2019/05/18", 2, "2019/05/21"},

						{funWithHolidaysWithWeekendsBaseTime, "2019/04/30", 0, "2019/04/30"},
						{funWithHolidaysWithoutWeekendsBaseTime, "2019/04/30", 0, "2019/04/30"},
						{funWithoutHolidaysWithWeekendsBaseTime, "2019/04/30", 0, "2019/04/30"},
						{funWithoutHolidaysWithoutWeekendsBaseTime, "2019/04/30", 0, "2019/04/30"},

						{funWithHolidaysWithWeekendsBaseTime, "2019/04/30", 4, "2019/05/14"},
						{funWithHolidaysWithoutWeekendsBaseTime, "2019/04/30", 4, "2019/05/14"},
						{funWithoutHolidaysWithWeekendsBaseTime, "2019/04/30", 4, "2019/05/06"},
						{funWithoutHolidaysWithoutWeekendsBaseTime, "2019/04/30", 4, "2019/05/06"}
				};
	}

	@Test(dataProvider = "time-including-holidays")
	public void checkTimeIncludingHolidays(MatrixFunctions  mf, String dateToSet, int dayOffset, String expectedDate)
			throws ParseException, FunctionException
	{
		long dateLong = parseDateAndTimeToMillis(dateToSet, "yyyy/MM/dd");
		long actualLong = mf.timeIncludingHolidays(dateLong, dayOffset);

		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		String actualDate = format.format(actualLong);
		assertEquals(actualDate, expectedDate);
	}

	@DataProvider(name = "holiday")
	Object[][] createDataForHoliday()
	{
		return new Object[][]
				{
						// Functions to test
						// date to set
						// expected holiday without offset
						// day offset, expected holiday with offset
						// holiday type, expected holiday with type
						// expected holiday with type and day offset
						{funWithHolidaysWithWeekends, "2019/04/22", "2019/04/27", 3, "2019/05/05", "holiday", "2019/05/01", "2019/05/07"},
						{funWithHolidaysWithoutWeekends, "2019/04/22", "2019/04/27", 3, "2019/05/05", "holiday", "2019/05/01", "2019/05/07"},

						{funWithHolidaysWithWeekends, "2019/04/29", "2019/05/04", 4, "2019/05/18", "weekend", "2019/05/04", "2019/05/18"},
						{funWithHolidaysWithoutWeekends, "2019/04/29", "2019/05/04", 4, "2019/05/18", "weekend", "2019/05/04", "2019/05/18"},
						{funWithoutHolidaysWithWeekends, "2019/04/29", "2019/05/04", 4, "2019/05/18", "weekend", "2019/05/04", "2019/05/18"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/29", "2019/05/04", 4, "2019/05/18", "weekend", "2019/05/04", "2019/05/18"},

						{funWithHolidaysWithWeekends, "2019/04/29", "2019/05/04", 4, "2019/05/18", "any", "2019/05/01", "2019/05/05"},
						{funWithHolidaysWithoutWeekends, "2019/04/29", "2019/05/04", 4, "2019/05/18", "any", "2019/05/01", "2019/05/05"},
						{funWithoutHolidaysWithWeekends, "2019/04/29", "2019/05/04", 4, "2019/05/18", "any", "2019/05/04", "2019/05/18"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/29", "2019/05/04", 4, "2019/05/18", "any", "2019/05/04", "2019/05/18"},

						{funWithHolidaysWithWeekends, "2019/05/04", "2019/05/04", 1, "2019/05/05", "holiday", "2019/05/07", "2019/05/09"},
						{funWithHolidaysWithoutWeekends, "2019/05/04", "2019/05/04", 1, "2019/05/05", "holiday", "2019/05/07", "2019/05/09"},

						{funWithHolidaysWithWeekends, "2019/05/24", "2019/05/25", 2, "2019/06/01", "any", "2019/05/25", "2019/05/31"},
						{funWithHolidaysWithoutWeekends, "2019/05/24", "2019/05/25", 2, "2019/06/01", "any", "2019/05/25", "2019/05/31"},
						{funWithoutHolidaysWithWeekends, "2019/05/24", "2019/05/25", 2, "2019/06/01", "any", "2019/05/25", "2019/06/01"},
						{funWithoutHolidaysWithoutWeekends, "2019/05/24", "2019/05/25", 2, "2019/06/01", "any", "2019/05/25", "2019/06/01"},
				};
	}

	private void holidayTest(FunctionWithException<MatrixFunctions, Long, FunctionException> methodToTest,
	                        MatrixFunctions mf,
	                        String expected)
			throws FunctionException, ParseException
	{
		long actualLong = methodToTest.apply(mf);

		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		String actualDate = format.format(actualLong);
		assertEquals(actualDate, expected);
	}

	@Test(dataProvider = "holiday")
	public void checkHoliday(MatrixFunctions mf, String dateToSet, String holidayWithoutOffset, int dayOffset,
	                         String holidayWithDayOffset, String holidayType, String holidayWithType,
	                         String holidayWithTypeAndOffset)
			throws ParseException, FunctionException
	{
		long dateLong = parseDateAndTimeToMillis(dateToSet, "yyyy/MM/dd");
		holidayTest((functions) -> functions.holiday(dateLong), mf, holidayWithoutOffset);
	}

	@Test(dataProvider = "holiday")
	public void checkHolidayWithDayOffset(MatrixFunctions mf, String dateToSet, String holidayWithoutOffset, int dayOffset,
	                                      String holidayWithDayOffset, String holidayType, String holidayWithType,
	                                      String holidayWithTypeAndOffset)
			throws ParseException, FunctionException
	{
		long dateLong = parseDateAndTimeToMillis(dateToSet, "yyyy/MM/dd");
		holidayTest((functions) -> functions.holiday(dateLong, dayOffset), mf, holidayWithDayOffset);
	}

	@Test(dataProvider = "holiday")
	public void checkHolidayWithType(MatrixFunctions mf, String dateToSet, String holidayWithoutOffset, int dayOffset,
	                                 String holidayWithDayOffset, String holidayType, String holidayWithType,
	                                 String holidayWithTypeAndOffset)
			throws ParseException, FunctionException
	{
		long dateLong = parseDateAndTimeToMillis(dateToSet, "yyyy/MM/dd");
		holidayTest((functions) -> functions.holiday(dateLong, holidayType), mf, holidayWithType);
	}

	@Test(dataProvider = "holiday")
	public void checkHolidayWithTypeAndOffset(MatrixFunctions mf, String dateToSet, String holidayWithoutOffset, int dayOffset,
	                                          String holidayWithDayOffset, String holidayType, String holidayWithType,
	                                          String holidayWithTypeAndOffset)
			throws ParseException, FunctionException
	{
		long dateLong = parseDateAndTimeToMillis(dateToSet, "yyyy/MM/dd");
		holidayTest((functions) -> functions.holiday(dateLong, holidayType, dayOffset), mf, holidayWithTypeAndOffset);
	}

	@DataProvider(name = "holiday-exception")
	Object[][] createDataForHolidayException()
	{
		return new Object[][]
				{
						// Used to test exceptions in case when holidays are not specified in MF constructor
						// matrix functions
						// date to set
						// day offset
						// holiday type
						{funWithoutHolidaysWithWeekends, "2019/04/22", 3, "holiday"},
						{funWithoutHolidaysWithoutWeekends, "2019/04/22", 3, "holiday"},
				};
	}


	@Test(dataProvider = "holiday-exception", expectedExceptions = FunctionException.class,
			expectedExceptionsMessageRegExp = EXCEPTION_MESSAGE_REGEX)
	public void checkHolidayWithTypeEx(MatrixFunctions mf, String dateToSet, int dayOffset, String holidayType)
			throws ParseException, FunctionException
	{
		long dateLong = parseDateAndTimeToMillis(dateToSet, "yyyy/MM/dd");
		mf.holiday(dateLong, holidayType);
	}

	@Test(dataProvider = "holiday-exception", expectedExceptions = FunctionException.class,
			expectedExceptionsMessageRegExp = EXCEPTION_MESSAGE_REGEX)
	public void checkHolidayWithTypeAndOffsetEx(MatrixFunctions mf, String dateToSet, int dayOffset, String holidayType)
			throws ParseException, FunctionException
	{
		long dateLong = parseDateAndTimeToMillis(dateToSet, "yyyy/MM/dd");
		mf.holiday(dateLong, holidayType, dayOffset);
	}

	@DataProvider(name = "date")
	Object[][] createDataForDate()
	{
		return new Object[][]
				{
						// Functions to test, date to set, offset, date with offset
						{funWithHolidaysWithWeekends, "20190422", 3, "20190425"},
						{funWithHolidaysWithoutWeekends, "20190422", 3, "20190425"},
						{funWithoutHolidaysWithWeekends, "20190422", 3, "20190425"},
						{funWithoutHolidaysWithoutWeekends, "20190422", 3, "20190425"},

						{funWithHolidaysWithWeekends, "20190429", 4, "20190513"},
						{funWithHolidaysWithoutWeekends, "20190429", 4, "20190506"},
						{funWithoutHolidaysWithWeekends, "20190429", 4, "20190503"},
						{funWithoutHolidaysWithoutWeekends, "20190429", 4, "20190503"},

						{funWithHolidaysWithWeekends, "20190504", 1, "20190506"},
						{funWithHolidaysWithoutWeekends, "20190504", 1, "20190505"},
						{funWithoutHolidaysWithWeekends, "20190504", 1, "20190506"},
						{funWithoutHolidaysWithoutWeekends, "20190504", 1, "20190505"},

						{funWithHolidaysWithWeekends, "20190517", 2, "20190520"},
						{funWithHolidaysWithoutWeekends, "20190517", 2, "20190519"},
						{funWithoutHolidaysWithWeekends, "20190517", 2, "20190521"},
						{funWithoutHolidaysWithoutWeekends, "20190517", 2, "20190519"},
				};
	}

	@Test(dataProvider = "date")
	public void checkDate(MatrixFunctions mf, String dateToSet, int offset, String dateWithOffset)
			throws ParseException,
			FunctionException
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date date = sdf.parse(dateToSet);
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		MatrixFunctions functionsWithOwnCalendar = spy(mf);
		when(functionsWithOwnCalendar.getCalendar()).thenAnswer(i -> calendar.clone());

		String actualResult = functionsWithOwnCalendar.date(offset);
		assertEquals(actualResult, dateWithOffset);
	}


	@DataProvider(name = "calculate-expression")
	Object[][] createDataForCalculateExpression()
	{

		Calendar calendar = new GregorianCalendar(2020, Calendar.APRIL, 15);
		Map<String, String> actionParams = new HashMap<>();
		actionParams.put("param1", "1");
		actionParams.put("param2", "2");
		actionParams.put("param3", "3");
		actionParams.put("param4", "4");
		actionParams.put("param5", "5");

		Map<String, Object> mvelVars = new HashMap<>();
		mvelVars.put("params", actionParams);
		mvelVars.put("a", 1);
		mvelVars.put("b", 2);
		mvelVars.put("c", 3);
		mvelVars.put("calendar", calendar);
		mvelVars.put("days", 1);
		mvelVars.put("months", 1);
		mvelVars.put("years", 1);
		mvelVars.put("hours", 0);

		Map<String, String> fixedIDs = new HashMap<>();
		fixedIDs.put("123", "params");
		fixedIDs.put("234", "params");
		fixedIDs.put("345", "params");

		return new Object[][]
				{
						// Expression, param name, mvelVars, fixed IDs, current action, expected result
						{"@{mul(a,b+c)}", null, mvelVars, fixedIDs, null, "5"},
						{"@{123.param1 + a}", null, mvelVars, fixedIDs, null, "11"},
						{"@{mul(234.param2, 345.param5)}", null, mvelVars, fixedIDs, null, "10"},
						{"@{234.param3.equals('3')}", null, mvelVars, fixedIDs, null, true},
						{"@{add(asNumber('18'), asNumber('12.00', 0))}", null, mvelVars, fixedIDs, null,
								"30"},

						{"@{time6(time(days, months, years, hours))}", null, mvelVars, fixedIDs, null, "000000"},
						{"@{date8(time(days, months, years))}", null, mvelVars, fixedIDs, null, "20200601"},
						{"@{date8(time(days, months))}", null, mvelVars, fixedIDs, null, "20190603"},
						{"@{date8(time(days))}", null, mvelVars, fixedIDs, null, "20190506"},

						{"@{timeIncludingHolidays(1557093600000, 3)}", null, mvelVars, fixedIDs, null, 1557784800000L},

						// for GMT+0: 1557144000000L ->  05/06/2019 12:00:00
						// 1557576000000L -> 05/11/2019 12:00:00
						{"@{holiday(1557144000000)}", null, mvelVars, fixedIDs, null, 1557576000000L},
						// 1557230400000L -> 05/07/2019 12:00:00
						{"@{holiday(1557144000000, 'any')}", null, mvelVars, fixedIDs, null, 1557230400000L},
						// 1558267200000L -> 05/19/2019 12:00:00
						{"@{holiday(1557144000000, 3)}", null, mvelVars, fixedIDs, null, 1558267200000L},
						// 1557576000000L -> 05/11/2019 12:00:00
						{"@{holiday(1557144000000, 'any', 3)}", null, mvelVars, fixedIDs, null, 1557576000000L},

						{"@{date8(sysTime(days, months, years, hours))}", null, mvelVars, fixedIDs, null, "20210505"},
						{"@{date8(sysTime(days, months, years))}", null, mvelVars, fixedIDs, null, "20210505"},
						{"@{date8(sysTime(days, months))}", null, mvelVars, fixedIDs, null, "20200505"},
						{"@{date8(sysTime(days))}", null, mvelVars, fixedIDs, null, "20200406"},

						{"@{toBigDecimal(234.param3)}", null, mvelVars, fixedIDs, null, BigDecimal.valueOf(3)},
						{"@{toBigDecimal('10.0','.')}", null, mvelVars, fixedIDs, null, BigDecimal.valueOf(10.0)},
						{"@{toBigDecimal('10.0','.', ' ')}", null, mvelVars, fixedIDs, null, BigDecimal.valueOf(10.0)},

						{"@{mul(100.00, 200.00)}", null, mvelVars, null, null, "20000"},
						{"@{min(mul(round(5.1, 0), roundUp(10.3, 0)), max(avg(5.0, div(49.0, 7.0)), roundDown(3.7, " +
								"1)" +
								"))}", null, mvelVars, null, null, BigDecimal.valueOf(6.0)},
						{"@{min(mul(round(5.1, 0), roundUp(10.3, 0)), max(avg(5.0, div(49.0, asNumber(trimZeros('7" +
								".00, 1')))), roundDown(3.7, 1)))}",
								null, mvelVars, null, null, BigDecimal.valueOf(6.0)},
						{"@{mul(sub( asNumber(addZeros('10.0', 3)), 4), add(asNumber(addZeros('10.0',2,'.')), 10, 1)" +
								")}",
								null, mvelVars, null, null, "120"},
						{"@{toBigDecimal(trimleft('100.00',2) + trimright('200.00',2))}",
								null, mvelVars, null, null, BigDecimal.valueOf(1000)},
						{"@{reformat(format(parseDate('20-12-2018','dd-MM-yyyy'),'yyyy/MM/dd hh:mm:ss'),'yyyy/MM/dd " +
								"hh:mm:ss', 'dd/MM/yyyy" +
								" hh:mm:ss')}", null, mvelVars, null, null, "20/12/2018 12:00:00"},
						{"@{time6(milliseconds('01-11-2019-11-20','dd-MM-yyyy-hh-mm'))}",
								null, mvelVars, null, null, "112000"},
						{"@{date8(endOfMonth(timeIncludingHolidays(1591862233915L, 25)))}",
								null, mvelVars, null, null, "20200731"},
				};
	}

	@Test(dataProvider = "calculate-expression")
	public void checkCalculateExpression(String expression, String paramName, Map<String, Object> mvelVars,
	                                     Map<String, String> fixedIDs, Action currentAction,
	                                     Object expectedResult) throws Exception
	{
		Calendar calendar = new GregorianCalendar(2020, Calendar.APRIL, 4);
		MatrixFunctions functionsWithOwnCalendar = new MatrixFunctions(funWithHolidaysWithWeekends.holidays, funWithHolidaysWithWeekends.businessDay,
				null, true,	null);
		functionsWithOwnCalendar.setCurrentTime(calendar);
		MatrixFunctions mf = spy(functionsWithOwnCalendar);
		when(mf.getCalendar()).thenAnswer(i -> calendar.clone());

		ObjectWrapper iterationWrapper = new ObjectWrapper(0);
		Object actualResult = mf.calculateExpression(expression, paramName, mvelVars, fixedIDs,
				currentAction, iterationWrapper);
		assertEquals(actualResult, expectedResult);
	}


	@AfterMethod
	public void tearDown() throws IOException
	{
		FileUtils.cleanDirectory(MATRIX_FUNCTIONS_TEST_OUTPUT_DIR.toFile());
	}
}