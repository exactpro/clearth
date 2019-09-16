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

package com.exactprosystems.clearth.utils.inputparams;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import static com.exactprosystems.clearth.utils.ClearThEnumUtils.valueOfIgnoreCase;
import static com.exactprosystems.clearth.utils.ClearThEnumUtils.enumToTextValues;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class InputParamsUtils
{
	public static final List<String> YES = Arrays.asList("y", "yes", "true", "1"),
			NO = Arrays.asList("n", "no", "false", "0");
	public static final String ERROR_PARAM_MISSING = "Required input parameter '%s' is not filled in matrix";

	private InputParamsUtils()
	{
	}

	public static String getRequiredString(Map<String, String> inputParams, String key) throws ResultException
	{
		String value = inputParams.get(key);
		if (isEmpty(value))
			throw ResultException.failed(String.format(ERROR_PARAM_MISSING, key));
		return value;
	}

	public static String getStringOrDefault(Map<String, String> inputParams, String key, String defaultValue)
	{
		String value = inputParams.get(key);
		if (isEmpty(value))
			return defaultValue;
		return value;
	}

	public static Boolean getBoolean(Map<String, String> inputParams, String key)
	{
		String value = inputParams.get(key);
		if (value != null)
			value = value.toLowerCase();
		
		if (YES.contains(value))
			return true;
		if (NO.contains(value))
			return false;
		return null;
	}

	public static boolean getRequiredBoolean(Map<String, String> inputParams, String key) throws ResultException
	{
		String value = getRequiredString(inputParams, key);
		if (YES.contains(value))
			return true;
		if (NO.contains(value))
			return false;
		throw ResultException.failed("Required input parameter '" + key + "' has incorrect value. Logical parameters can use the following values: " +
				StringUtils.join(YES, ", ") + ", " + StringUtils.join(NO, ", "));
	}

	public static boolean getBooleanOrDefault(Map<String, String> inputParams, String key, boolean defaultValue)
	{
		Boolean result = getBoolean(inputParams, key);
		if (result == null)
			return defaultValue;
		return result;
	}

	public static int getRequiredInt(Map<String, String> inputParams, String key) throws ResultException
	{
		String value = getRequiredString(inputParams, key);
		return parseInt(key, value);
	}

	public static int getIntOrDefault(Map<String, String> inputParams, String key, int defaultValue) throws ResultException
	{
		String value = inputParams.get(key);
		if (isEmpty(value))
			return defaultValue;
		return parseInt(key, value);
	}

	private static int parseInt(String key, String value) throws ResultException
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch(NumberFormatException e)
		{
			throw ResultException.failed("Required input parameter '" + key + "' has incorrect value: "+value+". " +
					"It should be an integer value", e);
		}
	}

	public static double getRequiredDouble(Map<String, String> inputParams, String key) throws ResultException
	{
		String value = getRequiredString(inputParams, key);
		return parseDouble(key, value);
	}

	public static double getDoubleOrDefault(Map<String, String> inputParams, String key, double defaultValue) throws ResultException
	{
		String value = inputParams.get(key);
		if (isEmpty(value))
			return defaultValue;
		return parseDouble(key, value);
	}

	private static double parseDouble(String key, String value) throws ResultException
	{
		try
		{
			return Double.parseDouble(value);
		}
		catch(NumberFormatException e)
		{
			throw ResultException.failed("Required input parameter '" + key + "' has incorrect value: "+value+". " +
					"It should be a decimal value", e);
		}
	}

	public static long getRequiredLong(Map<String, String> inputParams, String key) throws ResultException
	{
		String value = getRequiredString(inputParams, key);
		return parseLong(key, value);
	}

	public static long getLongOrDefault(Map<String, String> inputParams, String key, long defaultValue) throws ResultException
	{
		String value = inputParams.get(key);
		if (isEmpty(value))
			return defaultValue;
		return parseLong(key, value);
	}

	private static long parseLong(String key, String value) throws ResultException
	{
		try
		{
			return Long.parseLong(value);
		}
		catch(NumberFormatException e)
		{
			throw ResultException.failed("Required input parameter '" + key + "' has incorrect value: "+value+". " +
					"It should be an integer value", e);
		}
	}

	public static Date getDateOrDefault(Map<String, String> inputParams, String key, String format, Date defaultValue) throws ResultException
	{
		String dateStr = inputParams.get(key);
		if (StringUtils.isEmpty(dateStr))
			return defaultValue;

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		try
		{
			return sdf.parse(dateStr);
		}
		catch (ParseException e)
		{
			throw ResultException.failed("Input parameter '" + key + "' has incorrect date format. Please use " + format + " format.");
		}
	}

	public static File getFile(Map<String, String> inputParams, String key)
	{
		return getFileOrDefault(inputParams, key, (String)null);
	}
	
	public static File getFileOrDefault(Map<String, String> inputParams, String key, String defaultValue) 
	{
		return new File(getFilePathOrDefault(inputParams, key, defaultValue));
	}
	
	public static File getFileOrDefault(Map<String, String> inputParams, String key, File defaultValue) 
	{
		String defaultPath = defaultValue != null ? defaultValue.getPath() : null;
		return new File(getFilePathOrDefault(inputParams, key, defaultPath));
	}
	
	public static File getRequiredFile(Map<String, String> inputParams, String key) throws ResultException
	{
		return new File(getRequiredFilePath(inputParams, key));		
	}
	
	public static String getFilePath(Map<String, String> inputParams, String key)
	{
		return getFilePathOrDefault(inputParams, key, (String)null);
	}
	
	public static String getFilePathOrDefault(Map<String, String> inputParams, String key, String defaultValue) 
	{
		String fileName = inputParams.get(key);
		if (isEmpty(fileName)) 
			fileName = defaultValue != null ? defaultValue : "";
		return ClearThCore.rootRelative(fileName);
	}
	
	public static String getFilePathOrDefault(Map<String, String> inputParams, String key, File defaultValue) 
	{
		String defaultPath = defaultValue != null ? defaultValue.getPath() : null;
		return getFilePathOrDefault(inputParams, key, defaultPath);
	}
	
	public static String getRequiredFilePath(Map<String, String> inputParams, String key) throws ResultException
	{
		String fileName = getRequiredString(inputParams, key);
		return ClearThCore.rootRelative(fileName);
	}
	
	public static <E extends Enum<E>> E getRequiredEnum(Map<String, String> inputParams,
														String key,
														Class<E> enumClass) throws ResultException 
	{
		return getEnum(inputParams, key, enumClass, null, true);			
	}
	
	public static <E extends Enum<E>> E getEnum(Map<String, String> inputParams,
												String key,
												Class<E> enumClass) throws ResultException 
	{
		return getEnum(inputParams, key, enumClass, null, false);	
	}
	
	public static <E extends Enum<E>> E getEnum(Map<String, String> inputParams,
												String key,
												Class<E> enumClass,
												E defaultValue) throws ResultException 
	{
		return getEnum(inputParams, key, enumClass, defaultValue, false);	
	}
	
	private static <E extends Enum<E>> E getEnum(Map<String, String> inputParams,
												String key,
												Class<E> enumClass,
												E defaultValue,
												boolean required) throws ResultException 
	{
		String enumStringValue = inputParams.get(key);
		if (isEmpty(enumStringValue))
		{
			if (required)
				throw ResultException.failed(String.format(ERROR_PARAM_MISSING, key));
			return defaultValue;
		}
		
		// EnumUtils.getEnumIgnoreCase(Apache Commons Lang 3.8 API) could be used since the library updated to 3.8
		E value = valueOfIgnoreCase(enumClass, enumStringValue);
		if(value != null)
			return value;		
		throw ResultException.failed("Input parameter '" 
					+ key 
					+ "' has incorrect format. It should be one of the following (ignoring case): " 
					+ enumToTextValues(enumClass));
	}
}
