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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static com.exactprosystems.clearth.utils.ParametersUtils.*;
import static org.apache.commons.lang.StringUtils.*;

public class ComparisonUtils
{

	private static final Logger logger = LoggerFactory.getLogger(ComparisonUtils.class);

	public enum InfoIndication {
		NULL,
		NULL_OR_EMPTY
	}

	public static final String IS_NULL = "@{isNull}";
	public static final String IS_NOT_PRESENT = "@{isNotPresent}";
	public static final String IS_NOT_SET = "@{isNotSet}";
	public static final Set<String> NULL_VALUES = new HashSet<String>() {{
		add(IS_NULL);
		add(IS_NOT_PRESENT);
		add(IS_NOT_SET);
	}};

	public static final String IS_NOT_NULL = "@{isNotNull}";
	public static final String IS_PRESENT = "@{isPresent}";
	public static final String IS_SET = "@{isSet}";
	public static final Set<String> NOT_NULL_VALUES = new HashSet<String>() {{
		add(IS_NOT_NULL);
		add(IS_PRESENT);
		add(IS_SET);
	}};

	public static final String IS_EMPTY = "@{isEmpty}";

	public static final String IS_NOT_EMPTY = "@{isNotEmpty}";

	public static final String IS_NULL_OR_EMPTY = "@{isNullOrEmpty}";
	public static final String IS_NOT_PRESENT_OR_EMPTY = "@{isNotPresentOrEmpty}";
	public static final String IS_NOT_SET_OR_EMPTY = "@{isNotSetOrEmpty}";
	public static final Set<String> NULL_OR_EMPTY_VALUES = new HashSet<String>() {{
		add(IS_NULL_OR_EMPTY);
		add(IS_NOT_PRESENT_OR_EMPTY);
		add(IS_NOT_SET_OR_EMPTY);
	}};

	public static final String IS_ANY_VALUE = "@{isAnyValue}";
	public static final String IS_NUMBER = "@{isNumber}";
	public static final String IS_INTEGER = "@{isInteger}";
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

	public static final String IS_TIMESTAMP_START = "@{isTimestamp('";
	public static final String IS_AFTER_DATE = "@{isAfterDate(";
	public static final String IS_BEFORE_DATE = "@{isBeforeDate(";
	public static final String PATTERN_START = "{pattern(";
	public static final String AS_NUMBER_NAME = "asNumber";
	public static final String AS_NUMBER_START = "{" + AS_NUMBER_NAME + "(";
	public static final String AS_ABS_NUMBER_NAME = "asAbsNumber";
	public static final String AS_ABS_NUMBER_START = "{" + AS_ABS_NUMBER_NAME + "(";
	public static final String IS_NOT_EQUAL_TEXT = "@{isNotEqualText(";
	public static final String IS_NOT_EQUAL_NUMBER_NAME = "isNotEqualNumber";
	public static final String IS_NOT_EQUAL_NUMBER = "@{" + IS_NOT_EQUAL_NUMBER_NAME + "(";

	public static final String IS_GRATER_THAN_NAME = "isGreaterThan";
	public static final String IS_GREATER_THAN = "@{" + IS_GRATER_THAN_NAME + "(";
	public static final String IS_GRATER_OR_NULL_NAME = "isGreaterOrEqual";
	public static final String IS_GREATER_OR_EQUAL = "@{" + IS_GRATER_OR_NULL_NAME + "(";
	public static final String IS_LESS_THAN_NAME = "isLessThan";
	public static final String IS_LESS_THAN = "@{" + IS_LESS_THAN_NAME + "(";
	public static final String IS_LESS_THAN_OR_EQUAL_NAME = "isLessOrEqual";
	public static final String IS_LESS_OR_EQUAL = "@{" + IS_LESS_THAN_OR_EQUAL_NAME + "(";
	
	public static final Set<String> SPECIAL_FUNCTIONS = new HashSet<String> () {{
		add(IS_BEFORE_DATE);
		add(IS_AFTER_DATE);
		add(IS_GREATER_THAN);
		add(IS_GREATER_OR_EQUAL);
		add(IS_LESS_THAN);
		add(IS_LESS_OR_EQUAL);
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
		add(IS_GRATER_THAN_NAME);
		add(IS_GRATER_OR_NULL_NAME);
		add(IS_LESS_THAN_NAME);
		add(IS_LESS_THAN_OR_EQUAL_NAME);
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
		return SPECIAL_VALUES.contains(value.trim()) || StringUtils.startsWith(value, IS_TIMESTAMP_START);
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
	 * This method should be used to check if given expected value is better used with {@link #compareValues(String, String)} method for correct comparison
	 *
	 * @param value concerned
	 * @return true if given expected value should be used with compareValues() method
	 */
	public boolean isForCompareValues(String value)
	{
		return isSpecialValue(value) || isSpecialFunction(value) || contains(trim(value), PATTERN_START);
	}

	public boolean isTimeStamp(String value, String format)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
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

	public boolean compareAsNumber(String expectedValue, String actualValue) throws ParametersException
	{
		return numbersEqual(expectedValue, actualValue, false, false, AS_NUMBER_NAME);
	}

	public boolean compareAsAbsNumber(String expectedValue, String actualValue) throws ParametersException
	{
		return numbersEqual(expectedValue, actualValue, true, false, AS_ABS_NUMBER_NAME);
	}

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
			int start = expectedValue.indexOf("'");
			int end = expectedValue.lastIndexOf("'");
			return start != end && actualValue != null && isTimeStamp(actualValue, expectedValue.substring(start + 1, end));
		}
		else if (StringUtils.startsWith(expectedValue, IS_BEFORE_DATE))
		{
			return actualValue != null && compareDates(expectedValue, actualValue, true);
		}
		else if (StringUtils.startsWith(expectedValue, IS_AFTER_DATE))
		{
			return actualValue != null && compareDates(expectedValue, actualValue, false);
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
		else if(StringUtils.startsWith(expectedValue, IS_NOT_EQUAL_NUMBER))
		{
			return this.isNotEqualNumber(expectedValue, actualValue);
		}
		else if(StringUtils.startsWith(expectedValue, IS_NOT_EQUAL_TEXT))
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
	
	public boolean isGreaterOrEq(String expectedValue, String actualValue) throws ParametersException
	{
		BigDecimal expected = getExpectedBigDecimal(expectedValue, IS_GREATER_OR_EQUAL);
		
		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);
		
		return actual.compareTo(expected) >= 0;
	}
	
	public boolean isGreaterThan(String expectedValue, String actualValue) throws ParametersException 
	{
		BigDecimal expected = getExpectedBigDecimal(expectedValue, IS_GREATER_THAN);

		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);

		return actual.compareTo(expected) > 0;
	}
	
	public boolean isLessThan(String expectedValue, String actualValue) throws ParametersException
	{
		BigDecimal expected = getExpectedBigDecimal(expectedValue, IS_LESS_THAN);

		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);

		return actual.compareTo(expected) < 0;
	}
	
	public boolean isLessOrEq(String expectedValue, String actualValue) throws ParametersException
	{
		BigDecimal expected = getExpectedBigDecimal(expectedValue, IS_LESS_OR_EQUAL);

		if (!isNumberWithoutQualifier(actualValue))
			return false;
		BigDecimal actual = new BigDecimal(actualValue);

		return actual.compareTo(expected) <= 0;
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
		int start = expectedValue.indexOf("(");
		int end = expectedValue.lastIndexOf(")");
		if (start == end) {
			return false;
		}
		String expectedParams = expectedValue.substring(start + 1, end);
		String dateExpStr = expectedParams.substring(0, expectedParams.indexOf(','));
		String formatExpStr = expectedParams.substring(expectedParams.indexOf(',') + 1);
		start = dateExpStr.indexOf("'");
		end = dateExpStr.lastIndexOf("'");
		if (start == end) {
			return false;
		}
		dateExpStr = dateExpStr.substring(start + 1, end);
		start = formatExpStr.indexOf("'");
		end = formatExpStr.lastIndexOf("'");
		if (start == end) {
			return false;
		}
		formatExpStr = formatExpStr.substring(start + 1, end);
		SimpleDateFormat df = new SimpleDateFormat(formatExpStr);
		Date exp, act;
		try {
			exp = df.parse(dateExpStr);
			act = df.parse(actualValue);
		} catch (ParseException e) {
			logger.warn("Compare dates. Incorrect date format", e);
			return false;
		}
		return isBefore && act.before(exp) || !isBefore && act.after(exp);

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

	protected boolean isNotEqualText(String expectedValue, String actualValue) throws ParametersException
	{
		String expressionParameters = prepareExpectedValue(expectedValue);
		if(isEmpty(expressionParameters))
		{
			throw new ParametersException(format("Parameters in function '%s' are missing.", IS_NOT_EQUAL_NUMBER_NAME));
		}

		String[] entries = StringUtils.split(expressionParameters,',');
		checkNumberOfParams(IS_NOT_EQUAL_NUMBER_NAME, entries, 1, 3);

		String preparedValue = StringUtils.trim(entries[0]);
		if(!(preparedValue.startsWith("'") && preparedValue.endsWith("'")))
		{
			throw new ParametersException(format("In function '%s' value '%s' not valid for parameter[0] - 'Expected value'.", IS_NOT_EQUAL_NUMBER_NAME, preparedValue));
		}
		else
			preparedValue = preparedValue.substring(1, preparedValue.length() - 1);

		boolean isCaseSensitive = getBooleanValue( entries, 1, IS_NOT_EQUAL_NUMBER_NAME, IS_CASE_SENSITIVE);
		boolean isIgnoreSpaces = getBooleanValue(entries, 2, IS_NOT_EQUAL_NUMBER_NAME, IS_IGNORE_SPACES);

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
