/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.http;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;

import static com.exactprosystems.clearth.utils.StringOperationUtils.splitByCommasToSet;
import static com.exactprosystems.clearth.utils.Utils.nvl;
import static java.lang.String.format;
import static java.lang.String.join;

public class HttpParameterValidator
{
	private static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER = 
			DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_FORMAT);
	
	private final String paramName;
	private final HttpParameterType type;
	private final Set<String> possibleValues;
	private final BigDecimal minValue;
	private final BigDecimal maxValue;
	private final Integer precision;
	private final String format;

	public HttpParameterValidator(HttpParameter parameter)
	{
		this(parameter.getName(), parameter.getType(), parameter.getPossibleValues(),
				parameter.getMinValue(), parameter.getMaxValue(), parameter.getPrecision(),
				parameter.getFormat());
	}

	public HttpParameterValidator(String paramName, HttpParameterType type, String possibleValuesList,
	                              BigDecimal minValue, BigDecimal maxValue, Integer precision, String format) 
	{
		this.paramName = paramName;
		this.type = nvl(type, HttpParameterType.TEXT);
		this.possibleValues = StringUtils.isNotBlank(possibleValuesList) ? splitByCommasToSet(possibleValuesList) : null;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.precision = precision;
		this.format = format;
	}
	

	public String check(String value) 
	{
		switch (type)
		{
			case TEXT: return checkTextParameter(value);
			case TIMESTAMP: return checkTimestampParameter(value);
			case INTEGER: return checkIntegerParameter(value);
			case DECIMAL: return checkDecimalParameter(value);
			default: return null;
		}
	}

	private String checkTextParameter(String value)
	{
		if ((possibleValues != null) && !possibleValues.contains(value))
			return format("'%s' = '%s' isn't in list [%s].",
					paramName, value, join(", ", possibleValues));
		else
			return null;
	}

	private String checkTimestampParameter(String value)
	{
		DateTimeFormatter formatter;
		if (StringUtils.isNotBlank(format))
		{
			try
			{
				formatter = DateTimeFormatter.ofPattern(format);
			}
			catch (IllegalArgumentException e)
			{
				return format("Invalid format '%s' of timestamp parameter '%s': %s", format, paramName, e.getMessage());
			}
		}
		else
			formatter = DEFAULT_TIMESTAMP_FORMATTER;

		try
		{
			formatter.parse(value);
			return null;
		}
		catch (DateTimeParseException ignored)
		{
			return format("'%s' = '%s' doesn't fit to format '%s'.", paramName, value,
					StringUtils.isNotBlank(format) ? format : DEFAULT_TIMESTAMP_FORMAT);
		}
	}

	private String checkIntegerParameter(String value)
	{
		try
		{
			long number = Long.parseLong(value);
			return checkNumericParameterRange(value, new BigDecimal(number));
		}
		catch (NumberFormatException e)
		{
			return format("'%s' = '%s' isn't valid integer number.", paramName, value);
		}
	}

	private String checkDecimalParameter(String value)
	{
		BigDecimal number;
		try
		{
			number = new BigDecimal(value);
		}
		catch (NumberFormatException e)
		{
			return format("'%s' = '%s' isn't valid decimal number.", paramName, value);
		}

		if ((precision != null) && (number.scale() > precision))
			return format("'%s' = '%s' has precision more than %s.", paramName, value, precision);
		
		return checkNumericParameterRange(value, number);
	}

	private String checkNumericParameterRange(String textValue, BigDecimal value) 
	{
		if (((minValue == null) || value.compareTo(minValue) >= 0) &&
				((maxValue == null) || value.compareTo(maxValue) <= 0)) 
			return null;
		else
			return format("'%s' = '%s' is out of range [%s, %s].",
					paramName, textValue, nvl(minValue, "-Infinity"), nvl(maxValue, "+Infinity"));
	}
}
