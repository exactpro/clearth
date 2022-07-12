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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.functions.SpecialDataModel;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.automation.report.results.complex.ComparisonRow;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.exactprosystems.clearth.utils.ParametersUtils.*;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.*;

public class ComparisonUtils
{
	private static final Logger logger = LoggerFactory.getLogger(ComparisonUtils.class);

	public enum InfoIndication {
		NULL,
		NULL_OR_EMPTY
	}

	@SpecialDataModel(
			name = "isNull",
			usage = "@{isNull}",
			description = "Checks if the actual value doesn't exist"
	)
	public static final String IS_NULL = "@{isNull}";
	@SpecialDataModel(
			name = "isNotPresent",
			usage = "@{isNotPresent}",
			description = "Checks if the actual value doesn't exist"
	)
	public static final String IS_NOT_PRESENT = "@{isNotPresent}";
	@SpecialDataModel(
			name = "isNotSet",
			usage = "@{isNotSet}",
			description = "Checks if the actual value doesn't exist"
	)
	public static final String IS_NOT_SET = "@{isNotSet}";
	public static final Set<String> NULL_VALUES = new HashSet<String>() {{
		add(IS_NULL);
		add(IS_NOT_PRESENT);
		add(IS_NOT_SET);
	}};


	@SpecialDataModel(
			name = "isNotNull",
			usage = "@{isNotNull}",
			description = "Checks if the actual value exists"
	)
	public static final String IS_NOT_NULL = "@{isNotNull}";
	@SpecialDataModel(
			name = "isPresent",
			usage = "@{isPresent}",
			description = "Checks if the actual value exists"
	)
	public static final String IS_PRESENT = "@{isPresent}";
	@SpecialDataModel(
			name = "isSet",
			usage = "@{isSet}",
			description = "Checks if the actual value exists"
	)
	public static final String IS_SET = "@{isSet}";
	public static final Set<String> NOT_NULL_VALUES = new HashSet<String>() {{
		add(IS_NOT_NULL);
		add(IS_PRESENT);
		add(IS_SET);
	}};
	
	@SpecialDataModel(
			name = "isEmpty",
			usage = "@{isEmpty}",
			description = "Checks if the actual value is empty"
	)
	public static final String IS_EMPTY = "@{isEmpty}";
	
	
	@SpecialDataModel(
			name = "isNotEmpty",
			usage = "@{isNotEmpty}",
			description = "Checks if the actual value is not empty"
	)
	public static final String IS_NOT_EMPTY = "@{isNotEmpty}";
	
	@SpecialDataModel(
			name = "isNullOrEmpty",
			usage = "@{isNullOrEmpty}",
			description = "Checks if the actual value doesn't exist or is empty"
	)
	public static final String IS_NULL_OR_EMPTY = "@{isNullOrEmpty}";
	@SpecialDataModel(
			name = "isNotPresentOrEmpty",
			usage = "@{isNotPresentOrEmpty}",
			description = "Checks if the actual value doesn't exist or is empty"
	)
	public static final String IS_NOT_PRESENT_OR_EMPTY = "@{isNotPresentOrEmpty}";
	@SpecialDataModel(
			name = "isNotSetOrEmpty",
			usage = "@{isNotSetOrEmpty}",
			description = "Checks if the actual value doesn't exist or is empty"
	)
	public static final String IS_NOT_SET_OR_EMPTY = "@{isNotSetOrEmpty}";
	public static final Set<String> NULL_OR_EMPTY_VALUES = new HashSet<String>() {{
		add(IS_NULL_OR_EMPTY);
		add(IS_NOT_PRESENT_OR_EMPTY);
		add(IS_NOT_SET_OR_EMPTY);
	}};
	
	@SpecialDataModel(
			name = "isAnyValue",
			usage = "@{isAnyValue}",
			description = "Checks if the actual value has any value"
	)
	public static final String IS_ANY_VALUE = "@{isAnyValue}";
	@SpecialDataModel(
			name = "isNumber",
			usage = "@{isNumber}",
			description = "Checks if the actual value is a number"
	)
	public static final String IS_NUMBER = "@{isNumber}";
	@SpecialDataModel(
			name = "isInteger",
			usage = "@{isInteger}",
			description = "Checks if the actual value is an integer"
	)
	public static final String IS_INTEGER = "@{isInteger}";
	@SpecialDataModel(
			name = "isFloat",
			usage = "@{isFloat}",
			description = "Checks if the actual value is a floating point number"
	)
	public static final String IS_FLOAT = "@{isFloat}";
	public static final Set<String> SPECIAL_VALUES = new HashSet<String> () {{
		add(IS_NULL);
		add(IS_NOT_PRESENT);
		add(IS_NOT_SET);
		add(IS_NOT_NULL);
		add(IS_PRESENT);
		add(IS_SET);
		add(IS_EMPTY);
		add(IS_NOT_EMPTY);
		add(IS_NULL_OR_EMPTY);
		add(IS_NOT_PRESENT_OR_EMPTY);
		add(IS_NOT_SET_OR_EMPTY);
		add(IS_ANY_VALUE);
		add(IS_NUMBER);
		add(IS_INTEGER);
		add(IS_FLOAT);
	}};
	
	public static final String IS_TIMESTAMP_NAME = "isTimestamp";
	public static final String IS_TIMESTAMP_START = "@{" + IS_TIMESTAMP_NAME + "('";
	public static final String IS_AFTER_DATE_NAME = "isAfterDate";
	public static final String IS_AFTER_DATE = "@{" + IS_AFTER_DATE_NAME + "(";
	public static final String IS_BEFORE_DATE_NAME = "isBeforeDate";
	public static final String IS_BEFORE_DATE = "@{" + IS_BEFORE_DATE_NAME + "(";
	public static final String IS_BETWEEN_DATES_NAME = "isBetweenDates";
	public static final String IS_BETWEEN_DATES = "@{" + IS_BETWEEN_DATES_NAME + "(";
	public static final String PATTERN_NAME = "pattern";
	public static final String PATTERN_START = "{" + PATTERN_NAME + "(";
	public static final String AS_NUMBER_NAME = "asNumber";
	public static final String AS_NUMBER_START = "{" + AS_NUMBER_NAME + "(";
	public static final String AS_ABS_NUMBER_NAME = "asAbsNumber";
	public static final String AS_ABS_NUMBER_START = "{" + AS_ABS_NUMBER_NAME + "(";
	public static final String IS_NOT_EQUAL_TEXT_NAME = "isNotEqualText";
	public static final String IS_NOT_EQUAL_TEXT = "@{" + IS_NOT_EQUAL_TEXT_NAME + "(";
	public static final String IS_NOT_EQUAL_NUMBER_NAME = "isNotEqualNumber";
	public static final String IS_NOT_EQUAL_NUMBER = "@{" + IS_NOT_EQUAL_NUMBER_NAME + "(";

	public static final String IS_GREATER_THAN_NAME = "isGreaterThan";
	public static final String IS_GREATER_THAN = "@{" + IS_GREATER_THAN_NAME + "(";
	public static final String IS_GREATER_OR_EQUAL_NAME = "isGreaterOrEqual";
	public static final String IS_GREATER_OR_EQUAL = "@{" + IS_GREATER_OR_EQUAL_NAME + "(";
	public static final String IS_LESS_THAN_NAME = "isLessThan";
	public static final String IS_LESS_THAN = "@{" + IS_LESS_THAN_NAME + "(";
	public static final String IS_LESS_THAN_OR_EQUAL_NAME = "isLessOrEqual";
	public static final String IS_LESS_OR_EQUAL = "@{" + IS_LESS_THAN_OR_EQUAL_NAME + "(";
	public static final String IS_BETWEEN_NAME = "isBetween";
	public static final String IS_BETWEEN = "@{" + IS_BETWEEN_NAME + "(";
	
	public static final String INCLUDE_BOTH = "includeBoth", INCLUDE_RIGHT = "includeRight", INCLUDE_LEFT = "includeLeft";
	
	public static final Set<String> SPECIAL_FUNCTIONS = new HashSet<String> () {{
		add(IS_BEFORE_DATE);
		add(IS_AFTER_DATE);
		add(IS_BETWEEN_DATES);
		add(IS_GREATER_THAN);
		add(IS_GREATER_OR_EQUAL);
		add(IS_LESS_THAN);
		add(IS_LESS_OR_EQUAL);
		add(IS_BETWEEN);
		add(PATTERN_START);
		add(AS_ABS_NUMBER_START);
		add(AS_NUMBER_START);
		add(IS_NOT_EQUAL_NUMBER);
		add(IS_NOT_EQUAL_TEXT);
	}};
	
	public static final Set<String> SPECIAL_NUMBER_FUNCTION_NAMES = new HashSet<String>() {{
		add(AS_NUMBER_NAME);
		add(AS_ABS_NUMBER_NAME);
		add(IS_NOT_EQUAL_NUMBER_NAME);
		add(IS_GREATER_THAN_NAME);
		add(IS_GREATER_OR_EQUAL_NAME);
		add(IS_LESS_THAN_NAME);
		add(IS_LESS_THAN_OR_EQUAL_NAME);
		add(IS_BETWEEN_NAME);
		add(IS_BETWEEN_DATES_NAME);
	}};

	public static final Pattern FLOAT_PATTERN = Pattern.compile("[+-]?\\d+[,.]\\d+");
	public static final Pattern INTEGER_PATTERN = Pattern.compile("[+-]?\\d+");

	public static final char[] META_CHARACTERS =
			new char[] { '^', '[', '.', '$', '{', '*', '(', '\\', '+', ')', '|', '?', '<', '>' };

	public static final String EXPECTED_VALUE = "expected value";
	public static final String SCALE = "Scale";
	public static final String ERROR = "Error";
	public static final String IS_CASE_SENSITIVE = "IsCaseSensitive";
	public static final String IS_IGNORE_SPACES = "IsIgnoreSpaces";

	@SpecialDataModel(
			name = "pattern",
			value = "String pattern",
			usage = "@{pattern('2022-01-20 14:..:..')}",
			description = "Checks the actual value using the given regular expression"
	)
	public boolean compareByPattern(String expected, String actual)
	{
		if (actual == null)
			actual = "";
		String pattern = preparePattern(expected);
		Matcher matcher = Pattern.compile(pattern).matcher(actual);
		return matcher.matches();
	}

	public String preparePattern(String value)
	{
		String[] parts = value.split("((\\{pattern\\(')|('\\)}))");
		if (parts.length == 3 && parts[0].isEmpty() && parts[2].isEmpty())
		{
			return parts[1];
		}
		else
		{
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < parts.length; i++)
			{
				if (!parts[i].isEmpty())
				{
					result.append((i % 2 == 0) ? escapeMetaCharacters(parts[i]) : parts[i]);
				}
			}
			return result.toString();
		}
	}

	public String prepareExpectedValue(String expectedValue)
	{
		int start = expectedValue.indexOf("(");
		int end = expectedValue.lastIndexOf(")");
		if (start == end || start == -1 || end == -1)
		{
			return null;
		}

		return expectedValue.substring(start + 1, end);
	}

	public String escapeMetaCharacters(String value)
	{
		if (StringUtils.containsNone(value, META_CHARACTERS))
		{
			return value;
		}
		else
		{
			StringBuilder result = new StringBuilder();
			for (char c : value.toCharArray())
			{
				if (ArrayUtils.contains(META_CHARACTERS, c))
				{
					result.append('\\');
				}
				result.append(c);
			}
			return result.toString();
		}
	}

	public boolean isSpecialValue(String value)
	{
		return (value != null)
				&& (SPECIAL_VALUES.contains(value.trim()) || StringUtils.startsWith(value, IS_TIMESTAMP_START));
	}

	public boolean isSpecialFunction(String value)
	{
		value = StringUtils.replaceChars(value, "()", "");
		for (String function : SPECIAL_FUNCTIONS)
		{
			function = StringUtils.replaceChars(function, "()", "");
			if (StringUtils.startsWith(value, function))
				return true;
		}
		return false;
	}


	/**
	 * This method should be used to check if given expected value is better to use with
	 * {@link #compareValues(String, String)} method for correct comparison.
	 *
	 * @param value concerned
	 * @return true if given expected value should be used with compareValues() method
	 */
	public boolean isForCompareValues(String value)
	{
		return isSpecialValue(value) || isSpecialFunction(value) || contains(trim(value), PATTERN_START);
	}

	@SpecialDataModel(
			name = "isTimestamp",
			value = "String format",
			usage = "@{isTimestamp('dd.MM.yyyy')}",
			description = "Checks if the actual value is a timestamp in the given format"
	)
	public boolean isTimeStamp(String value, String format)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setLenient(false);
		ParsePosition position = new ParsePosition(0);

		Date date = dateFormat.parse(value, position);

		return (position.getIndex() == value.length() && date != null);
	}

	protected boolean numbersEqual(BigDecimal expectedValue, BigDecimal actualValue, Integer scale, BigDecimal error, boolean abs)
	{
		if(abs)
		{
			expectedValue = expectedValue.abs();
			actualValue = actualValue.abs();
		}

		if(scale != null)
		{
			expectedValue = expectedValue.setScale(scale, BigDecimal.ROUND_HALF_UP);
			actualValue = actualValue.setScale(scale, BigDecimal.ROUND_HALF_UP);
		}

		if(error == null)
		{
			return expectedValue.compareTo(actualValue) == 0;
		}
		else
		{
			return expectedValue.subtract(actualValue).abs().compareTo(error) < 0;
		}
	}

	@SpecialDataModel(
			name = "asNumber",
			value = "BigDecimal number, (Optional) BigDecimal marginOfError, (Optional) int scale",
			usage = "@{asNumber(500.1, 10, 1)}",
			description = "Compares the actual value and the given value as numbers"
	)
	public boolean compareAsNumber(String expectedValue, String actualValue) throws ParametersException
	{
		return numbersEqual(expectedValue, actualValue, false, false, AS_NUMBER_NAME);
	}
	
	@SpecialDataModel(
			name = "asAbsNumber",
			value = "BigDecimal number, (Optional) BigDecimal marginOfError, (Optional) int scale",
			usage = "@{asAbsNumber(500.1, 10, 1)}",
			description = "Compares the actual value and the given value as absolute numbers"
	)
	public boolean compareAsAbsNumber(String expectedValue, String actualValue) throws ParametersException
	{
		return numbersEqual(expectedValue, actualValue, true, false, AS_ABS_NUMBER_NAME);
	}

	@SpecialDataModel(
			name = "isNotEqualNumber",
			value = "BigDecimal number, (Optional) BigDecimal marginOfError, (Optional) int scale",
			usage = "@{isNotEqualNumber(500.1, 10, 1)}",
			description = "Checks if the actual number and the given number are not equal numbers"
	)
	protected boolean isNotEqualNumber(String expectedValue, String actualValue) throws ParametersException
	{
		return numbersEqual(expectedValue, actualValue, false, true, IS_NOT_EQUAL_NUMBER_NAME);
	}
	
	protected boolean numbersEqual(String expectedExpression, String actualValue,
	                             boolean abs, boolean invert,
	                             String functionName) throws ParametersException
	{
		String paramsLine = prepareExpectedValue(expectedExpression);
		if (isEmpty(paramsLine))
			throw new ParametersException(format("Parameters in function '%s' are missing.", functionName));
		
		String[] params = removeQuotesAndSpaces(split(paramsLine,','));
		checkNumberOfParams(functionName, params, 1, 3);
		
		BigDecimal expected = getBigDecimalValue(params, 0, functionName, EXPECTED_VALUE);
		BigDecimal error = getBigDecimalValue(params, 1, functionName, ERROR);
		Integer scale = getIntegerValue(params, 2, functionName, SCALE);
		
		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);
		
		boolean equals = numbersEqual(expected, actual, scale, error, abs);
		
		return invert != equals;
	}

	public boolean compareValues(String expectedValue, String actualValue, boolean isCaseSensitive) throws ParametersException
	{
		logger.trace("Checking actual value '{}' using expression '{}'.", actualValue, expectedValue);
		
		String trimmedExpectedValue = trim(expectedValue);
		if (contains(expectedValue, PATTERN_START))
		{
			return compareByPattern(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_TIMESTAMP_START))
		{
			int start = expectedValue.indexOf('\'');
			int end = expectedValue.lastIndexOf('\'');
			return start != end && actualValue != null && isTimeStamp(actualValue, expectedValue.substring(start + 1, end));
		}
		else if (StringUtils.startsWith(expectedValue, IS_BEFORE_DATE))
		{
			return actualValue != null && isBeforeDate(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_AFTER_DATE))
		{
			return actualValue != null && isAfterDate(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_BETWEEN_DATES))
		{
			return actualValue != null && isBetweenDates(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_GREATER_THAN))
		{
			return isGreaterThan(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_GREATER_OR_EQUAL))
		{
			return isGreaterOrEq(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_LESS_THAN))
		{
			return isLessThan(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_LESS_OR_EQUAL))
		{
			return isLessOrEq(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_BETWEEN))
		{
			return isBetween(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, AS_NUMBER_START))
		{
			return compareAsNumber(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, AS_ABS_NUMBER_START))
		{
			return compareAsAbsNumber(expectedValue, actualValue);
		}
		else if (SPECIAL_VALUES.contains(trimmedExpectedValue))
		{
			if (NULL_VALUES.contains(trimmedExpectedValue))
			{
				return actualValue == null;
			}
			else if (NOT_NULL_VALUES.contains(trimmedExpectedValue))
			{
				return actualValue != null;
			}
			else if (IS_EMPTY.equals(trimmedExpectedValue))
			{
				return actualValue != null && actualValue.isEmpty();
			}
			else if (IS_NOT_EMPTY.equals(trimmedExpectedValue))
			{
				return StringUtils.isNotEmpty(actualValue);
			}
			else if (NULL_OR_EMPTY_VALUES.contains(trimmedExpectedValue))
			{
				return isEmpty(actualValue);
			}
			else if (IS_ANY_VALUE.equals(trimmedExpectedValue))
			{
				return true;
			}
			else if (IS_NUMBER.equals(trimmedExpectedValue))
			{
				return NumberUtils.isNumber(actualValue);
			}
			else if (IS_FLOAT.equals(trimmedExpectedValue))
			{
				return FLOAT_PATTERN.matcher(actualValue).matches();
			}
			else // @{isInteger}
			{
				return INTEGER_PATTERN.matcher(actualValue).matches();
			}
		}
		else if (StringUtils.startsWith(expectedValue, IS_NOT_EQUAL_NUMBER))
		{
			return this.isNotEqualNumber(expectedValue, actualValue);
		}
		else if (StringUtils.startsWith(expectedValue, IS_NOT_EQUAL_TEXT))
		{
			return this.isNotEqualText(expectedValue, actualValue);
		}
		else if (isCaseSensitive)
		{
			return StringUtils.equals(expectedValue, actualValue);
		}
		else
			return StringUtils.equalsIgnoreCase(expectedValue, actualValue);
	}

	public boolean compareValues(String expectedValue, String actualValue) throws ParametersException
	{
		return compareValues(expectedValue, actualValue, true);
	}

	public boolean compareValuesIgnoreCase(String expectedValue, String actualValue) throws ParametersException
	{
		return compareValues(expectedValue, actualValue, false);
	}
	
	@SpecialDataModel(
			name = "isGreaterOrEqual",
			value = "BigDecimal number",
			usage = "@{isGreaterOrEqual(500)}",
			description = "Checks if the actual value is greater than or equal to the given number"
	)
	public boolean isGreaterOrEq(String expectedValue, String actualValue) throws ParametersException
	{
		BigDecimal expected = getExpectedBigDecimal(expectedValue, IS_GREATER_OR_EQUAL_NAME);
		
		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);
		
		return actual.compareTo(expected) >= 0;
	}
	
	@SpecialDataModel(
			name = "isGreaterThan",
			value = "BigDecimal number",
			usage = "@{isGreaterThan(500)}",
			description = "Checks if the actual value is greater than the given number"
	)
	public boolean isGreaterThan(String expectedValue, String actualValue) throws ParametersException 
	{
		BigDecimal expected = getExpectedBigDecimal(expectedValue, IS_GREATER_THAN_NAME);

		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);

		return actual.compareTo(expected) > 0;
	}
	
	@SpecialDataModel(
			name = "isLessThan",
			value = "BigDecimal number",
			usage = "@{isLessThan(500)}",
			description = "Checks if the actual value is less than the given number"
	)
	public boolean isLessThan(String expectedValue, String actualValue) throws ParametersException
	{
		BigDecimal expected = getExpectedBigDecimal(expectedValue, IS_LESS_THAN_NAME);

		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);

		return actual.compareTo(expected) < 0;
	}
	
	@SpecialDataModel(
			name = "isLessOrEqual",
			value = "BigDecimal number",
			usage = "@{isLessOrEqual(500)}",
			description = "Checks if the actual value is less than or equal to the given number"
	)
	public boolean isLessOrEq(String expectedValue, String actualValue) throws ParametersException
	{
		BigDecimal expected = getExpectedBigDecimal(expectedValue, IS_LESS_THAN_OR_EQUAL_NAME);

		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);

		return actual.compareTo(expected) <= 0;
	}

	/**
	* Syntax examples of matrix function @{isBetween}: 
	* <br>@{isBetween(2,4,'includeLeft')} ~ [2,4); @{isBetween(2,4,'includeRight')} ~ (2,4];
	* <br>@{isBetween(2,4)} ~ (2,4); @{isBetween(2,4,'includeBoth')} ~ [2,4];
	 */
	@SpecialDataModel(
			name = "isBetween",
			value = "BigDecimal left, BigDecimal right, (Optional) String inclusion",
			usage = "@{isBetween(2,4,'includeLeft')}",
			description = "Checks if the actual value is between two given numbers. Inclusion types: " + INCLUDE_BOTH + ", " + INCLUDE_LEFT  + ", " + INCLUDE_RIGHT
	)
	public boolean isBetween(String expectedExpression, String actualValue) throws ParametersException
	{
		String paramsLine = prepareExpectedValue(expectedExpression);
		//1th - LeftBoundary, 2th - RightBoundary, (Optional)3th - Boundaries inclusion
		String[] params = split(paramsLine, ',');
		ParametersUtils.checkNumberOfParams(IS_BETWEEN_NAME, params, 2, 3);
		ParametersUtils.removeQuotesAndSpaces(params);

		BigDecimal leftBound = getBigDecimalValue(params, 0, IS_BETWEEN_NAME, "leftBound");
		BigDecimal rightBound = getBigDecimalValue(params, 1, IS_BETWEEN_NAME, "rightBound");

		if (!isNumberWithoutQualifier(actualValue))
		{
			logger.warn("Unable to parse actual value '{}' in function '{}'", actualValue, IS_BETWEEN_NAME);
			return false;
		}
		BigDecimal actual = new BigDecimal(StringUtils.strip(actualValue, "'"));

		String inclusion = params.length == 3 ? StringUtils.strip(params[2], "'") : null;
		
		return isBetween(leftBound, rightBound, actual, inclusion);
	}

	private boolean isBetween(BigDecimal left, BigDecimal right, BigDecimal actual, String inclusion)
			throws ParametersException
	{
		if (StringUtils.isEmpty(inclusion))
			return actual.compareTo(left) > 0 && actual.compareTo(right) < 0;
		
		if (INCLUDE_LEFT.equals(inclusion))
			return actual.compareTo(left) >= 0 && actual.compareTo(right) < 0;
		
		if (INCLUDE_RIGHT.equals(inclusion))
			return actual.compareTo(left) > 0 && actual.compareTo(right) <= 0;
		
		if (INCLUDE_BOTH.equals(inclusion))
			return actual.compareTo(left) >= 0 && actual.compareTo(right) <= 0;

		String msg = String.format("Parameter '%s' in function '%s' is invalid", inclusion, IS_BETWEEN_NAME);
		logger.warn(msg);
		throw new ParametersException(msg);
	}

	protected BigDecimal getExpectedBigDecimal(String textValue, String functionName) throws ParametersException
	{
		String preparedValue = prepareExpectedValue(textValue);
		if (isNumberWithoutQualifier(preparedValue))
			return new BigDecimal(preparedValue);
		else 
			throw new ParametersException(format("In function '%s' expected value '%s' isn't valid number.", 
					functionName, preparedValue));
	}

	public ResultDetail createResultDetail(String paramName, String expectedValue, String actualValue, InfoIndication infoIndication)
	{
		ResultDetail resultDetail = new ResultDetail();
		resultDetail.setParam(paramName);
		resultDetail.setExpected(expectedValue);
		resultDetail.setActual(actualValue);

		if ((infoIndication == InfoIndication.NULL && expectedValue == null)
				|| (infoIndication == InfoIndication.NULL_OR_EMPTY && isEmpty(expectedValue)))
		{
			resultDetail.setInfo(true);
			resultDetail.setIdentical(true);
		}
		else
		{
			try
			{
				resultDetail.setIdentical(compareValues(expectedValue, actualValue));
			}
			catch (ParametersException e)
			{
				resultDetail.setErrorMessage(e.getMessage());
			}
		}
		return resultDetail;
	}

	public ResultDetail createResultDetail(String paramName, String expectedValue, String actualValue)
	{
		return createResultDetail(paramName, expectedValue, actualValue, null);
	}

	public void compare(DetailedResult result, List<String> expectedValues, List<String> actualValues, String numberPrefix)
	{
		if (numberPrefix == null)
			numberPrefix = "";

		int number = 1;

		for (String expected : expectedValues)
		{
			if (actualValues.contains(expected))
			{
				result.addResultDetail(new ResultDetail(numberPrefix + number, expected, expected, true));
				actualValues.remove(expected);
			}
			else
			{
				result.addResultDetail(new ResultDetail(numberPrefix + number, expected, "", false));
			}
			number++;
		}

		if (actualValues.size() != 0)
		{
			for (String actual : actualValues)
			{
				result.addResultDetail(new ResultDetail(numberPrefix + number, "", actual, false));
				number++;
			}
		}
	}

	public void compare(DetailedResult result, List<String> expectedValues, List<String> actualValues)
	{
		compare(result, expectedValues, actualValues, "");
	}
	
	public boolean compareDates(String expectedValue, String actualValue, boolean isBefore) {
		int start = expectedValue.indexOf('(');
		int end = expectedValue.lastIndexOf(')');
		if (start == end) {
			return false;
		}
		String expectedParams = expectedValue.substring(start + 1, end);
		String dateExpStr = expectedParams.substring(0, expectedParams.indexOf(','));
		String formatExpStr = expectedParams.substring(expectedParams.indexOf(',') + 1);
		start = dateExpStr.indexOf('\'');
		end = dateExpStr.lastIndexOf('\'');
		if (start == end) {
			return false;
		}
		dateExpStr = dateExpStr.substring(start + 1, end);
		start = formatExpStr.indexOf('\'');
		end = formatExpStr.lastIndexOf('\'');
		if (start == end) {
			return false;
		}
		formatExpStr = formatExpStr.substring(start + 1, end);
		SimpleDateFormat df = new SimpleDateFormat(formatExpStr);
		df.setLenient(false);
		Date exp, act;
		try {
			exp = df.parse(dateExpStr);
			act = df.parse(actualValue);
		} catch (ParseException e) {
			logger.warn("Compare dates. Incorrect date format", e);
			return false;
		}
		
		return isBefore ? act.before(exp) : act.after(exp);
	}
	
	@SpecialDataModel(
			name = "isBeforeDate",
			value = "String date, String format",
			usage = "@{isBeforeDate('01.01.2001', 'dd.MM.yyyy')}",
			description = "Checks if the actual date is before the given date"
	)
	public boolean isBeforeDate(String expectedValue, String actualValue)
	{
		return compareDates(expectedValue, actualValue, true);
	}
	
	@SpecialDataModel(
			name = "isAfterDate",
			value = "String date, String format",
			usage = "@{isAfterDate('01.01.2001', 'dd.MM.yyyy')}",
			description = "Checks if the actual date is after the given date"
	)
	public boolean isAfterDate(String expectedValue, String actualValue)
	{
		return compareDates(expectedValue, actualValue, false);
	}

	/**
	 * Syntax example of matrix function @{isBetweenDates}: 
	 * <br>@{isBetweenDates('04.02.2003','06.02.2003','dd.MM.yyyy','includeLeft')} ~ [04.02.2003,06.02.2003);
	 * <br>@{isBetweenDates('04.02.2003','06.02.2003','dd.MM.yyyy','includeRight')} ~ (04.02.2003,06.02.2003];
	 * <br>@{isBetweenDates('04.02.2003','06.02.2003','dd.MM.yyyy','includeBoth')} ~ [04.02.2003,06.02.2003];
	 * <br>@{isBetweenDates('04.02.2003','06.02.2003','dd.MM.yyyy')} ~ (04.02.2003,06.02.2003);
	 */
	@SpecialDataModel(
			name = "isBetweenDates",
			value = "String leftDateBound, String rightDateBound, String format, (Optional) String inclusion",
			usage = "@{isBetweenDates('04.02.2003', '06.02.2003', 'dd.MM.yyyy')}",
			description = "Checks if the actual date is between two given dates"
	)
	public boolean isBetweenDates(String expectedExpression, String actualValue) throws ParametersException
	{
		String paramsLine = prepareExpectedValue(expectedExpression);
		String[] params = split(paramsLine, ',');
		ParametersUtils.checkNumberOfParams(IS_BETWEEN_DATES_NAME, params, 3, 4);
		ParametersUtils.removeQuotesAndSpaces(params);
		
		int i = 3;
		SimpleDateFormat dtf;
		Date leftBound, rightBound, actual;
		try
		{
			dtf = new SimpleDateFormat(params[--i]);
			dtf.setLenient(false);
			rightBound = dtf.parse(params[--i]);
			leftBound = dtf.parse(params[--i]);
		}
		catch (ParseException | IllegalArgumentException e)
		{
			String msg = format("Parameter '%d' in function '%s' contains invalid value: '%s'.",
					i, IS_BETWEEN_DATES_NAME, params[i]);
			logger.warn(msg, e);
			throw new ParametersException(msg, e);
		}

		try
		{
			actual = dtf.parse(StringUtils.strip(actualValue, "'"));
		}
		catch (Exception e)
		{
			logger.warn("Unable to parse actual value {} in function '{}'", actualValue, IS_BETWEEN_DATES_NAME, e);
			return false;
		}

		return isBetweenDates(leftBound, rightBound, actual, params.length == 4 ? params[3] : null);
	}

	private boolean isBetweenDates(Date leftBound, Date rightBound, Date actual, String inclusion) 
			throws ParametersException
	{
		if (StringUtils.isEmpty(inclusion))
			return actual.after(leftBound) && actual.before(rightBound);

		if (INCLUDE_LEFT.equals(inclusion))
			return (actual.equals(leftBound) || actual.after(leftBound)) && actual.before(rightBound);

		if (INCLUDE_RIGHT.equals(inclusion))
			return actual.after(leftBound) && (actual.equals(rightBound) || actual.before(rightBound));

		if (INCLUDE_BOTH.equals(inclusion))
			return (actual.equals(leftBound) || actual.after(leftBound)) &&
					(actual.equals(rightBound) || actual.before(rightBound));

		String msg = String.format("Parameter '%s' in function '%s' is invalid", inclusion, IS_BETWEEN_DATES_NAME);
		logger.warn(msg);
		throw new ParametersException(msg);
	}

	public boolean isEmptyByMatrix(String matrixValue)
	{
		return IS_EMPTY.equals(matrixValue);
	}

	protected boolean compareTexts(String preparedValue, String actualValue, boolean isCaseSensitive, boolean isIgnoreSpaces)
	{
		if(!isCaseSensitive)
		{
			preparedValue = StringUtils.lowerCase(preparedValue);
			actualValue = StringUtils.lowerCase(actualValue);
		}
		if(isIgnoreSpaces)
		{
			preparedValue = StringUtils.trim(preparedValue);
			actualValue = StringUtils.trim(actualValue);
		}

		return preparedValue.equals(actualValue);
	}

	@SpecialDataModel(
			name = "isNotEqualText",
			value = "String text, (Optional) boolean isCaseSensitive, (Optional) boolean isIgnoreSpaces",
			usage = "@{isNotEqualText('Your text', true, true)}",
			description = "Checks if the actual text and the given text are not equal"
	)
	protected boolean isNotEqualText(String expectedValue, String actualValue) throws ParametersException
	{
		String expressionParameters = prepareExpectedValue(expectedValue);
		if(isEmpty(expressionParameters))
		{
			throw new ParametersException(format("Parameters in function '%s' are missing.", IS_NOT_EQUAL_TEXT_NAME));
		}

		String[] entries = StringUtils.split(expressionParameters,',');
		checkNumberOfParams(IS_NOT_EQUAL_TEXT_NAME, entries, 1, 3);

		String preparedValue = StringUtils.trim(entries[0]);
		if(!(preparedValue.startsWith("'") && preparedValue.endsWith("'")))
		{
			throw new ParametersException(format("In function '%s' value '%s' not valid for parameter[0] - 'Expected value'.", IS_NOT_EQUAL_TEXT_NAME, preparedValue));
		}
		else
			preparedValue = preparedValue.substring(1, preparedValue.length() - 1);

		boolean isCaseSensitive = getBooleanValue(entries, 1, IS_NOT_EQUAL_TEXT_NAME, IS_CASE_SENSITIVE);
		boolean isIgnoreSpaces = getBooleanValue(entries, 2, IS_NOT_EQUAL_TEXT_NAME, IS_IGNORE_SPACES);

		return !compareTexts(preparedValue, actualValue, isCaseSensitive, isIgnoreSpaces);
	}

	public ComparisonRow createComparisonRow(String paramName, String expectedValue, String actualValue, String actualValueForReport, 
			boolean isHighlighted, InfoIndication infoIndication)
	{
		ComparisonRow comparisonRow = new ComparisonRow();
		comparisonRow.setParam(paramName);
		comparisonRow.setExpected(expectedValue);
		comparisonRow.setActual(actualValueForReport);
		comparisonRow.setHighlighted(isHighlighted);

		if ((infoIndication == InfoIndication.NULL && expectedValue == null)
				|| (infoIndication == InfoIndication.NULL_OR_EMPTY && isEmpty(expectedValue)))
		{
			comparisonRow.setInfo(true);
			comparisonRow.setIdentical(true);
		}
		else
		{
			try
			{
				comparisonRow.setIdentical(compareValues(expectedValue, actualValue));
			}
			catch (ParametersException e)
			{
				comparisonRow.setErrorMessage(e.getMessage());
			}
		}
		return comparisonRow;
	}


	public ComparisonRow createComparisonRow(String paramName, String expectedValue, String actualValue, String actualValueForReport, boolean isHighlighted)
	{
		return createComparisonRow(paramName, expectedValue, actualValue, actualValueForReport, isHighlighted, null);
	}
}
