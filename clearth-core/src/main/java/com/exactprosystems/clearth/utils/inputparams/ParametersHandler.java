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

import static java.util.Collections.addAll;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.split;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.exactprosystems.clearth.ClearThCore;
import static com.exactprosystems.clearth.utils.ClearThEnumUtils.valueOfIgnoreCase;
import static com.exactprosystems.clearth.utils.ClearThEnumUtils.enumToTextValues;

import java.io.File;
/**
 * Use case:
 * 
 * A.
 *     ParametersHandler handler = new ParametersHandler(inputParams);
 *     try
 *     {
 *         String name = handler.getRequiredString("Name");
 *         Date date = handler.getRequiredDate("Start Date", "yyyyMMdd");
 *         Long count = handler.getRequiredLong("Total Count");
 *     }
 *     finally
 *     {
 *         handler.check();
 *     }
 *     
 * B.
 *     ParametersHandler handler = new ParametersHandler(inputParams);
 *     
 *     String name = handler.getRequiredString("Name");
 *     Date date = handler.getRequiredDate("Start Date", "yyyyMMdd");
 *     Long count = handler.getRequiredLong("Total Count");
 *     
 *     String errorMessage = handler.getErrorMessage();
 *     if (errorMessage != null)
 *         throw new MyException("Invalid settings in 'myConfig.csv': " + errorMessage);
 *
 */
public class ParametersHandler
{
	public static final Collection<String> BOOLEAN_VALUES = new LinkedHashSet<String>();
	static 
	{
		BOOLEAN_VALUES.addAll(InputParamsUtils.YES);
		BOOLEAN_VALUES.addAll(InputParamsUtils.NO);
	}

	protected static final String DEFAULT_DELIMITER = ";";
	protected static final int POSITIVE = 1;
	protected static final int NEGATIVE = -1;
	protected static final int NON_NEGATIVE = 0;

	protected final Map<String, String> params;
	protected final List<ValidationError> errors = new ArrayList<ValidationError>();

	public ParametersHandler(Map<String, String> params)
	{
		this.params = params;
	}
	
	// String //////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getRequiredString(String name)
	{
		return getString(name, true);
	}
	
	public String getRequiredString(String name, Collection<String> possibleValues)
	{
		return getString(name, null, true, possibleValues, false);
	}

	public String getRequiredString(String name, Collection<String> possibleValues, boolean matchCase)
	{
		return getString(name, null, true, possibleValues, matchCase);
	}
	
	public String getString(String name)
	{
		return getString(name, false);
	}
	
	public String getString(String name, String defaultValue)
	{
		return getString(name, defaultValue, false);
	}
	
	public String getString(String name, Collection<String> possibleValues)
	{
		return getString(name, null, false, possibleValues, false);
	}
	
	public String getString(String name, String defaultValue, Collection<String> possibleValues)
	{
		return getString(name, defaultValue, false, possibleValues, false);
	}

	public String getString(String name, Collection<String> possibleValues, boolean matchCase)
	{
		return getString(name, null, false, possibleValues, matchCase);
	}
	
	private String getString(String name, boolean required)
	{
		return getString(name, (String)null, required);
	}
	
	private String getString(String name, String defaultValue, boolean required)
	{
		String value = params.get(name);
		if (StringUtils.isEmpty(value))
		{
			if (required)
				errors.add(new RequiredParameterError(name));
			else 
				value = defaultValue != null ? defaultValue : value;
		}
		return value;
	}
	
	private String getString(String name, String defaultValue, boolean required, Collection<String> possibleValues, 
	                         boolean matchCase)
	{
		String value = getString(name, required);
		if (hasError(name))
			return null;		
		if (StringUtils.isEmpty(value))
			return defaultValue;
		
		if (matchCase)
		{
			if (possibleValues.contains(value))
				return value;
		}
		else 
		{
			for (String possible : possibleValues)
			{
				if (possible.equalsIgnoreCase(value))
					return possible;
			}
		}
		errors.add(new UnexpectedValueError(name, value, possibleValues));
		return null;
	}
	
	// Set /////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Set<String> getRequiredSet(String name)
	{
		return getSet(name, DEFAULT_DELIMITER, true);
	}
	
	public Set<String> getRequiredSet(String name, String delimiter)
	{
		return getSet(name, delimiter, true);
	}

	public Set<String> getRequiredSet(String name, Pattern delimiter)
	{
		return getSet(name, delimiter, true);
	}

	public Set<String> getSet(String name)
	{
		return getSet(name, DEFAULT_DELIMITER, false);
	}

	public Set<String> getSet(String name, String delimiter)
	{
		return getSet(name, delimiter, false);
	}

	public Set<String> getSet(String name, Pattern delimiter)
	{
		return getSet(name, delimiter, false);
	}
	
	private Set<String> getSet(String name, String delimiter, boolean required)
	{
		Set<String> set = new LinkedHashSet<String>();
		findAndSplit(set, name, delimiter, required);
		return set;
	}

	private Set<String> getSet(String name, Pattern delimiter, boolean required)
	{
		Set<String> set = new LinkedHashSet<String>();
		findAndSplit(set, name, delimiter, required);
		return set;
	}

	// Map /////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Map<String, String> getParamsMap(String name, String pairDelimiter, boolean required)
	{
		Map<String, String> resultMap = new LinkedHashMap<String, String>();
		String value = getString(name, required);
		if (isNotEmpty(value)) {
			Set<String> pairs = getSet(name, pairDelimiter, required);
			for (String pairStr : pairs) {
				Pair<String, String> pair = KeyValueUtils.parseKeyValueString(pairStr);
				resultMap.put(pair.getFirst(), pair.getSecond());
			}
		}

		return resultMap;
	}


	// List ////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public List<String> getRequiredList(String name)
	{
		return getList(name, DEFAULT_DELIMITER, true);
	}
	
	public List<String> getRequiredList(String name, String delimiter)
	{
		return getList(name, delimiter, true);
	}

	public List<String> getRequiredList(String name, Pattern delimiter)
	{
		return getList(name, delimiter, true);
	}

	public List<String> getList(String name)
	{
		return getList(name, DEFAULT_DELIMITER, false);
	}

	public List<String> getList(String name, String delimiter)
	{
		return getList(name, delimiter, false);
	}

	public List<String> getList(String name, Pattern delimiter)
	{
		return getList(name, delimiter, false);
	}
	
	private List<String> getList(String name, String delimiter, boolean required)
	{
		List<String> list = new ArrayList<String>();
		findAndSplit(list, name, delimiter, required);
		return list;
	}

	private List<String> getList(String name, Pattern delimiter, boolean required)
	{
		List<String> list = new ArrayList<String>();
		findAndSplit(list, name, delimiter, required);
		return list;
	}

	// Collection //////////////////////////////////////////////////////////////////////////////////////////////////////

	private void findAndSplit(Collection<String> collection, String name, String delimiter, boolean required)
	{
		String value = getString(name, required);
		if (isNotEmpty(value))
		{
			String[] values = split(value, delimiter);
			for (int index = 0; index < values.length; index++)
			{
				values[index] = StringUtils.trim(values[index]);
			}
			addAll(collection, values);
		}
	}

	private void findAndSplit(Collection<String> collection, String name, Pattern delimiter, boolean required)
	{
		String value = getString(name, required);
		if (isNotEmpty(value))
		{
			String[] values = delimiter.split(value);
			for (int index = 0; index < values.length; index++)
			{
				values[index] = StringUtils.trim(values[index]);
			}
			addAll(collection, values);
		}
	}

	// Date ////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Date getRequiredDate(String name, String format)
	{
		return getDate(name, null, format, true);
	}
	
	public String getRequiredDateAsString(String name, String format)
	{
		return getDateAsString(name, null, format, true);
	}
	
	public Date getDate(String name, String format)
	{
		return getDate(name, null, format, false);
	}
	
	public Date getDate(String name, Date defaultValue, String format)
	{
		return getDate(name, defaultValue, format, false);
	}
	
	public String getDateAsString(String name, String format)
	{
		return getDateAsString(name, null, format, false);
	}
	
	public String getDateAsString(String name, String defaultValue, String format)
	{
		return getDateAsString(name, defaultValue, format, false);
	}
	
	private Date getDate(String name, Date defaultValue, String format, boolean required)
	{
		return (Date) getDate(name, defaultValue, format, required, true);
	}
	
	private String getDateAsString(String name, String defaultValue, String format, boolean required)
	{
		return (String) getDate(name, defaultValue, format, required, false);
	}
	
	private Object getDate(String name, Object defaultValue, String format, boolean required, boolean returnDate)
	{
		String value = getString(name, required);
		if (hasError(name))
			return null;
		if (StringUtils.isEmpty(value))
			return defaultValue;

		try
		{
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			sdf.setLenient(false);
			Date date = sdf.parse(value);
			return returnDate ? date : sdf.format(date);
		}
		catch (ParseException e)
		{
			errors.add(new FormatError(name, value, format));
			return null;
		}
	}

	public LocalDate getRequiredLocalDate(String name, String pattern)
	{
		return (LocalDate) getTemporal(name, pattern, LocalDate::parse, null, true);
	}

	public LocalDate getLocalDate(String name, String pattern)
	{
		return (LocalDate) getTemporal(name, pattern, LocalDate::parse, null, false);
	}

	public LocalDate getLocalDate(String name, String pattern, LocalDate defaultValue)
	{
		return (LocalDate) getTemporal(name, pattern, LocalDate::parse, defaultValue, false);
	}

	public LocalTime getRequiredLocalTime(String name, String pattern)
	{
		return (LocalTime) getTemporal(name, pattern, LocalTime::parse, null, true);
	}

	public LocalTime getLocalTime(String name, String pattern)
	{
		return (LocalTime) getTemporal(name, pattern, LocalTime::parse, null, false);
	}

	public LocalTime getLocalTime(String name, String pattern, LocalTime defaultValue)
	{
		return (LocalTime) getTemporal(name, pattern, LocalTime::parse, defaultValue, false);
	}

	public LocalDateTime getRequiredLocalDateTime(String name, String pattern)
	{
		return (LocalDateTime) getTemporal(name, pattern, LocalDateTime::parse, null, true);
	}

	public LocalDateTime getLocalDateTime(String name, String pattern)
	{
		return (LocalDateTime) getTemporal(name, pattern, LocalDateTime::parse, null, false);
	}

	public LocalDateTime getLocalDateTime(String name, String pattern, LocalDateTime defaultValue)
	{
		return (LocalDateTime) getTemporal(name, pattern, LocalDateTime::parse, defaultValue, false);
	}

	public ZonedDateTime getRequiredZonedDateTime(String name, String pattern, ZoneId zone)
	{
		return getZonedDateTime(name, pattern, zone, null, true);
	}

	public ZonedDateTime getZonedDateTime(String name, String pattern, ZoneId zone)
	{
		return getZonedDateTime(name, pattern, zone, null, false);
	}

	public ZonedDateTime getZonedDateTime(String name, String pattern, ZoneId zone, ZonedDateTime defaultValue)
	{
		return getZonedDateTime(name, pattern, zone, defaultValue, false);
	}

	private ZonedDateTime getZonedDateTime(String name,
	                                       String pattern,
	                                       ZoneId zone,
	                                       ZonedDateTime defaultValue,
	                                       boolean required)
	{
		String value = getString(name, required);
		if (hasError(name))
			return null;
		if (StringUtils.isEmpty(value))
			return defaultValue;

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		try
		{
			LocalDateTime localDate = LocalDateTime.parse(value, formatter);
			return ZonedDateTime.of(localDate, zone);
		}
		catch (DateTimeParseException e)
		{
			errors.add(new FormatError(name, value, pattern));
			return null;
		}
	}

	private Temporal getTemporal(String name,
	                             String pattern,
	                             BiFunction<String, DateTimeFormatter, Temporal> parser,
	                             Temporal defaultValue,
	                             boolean required)
	{
		String value = getString(name, required);
		if (hasError(name))
			return null;
		if (StringUtils.isEmpty(value))
			return defaultValue;

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		try
		{
			return parser.apply(value, formatter);
		}
		catch (DateTimeParseException e)
		{
			errors.add(new FormatError(name, value, pattern));
			return null;
		}
	}


	// Double //////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Double getRequiredNonNegativeDouble(String name)
	{
		return getBoundedDouble(name, null, true, NON_NEGATIVE);
	}

	public Double getNonNegativeDouble(String name)
	{
		return getBoundedDouble(name, null, false, NON_NEGATIVE);
	}

	public Double getNonNegativeDouble(String name, Double defaultValue)
	{
		return getBoundedDouble(name, defaultValue, false, NON_NEGATIVE);
	}

	public Double getRequiredPositiveDouble(String name)
	{
		return getBoundedDouble(name, null, true, POSITIVE);
	}

	public Double getPositiveDouble(String name)
	{
		return getBoundedDouble(name, null, false, POSITIVE);
	}

	public Double getPositiveDouble(String name, Double defaultValue)
	{
		return getBoundedDouble(name, defaultValue, false, POSITIVE);
	}

	private Double getBoundedDouble(String name, Double defaultValue, boolean required, int type)
	{
		return (Double) getBoundedNumber(name, defaultValue, required, Double.class, "float number", type);
	}


	public Double getRequiredDouble(String name)
	{
		return getDouble(name, null, true);
	}

	public Double getDouble(String name)
	{
		return getDouble(name, null, false);
	}
	
	public Double getDouble(String name, Double defaultValue)
	{
		return getDouble(name, defaultValue, false);
	}
	
	private Double getDouble(String name, Double defaultValue, boolean required)
	{
		return (Double) getNumber(name, defaultValue, required, Double.class, "float number");
	}

//	public Double getRequiredDouble(String name, double from, double to)

	// Integer /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Integer getRequiredNonNegativeInteger(String name)
	{
		return getBoundedInteger(name, null, true, NON_NEGATIVE);
	}

	public Integer getNonNegativeInteger(String name)
	{
		return getBoundedInteger(name, null, false, NON_NEGATIVE);
	}

	public Integer getNonNegativeInteger(String name, Integer defaultValue)
	{
		return getBoundedInteger(name, defaultValue, false, NON_NEGATIVE);
	}

	public Integer getRequiredPositiveInteger(String name)
	{
		return getBoundedInteger(name, null, true, POSITIVE);
	}

	public Integer getPositiveInteger(String name)
	{
		return getBoundedInteger(name, null, false, POSITIVE);
	}

	public Integer getPositiveInteger(String name, Integer defaultValue)
	{
		return getBoundedInteger(name, defaultValue, false, POSITIVE);
	}

	private Integer getBoundedInteger(String name, Integer defaultValue, boolean required, int type)
	{
		return (Integer) getBoundedNumber(name, defaultValue, required, Integer.class, "integer", type);
	}
	
	public Integer getRequiredInteger(String name)
	{
		return getInteger(name, true);
	}
	
	public Integer getRequiredInteger(String name, Collection<Integer> possibleValues)
	{
		return getInteger(name, null, true, possibleValues);
	}

	public Integer getInteger(String name)
	{
		return getInteger(name, false);
	}
	
	public Integer getInteger(String name, Integer defaultValue)
	{
		return getInteger(name, defaultValue, false);
	}
	
	public Integer getInteger(String name, Collection<Integer> possibleValues)
	{
		return getInteger(name, null, false, possibleValues);
	}
	
	public Integer getInteger(String name, Integer defaultValue, Collection<Integer> possibleValues)
	{
		return getInteger(name, defaultValue, false, possibleValues);
	}

	private Integer getInteger(String name, Integer defaultValue, boolean required)
	{
		return (Integer) getNumber(name, defaultValue, required, Integer.class, "integer");
	}
	
	private Integer getInteger(String name, boolean required)
	{
		return getInteger(name, null, required);
	}
	
	private Integer getInteger(String name, Integer defaultValue, boolean required, Collection<Integer> possibleValues)
	{
		Integer value = getInteger(name, required);
		if (hasError(name))
			return null;
		if (value == null)
			return defaultValue;
		
		if (possibleValues.contains(value))
			return value;
		else 
		{
			errors.add(new UnexpectedValueError(name, value, possibleValues));
			return null;
		}
	}

//	public Integer getRequiredInteger(String name, long from, long to)
	
	// Boolean /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Boolean getRequiredBooleanOrNull(String name)
	{
		return getBooleanOrNull(name, true);
	}
	
	public boolean getRequiredBoolean(String name, boolean defaultValue)
	{
		return getBoolean(name, true, defaultValue);
	}

	public Boolean getBooleanOrNull(String name)
	{
		return getBooleanOrNull(name, false);
	}
	
	public boolean getBoolean(String name, boolean defaultValue)
	{
		return getBoolean(name, false, defaultValue);
	}

	private Boolean getBooleanOrNull(String name, boolean required)
	{
		String value = getString(name, required);
		if (hasError(name) || StringUtils.isEmpty(value))
			return null;
		String lowerCaseNotNullValue = value.toLowerCase(); 

		if (InputParamsUtils.YES.contains(lowerCaseNotNullValue))
			return true;
		if (InputParamsUtils.NO.contains(lowerCaseNotNullValue))
			return false;
		
		if (required)
			errors.add(new UnexpectedValueError(name, value, BOOLEAN_VALUES));
		
		return null;
	}
	
	private boolean getBoolean(String name, boolean required, boolean defaultValue)
	{
		String value = getString(name, required);
		if (hasError(name) || StringUtils.isEmpty(value))
			return defaultValue;
		
		String lowerCaseValue = value.toLowerCase();
		
		if (InputParamsUtils.YES.contains(lowerCaseValue))
			return true;
		if (InputParamsUtils.NO.contains(lowerCaseValue))
			return false;
		
		errors.add(new UnexpectedValueError(name, value, BOOLEAN_VALUES));
		return defaultValue;
	}

	// BigDecimal /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public BigDecimal getRequiredNonNegativeBigDecimal(String name)
	{
		return getBoundedBigDecimal(name, null, true, NON_NEGATIVE);
	}

	public BigDecimal getNonNegativeBigDecimal(String name)
	{
		return getBoundedBigDecimal(name, null, false, NON_NEGATIVE);
	}

	public BigDecimal getNonNegativeBigDecimal(String name, BigDecimal defaultValue)
	{
		return getBoundedBigDecimal(name, defaultValue, false, NON_NEGATIVE);
	}

	public BigDecimal getRequiredPositiveBigDecimal(String name)
	{
		return getBoundedBigDecimal(name, null, true, POSITIVE);
	}

	public BigDecimal getPositiveBigDecimal(String name)
	{
		return getBoundedBigDecimal(name, null, false, POSITIVE);
	}

	public BigDecimal getPositiveBigDecimal(String name, BigDecimal defaultValue)
	{
		return getBoundedBigDecimal(name, defaultValue, false, POSITIVE);
	}

	private BigDecimal getBoundedBigDecimal(String name, BigDecimal defaultValue, boolean required, int type)
	{
		return (BigDecimal) getBoundedNumber(name, defaultValue, required, BigDecimal.class, "BigDecimal", type);
	}
	
	public BigDecimal getRequiredBigDecimal(String name)
	{
		return getBigDecimal(name, null, true);
	}
	
	public BigDecimal getBigDecimal(String name)
	{
		return getBigDecimal(name, null, false);
	}
	
	public BigDecimal getBigDecimal(String name, BigDecimal defaultValue)
	{
		return getBigDecimal(name, defaultValue, false);
	}

	private BigDecimal getBigDecimal(String name, BigDecimal defaultValue, boolean required)
	{
		return (BigDecimal) getNumber(name, defaultValue, required, BigDecimal.class, "BigDecimal");
	}

	// Long ////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public Long getRequiredNonNegativeLong(String name)
	{
		return getBoundedLong(name, null, true, NON_NEGATIVE);
	}

	public Long getNonNegativeLong(String name)
	{
		return getBoundedLong(name, null, false, NON_NEGATIVE);
	}

	public Long getNonNegativeLong(String name, Long defaultValue)
	{
		return getBoundedLong(name, defaultValue, false, NON_NEGATIVE);
	}
	
	public Long getRequiredPositiveLong(String name)
	{
		return getBoundedLong(name, null, true, POSITIVE);
	}

	public Long getPositiveLong(String name)
	{
		return getBoundedLong(name, null, false, POSITIVE);
	}

	public Long getPositiveLong(String name, Long defaultValue)
	{
		return getBoundedLong(name, defaultValue, false, POSITIVE);
	}

	private Long getBoundedLong(String name, Long defaultValue, boolean required, int type)
	{
		return (Long) getBoundedNumber(name, defaultValue, required, Long.class, "long integer", type);
	}
	
	public Long getRequiredLong(String name)
	{
		return getLong(name, null, true);
	}
	
	public Long getLong(String name)
	{
		return getLong(name, null, false);
	}
	
	public Long getLong(String name, Long defaultValue)
	{
		return getLong(name, defaultValue, false);
	}
	
	private Long getLong(String name, Long defaultValue, boolean required)
	{
		return (Long) getNumber(name, defaultValue, required, Long.class, "long integer");
	}
	
	// Number //////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <N extends Number> Number getBoundedNumber(String name, N defaultValue, boolean required, Class<N> clazz, String format, int type)
	{
		Number number = getNumber(name, defaultValue, required, clazz, format);

		if (hasError(name) || number == null)
			return null;

		boolean success = false;
		int sigNum = new BigDecimal(number.toString()).signum();

		switch (type)
		{
			case POSITIVE: success = sigNum == POSITIVE; break;
			case NEGATIVE: success = sigNum == NEGATIVE; break;
			case NON_NEGATIVE: success = sigNum >= NON_NEGATIVE; break;
		}

		if(!success)
			errors.add(new FormatError(name, String.valueOf(number), format));

		return number;
	}

	private <N extends Number> Number getNumber(String name, N defaultValue, boolean required, Class<N> clazz, String format)
	{
		String value = getString(name, required);
		if (hasError(name))
			return null;
		if (StringUtils.isEmpty(value))
			return defaultValue;
		
		if (NumberUtils.isNumber(value))
		{
			try
			{
				if (clazz == Double.class)
					return Double.parseDouble(value);
				else if (clazz == Integer.class)
					return Integer.parseInt(value);
				else if (clazz == Long.class)
					return Long.parseLong(value);
				else if (clazz == BigDecimal.class)
					return new BigDecimal(value);
			}
			catch (NumberFormatException e) { /* Do nothing */ }
		}
		errors.add(new FormatError(name, value, format));
		return null;
	}
	
	// File ///////////////////////////////////////////////////////////////////////////////////////////////////
	
	private File getFile(String name, String defaultValue, boolean required)
	{
		return new File(getFilePath(name, defaultValue, required));
	}

	public File getRequiredFile(String name) 
	{
		return getFile(name, null, true);
	}
	
	public File getFile(String name) 
	{
		return getFile(name, null, false);
	}
	
	public File getFile(String name, String defaultValue) 
	{
		return getFile(name, defaultValue, false);
	}
	
	public File getFile(String name, File defaultValue) 
	{
		String defaultPath = defaultValue != null ? defaultValue.getPath() : null;
		return getFile(name, defaultPath, false);
	}
	
	// FilePath ///////////////////////////////////////////////////////////////////////////////////////////////////
	
	private String getFilePath(String name, String defaultValue, boolean required)
	{
		String fileName = getString(name, defaultValue, required);
		if(fileName == null)
			fileName = "";
		return ClearThCore.rootRelative(fileName);
	}
	
	public String getRequiredFilePath(String name) 
	{
		return getFilePath(name, null, true);
	}
	
	public String getFilePath(String name) 
	{
		return getFilePath(name, null, false);
	}
	
	public String getFilePath(String name, String defaultValue) 
	{
		return getFilePath(name, defaultValue, false);
	}
	
	public String getFilePath(String name, File defaultValue) 
	{
		String defaultPath = defaultValue != null ? defaultValue.getPath() : null;
		return getFilePath(name, defaultPath , false);
	}
	
	// Enum ////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public <E extends Enum<E>> E getEnum(String name, Class<E> enumClass) 
	{
		return getEnum(name, enumClass, null, false);
	}

	/**
	 * <p>Gets the enum for the class, returning {@code defaultValue} if not found.</p>
	 *
	 * <p>This method differs from {@link ParametersHandler#getEnum} with specified defaultValue
	 * in that it does not throw an exception for an invalid enum name.</p>
	 *
	 * @param <E> the type of the enumeration
	 * @param enumClass  the class of the enum to query, not null
	 * @param name   the parameter which contains enum name
	 * @param defaultValue   the enum value which returns if enum name is not found 
	 * @return the enum, defaultValue if not found
	 */
	public <E extends Enum<E>> E getEnumOrDefault(String name, Class<E> enumClass, E defaultValue)
	{
		String enumStringValue = params.get(name);
		if (StringUtils.isEmpty(enumStringValue))
			return defaultValue;
		
		E foundedValue = valueOfIgnoreCase(enumClass, enumStringValue);
		return foundedValue == null ? defaultValue : foundedValue;
	}

	public <E extends Enum<E>> E getEnum(String name, Class<E> enumClass, E defaultValue) 
	{
		return getEnum(name, enumClass, defaultValue, false);
	}
	
	public <E extends Enum<E>> E getReqiuredEnum(String name, Class<E> enumClass) 
	{
		return getEnum(name, enumClass, null, true);
	}
	
	private <E extends Enum<E>> E getEnum(String name, Class<E> enumClass, E defaultValue, boolean required) 
	{
		String enumStringValue = params.get(name);
		if (isEmpty(enumStringValue))
		{
			if (required)
			{
				errors.add(new RequiredParameterError(name));
				return null;
			}
			else 
				return defaultValue;
		}
		
		// EnumUtils.getEnumIgnoreCase(Apache Commons Lang 3.8 API) could be used since the library updated to 3.8
		E value = valueOfIgnoreCase(enumClass, enumStringValue);
		if(value == null)
			errors.add(new UnexpectedValueError(name, enumStringValue, enumToTextValues(enumClass)));
		return value;	
	}
	
	// Checker /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getErrorMessage()
	{
		if (errors.isEmpty())
			return null;
		
		List<String> errorMessages = new ArrayList<>();

		List<RequiredParameterError> requiredParamErrors = filter(errors, RequiredParameterError.class);
		if (!requiredParamErrors.isEmpty())
			errorMessages.add("The following required parameters are empty: " + joinNames(requiredParamErrors));

		List<FormatError> formatErrors = filter(errors, FormatError.class);
		if (!formatErrors.isEmpty())
			errorMessages.add("The following parameters have incorrect format: " + joinNamesFormats(formatErrors));

		List<UnexpectedValueError> unexpectedValueErrors = filter(errors, UnexpectedValueError.class);
		if (!unexpectedValueErrors.isEmpty())
			errorMessages.add("The following parameters have unexpected values: " + joinNamesPossibleValues(unexpectedValueErrors));

		return !errorMessages.isEmpty() ? StringUtils.join(errorMessages, "; \n") : null;
	}
	
	public void check() throws ParametersException
	{
		String errorMessage = getErrorMessage();
		if (errorMessage != null)
			throw new ParametersException(errorMessage);
	}
	
	public void check(String... additionalNames) throws ParametersException
	{
		for (String name : additionalNames)
		{
			if (!params.containsKey(name))
				errors.add(new RequiredParameterError(name));
		}
		check();
	}
	
	// Internal methods ////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean hasError(String name)
	{
		return !errors.isEmpty() && StringUtils.equals(name, errors.get(errors.size() - 1).getName());
	}
	
	@SuppressWarnings("unchecked")
	private static <T, T2> List<T2> filter(List<T> objects, Class<T2> clazz)
	{
		List<T2> filtered = null;
		for (T o: objects) {
			if (clazz.isInstance(o))
			{
				if (filtered == null)
					filtered = new ArrayList<T2>();
				filtered.add((T2)o);
			}
		}
		return filtered == null ? Collections.<T2>emptyList() : filtered;
	}
	
	private static String joinNames(List<? extends ValidationError> errors)
	{
		StringBuilder sb = new StringBuilder();
		for(ValidationError e: errors)
		{
			if (sb.length() != 0)
				sb.append(", ");
			sb.append("'").append(e.getName()).append("'");
		}
		return sb.toString();
	}

	private static String joinNamesFormats(List<FormatError> errors)
	{
		StringBuilder sb = new StringBuilder();
		for(FormatError e: errors)
		{
			if (sb.length() != 0)
				sb.append(", ");
			sb.append("'").append(e.getName()).append("' (").append(e.getFormat()).append(")");
		}
		return sb.toString();
	}
	
	private static String joinNamesPossibleValues(List<UnexpectedValueError> errors)
	{
		CommaBuilder cb = new CommaBuilder();
		for (UnexpectedValueError e : errors)
		{
			cb.append(String.format("'%s' (actual = '%s', possible values: '%s')",
					e.getName(), e.getValue(), StringUtils.join(e.getPossibleValues(), "', '")));
		}
		return cb.toString();
	}
}
