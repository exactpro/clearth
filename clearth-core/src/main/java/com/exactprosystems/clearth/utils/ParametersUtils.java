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

package com.exactprosystems.clearth.utils;

import static com.exactprosystems.clearth.utils.ComparisonUtils.INTEGER_PATTERN;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;

public class ParametersUtils {

	public static Integer getIntegerValue(String[] params, int paramIndex, String functionName, String parameterName) throws ParametersException
	{
		if(paramIndex > params.length - 1)
		{
			return null;
		}

		String param = StringUtils.trim(params[paramIndex]);

		if(StringUtils.isEmpty(param))
		{
			return null;
		}
		else if(INTEGER_PATTERN.matcher(param).matches())
		{
			return Integer.parseInt(param);
		}
		else
		{
			throw new ParametersException(String.format("In function '%s' value '%s' not valid for param[%s] - '%s'.", functionName, param, paramIndex + 1, parameterName));
		}
	}


	public static BigDecimal getBigDecimalValue(String[] params, int paramIndex, String functionName, String parameterName) throws ParametersException
	{
		if(paramIndex > params.length - 1)
		{
			return null;
		}

		String param = StringUtils.trim(params[paramIndex]);

		if(StringUtils.isEmpty(param))
		{
			return null;
		}
		else if(isNumberWithoutQualifier(param))
		{
			return new BigDecimal(param);
		}
		else
		{
			throw new ParametersException(String.format("In function '%s' value '%s' not valid for param[%s] - '%s'.", functionName, param, paramIndex + 1, parameterName));
		}
	}

	public static boolean getBooleanValue(String[] params, int paramIndex, String functionName, String parameterName) throws ParametersException
	{
		if(paramIndex > params.length - 1)
			return true;

		String param = StringUtils.trim(params[paramIndex]);
		if(param.equals("true") || StringUtils.isEmpty(param))
			return true;
		else if(param.equals("false"))
			return false;
		else
			throw new ParametersException(String.format("In function '%s' value '%s' not valid for param[%s] - '%s'.", functionName, param, paramIndex + 1, parameterName));
	}

	/**
	 * This method should be used to check if given value is a number and doesn`t have any qualifier like 'f', 'F', 'l', 'L' etc.
	 */
	public static boolean isNumberWithoutQualifier(String value)
	{
		return NumberUtils.isNumber(value) && Character.isDigit(value.charAt(value.length() - 1));
	}

	public static void checkNumberOfParams(String function, String[] params, int minNumberOfParam, int maxNumberOfParam) throws ParametersException
	{
		if(!(params.length >= minNumberOfParam) || !(params.length <= maxNumberOfParam))
		{
			throw new ParametersException(String.format("Wrong number of parameters in function '%s', min = %s, max = %s.", function, minNumberOfParam, maxNumberOfParam));
		}
	}

	public static String[] removeQuotesAndSpaces(String[] entries)
	{
		for (int i = 0; i < entries.length; i++)
			entries[i] = removeQuotesAndSpaces(entries[i]);
		return entries;
	}
	
	public static String removeQuotesAndSpaces(String value)
	{
		value = StringUtils.trim(value);
		if (value.startsWith("'") && value.endsWith("'"))
			value = value.substring(1, value.length() - 1);
		return value;
	}
}
