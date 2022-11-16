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

import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.automation.exceptions.FunctionException;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.functions.MethodDataModel;
import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.utils.*;
import org.apache.commons.lang.StringUtils;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.PropertyAccessException;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.*;
import java.util.*;

import static com.exactprosystems.clearth.ClearThCore.valueGenerators;
import static com.exactprosystems.clearth.ClearThCore.comparisonUtils;
import static org.apache.commons.lang.StringUtils.*;

public class MatrixFunctions
{
	protected static final String HOLIDAY_DATE_PATTERN = "yyyyMMdd";
	@SuppressWarnings("unused") // May be used in projects
	protected static final DateFormat holidayDF = new SimpleDateFormat(HOLIDAY_DATE_PATTERN);
	
	public static final String FORMULA_START = "@{",
			FORMULA_END = "}",
			PASSED_PARAM = "passed",
			FAIL_REASON_PARAM = "failReason",
			
			ERROR_TIME_FORMAT = "Could not parse '%s' as timestamp in format '%s'";
	
	protected static final String DEFAULT_VALUE_GENERATOR = "DefaultValueGenerator";
	protected static final int MAX_SHIFT = 100000;
	protected static final String HOLIDAY_HOLIDAY = "holiday", HOLIDAY_WEEKEND = "weekend", HOLIDAY_ANY = "any";
	protected static final List<String> POSSIBLE_HOLIDAY_TYPES = Arrays.asList(HOLIDAY_HOLIDAY, HOLIDAY_WEEKEND, HOLIDAY_ANY);
	
	protected final Map<String, Boolean> holidays;
	protected final Date businessDay;
	protected Calendar baseTime, baseTimeChanged;
	protected final Map<String, ValueGenerator> valueGenerators;
	protected final ParserContext functionsContext;

	protected Action currentAction = null;
	protected boolean weekendHoliday;
	protected volatile Calendar currentTime;

	protected final Object weekendHolidayMonitor = new Object();
	protected final MvelExpressionValidator validator = new MvelExpressionValidator();

	public MatrixFunctions(Map<String, Boolean> holidays, Date businessDay, Date baseTime, boolean weekendHoliday, ValueGenerator valueGenerator)
	{
		this.holidays = (holidays != null) ? holidays : new HashMap<String, Boolean>();
		this.businessDay = businessDay;
		setBaseTime(baseTime);
		this.weekendHoliday = weekendHoliday;
		valueGenerators = new HashMap<String, ValueGenerator>();
		valueGenerators.put(DEFAULT_VALUE_GENERATOR, valueGenerator);
		
		functionsContext = new ParserContext();
		functionsContext.addImport(this.getClass());
	}
	
	protected void shiftError(int shift, String shiftName) throws FunctionException
	{
		throw new FunctionException("Too large shift ("+shift+" "+shiftName+") specified.");
	}
	
	protected int maxNumberOfIterations()
	{
		return 100;
	}

	protected Calendar getCalendar()
	{
		return Calendar.getInstance();
	}

	public String asNumber(String expected){
		return String.format("%s'%s')}", ComparisonUtils.AS_NUMBER_START, expected);
	}

	public String asNumber(String expected, String precision){
		return String.format("%s'%s','%s')}", ComparisonUtils.AS_NUMBER_START, expected, precision);
	}

	public String asAbsNumber(String expected){
		return String.format("%s'%s')}", ComparisonUtils.AS_ABS_NUMBER_START, expected);
	}

	public String asAbsNumber(String expected, String precision){
		return String.format("%s'%s','%s')}", ComparisonUtils.AS_ABS_NUMBER_START, expected, precision);
	}

	public String pattern(String value)
	{
		return String.format("%s'%s')}", ComparisonUtils.PATTERN_START, value);
	}

	
	protected void applyTime(Calendar c, Calendar now)
	{
		if (baseTime == null)
		{
			c.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
			c.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
			c.set(Calendar.SECOND, now.get(Calendar.SECOND));
			c.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND));
		}
		else
		{
			c.set(Calendar.HOUR_OF_DAY, baseTime.get(Calendar.HOUR_OF_DAY));
			c.set(Calendar.MINUTE, baseTime.get(Calendar.MINUTE));
			c.set(Calendar.SECOND, baseTime.get(Calendar.SECOND));
			c.set(Calendar.MILLISECOND, baseTime.get(Calendar.MILLISECOND));
			
			int year = c.get(Calendar.YEAR),
					day = c.get(Calendar.DAY_OF_YEAR);
			c.setTimeInMillis(c.getTimeInMillis()+(now.getTimeInMillis()-baseTimeChanged.getTimeInMillis()));
			//Prevent change of day. Only time should be changed here!
			c.set(Calendar.YEAR, year);
			c.set(Calendar.DAY_OF_YEAR, day);
		}
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "int days",
			description = "Returns a number of milliseconds passed since January 1, 1970 till current business day shifted by a given number of days.",
			usage = "time(0)"
	)
	public long time(int days) throws FunctionException
	{
		return time(days, 0, 0);
	}

	@MethodDataModel(
			group = "Date and Time",
			args = "int days, int months",
			description = "The same as time(int days), but with ability to specify shift for months.",
			usage = "time(0,0)"
	)
	public long time(int days, int months) throws FunctionException
	{
		return time(days, months, 0);
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "int days, int months, int years",
			description = "The same as time(int days, int months), but with ability to specify shift for years.",
			usage = "time(0,0,0)"
	)
	public long time(int days, int months, int years) throws FunctionException
	{
		return time(days, months, years, 0);
	}

	@MethodDataModel(
			group = "Date and Time",
			args = "int days, int months, int years, int hours",
			description = "The same as time(int days, int months, int years), but with ability to specify shift for hours.",
			usage = "time(0,0,0,0)"
	)
	public long time(int days, int months, int years, int hours) throws FunctionException
	{
		Calendar c = getCalendar(),
				now;
		if (currentTime != null)
			now = currentTime;
		else
			now = getCalendar();
		
		if (businessDay != null)
			c.setTime(businessDay);
		
		applyTime(c, now);
		return getTime(c, days, months, years, hours);
	}

	@MethodDataModel(
			group = "Date and Time",
			args = "long time, int offset",
			description = "Shifts given time in milliseconds passed since January 1, 1970 by a given number of days, skipping holidays and weekends."
					+ "Treats all weekends as holidays disregarding 'Weekend is holiday' setting.",
			usage = "timeIncludingHolidays(time(0),3)"
	)
	public long timeIncludingHolidays(long time, int offset) throws FunctionException
	{
		synchronized (weekendHolidayMonitor)
		{
			boolean currentWeekendHoliday = weekendHoliday;
			if (!weekendHoliday)
				setWeekendHoliday(true);

			Calendar c = getCalendar();
			c.setTimeInMillis(time);
			time = getTime(c, offset, 0, 0, 0);

			setWeekendHoliday(currentWeekendHoliday);

			return time;
		}
	}

	@MethodDataModel(
			group = "Date and Time",
			args = "long date",
			description = "Returns next holiday date relative to specified one",
			usage = "holiday(0)"
	)
	public long holiday(long date) throws FunctionException
	{
		return holiday(date, HOLIDAY_WEEKEND, 0);
	}

	@MethodDataModel(
			group = "Date and Time",
			args = "long date, String holidayType",
			description = "The same as holiday(long date), but with ability to specify one of holiday type: \"weekend\", \"holiday\" or \"any\"",
			usage = "holiday(0, 'weekend')"
	)
	public long holiday(long date, String holidayType) throws FunctionException
	{
		return holiday(date, holidayType, 0);
	}

	@MethodDataModel(
			group = "Date and Time",
			args = "long date, int offset",
			description = "The same as holiday(long date), but with ability to specify number of holidays(with type 'weekend') to skip",
			usage = "holiday(0, 0)"
	)
	public long holiday(long date, int offset) throws FunctionException
	{
		return holiday(date, HOLIDAY_WEEKEND, offset);
	}

	@MethodDataModel(
			group = "Date and Time",
			args = "long date, String holidayType, int offset",
			description = "The same as holiday(long date, String holidayType), but with ability to specify number of holidays of holidayType to skip",
			usage = "holiday(0, 'weekend', 0)"
	)
	public long holiday(long date, String holidayType, int offset) throws FunctionException
	{
		// check holiday type
		if (StringUtils.isEmpty(holidayType))
			holidayType = HOLIDAY_WEEKEND;
		else if (!POSSIBLE_HOLIDAY_TYPES.contains(holidayType))
			throw new FunctionException("Wrong value of holiday type: possible values are: "+POSSIBLE_HOLIDAY_TYPES);

		// check offset
		if (offset > 100000 || offset < 0)
			throw new FunctionException("Offset should be in range from 0 to 100000");

		SimpleDateFormat holidayDF = new SimpleDateFormat(HOLIDAY_DATE_PATTERN);
		holidayDF.setLenient(false);
		
		// check if holidays set (for holiday type only)
		if (holidayType.equals(HOLIDAY_HOLIDAY))
		{
			int holidayCount = 0;
			for (Map.Entry<String, Boolean> entry : holidays.entrySet())
			{
				try
				{
					if (holidayDF.parse(entry.getKey()).getTime() >= date && entry.getValue())
						holidayCount++;
				}
				catch (ParseException e)
				{
					throw new FunctionException("Error while checking holiday", e);
				}
			}

			if (holidayCount < 1)
				throw new FunctionException("Unable to get next holiday: holidays are not specified in current scheduler");
			if (holidayCount < offset)
				throw new FunctionException("Unable to get next holiday: offset exceeds holidays count in current scheduler");
		}

		Calendar c = getCalendar();
		c.setTimeInMillis(date);

		String dayStr;
		int dow, skipped = 0;
		boolean checkHolidays = !holidayType.equals(HOLIDAY_WEEKEND),
				checkWeekends = !holidayType.equals(HOLIDAY_HOLIDAY);

		while (true)
		{
			dayStr = holidayDF.format(c.getTime());
			dow = c.get(Calendar.DAY_OF_WEEK);

			if (checkWeekends && (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) ||
					checkHolidays && holidays.containsKey(dayStr) && holidays.get(dayStr))
			{
				if (skipped == offset)
					return c.getTime().getTime();

				skipped++;
			}

			c.add(Calendar.DATE, 1);
		}
	}

	@MethodDataModel(
			group = "Date and Time",
			args = "int days",
			description = "Same as time(int days) function, but use current date and time of ClearTH host, not business day.",
			usage = "sysTime(0)"
	)
	public long sysTime(int days) throws FunctionException
	{
		return sysTime(days, 0, 0, 0);
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "int days, int months",
			description = "Same as time(int days, int months) function, but uses current date and time of ClearTH host, not business day.",
			usage = "sysTime(0,0)"
	)
	public long sysTime(int days, int months) throws FunctionException
	{
		return sysTime(days, months, 0, 0);
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "int days, int months, int years",
			description = "Same as time(int days, int months, int years) function, but uses current date and time of ClearTH host, not business day.",
			usage = "sysTime(0,0,0)"
	)
	public long sysTime(int days, int months, int years) throws FunctionException
	{	
		return sysTime(days, months, years, 0);
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "int days, int months, int years, int hours",
			description = "Same as time(int days, int months, int years, int hours) function, but use current date and time of ClearTH host, not business day.",
			usage = "sysTime(0,0,0,0)"
	)
	public long sysTime(int days, int months, int years, int hours) throws FunctionException
	{	
		Calendar c = getCalendar();
		return getTime(c, days, months, years, hours);
	}
	/**
	 * Get time with offset
	 * @param days offset (days)
	 * @param months offset (months)
	 * @param years offset (years)
	 * @param hours offset (hours)
	 * @return time in milliseconds
	 */
	public long getTime(Calendar c, int days, int months, int years, int hours) throws FunctionException
	{
		if (Math.abs(days) > MAX_SHIFT)
			shiftError(days, "days");
		if (Math.abs(months) > MAX_SHIFT)
			shiftError(months, "months");
		if (Math.abs(years) > MAX_SHIFT)
			shiftError(years, "years");
		
		if (years != 0)
			c.add(Calendar.YEAR, years);
		if (months != 0)
			c.add(Calendar.MONTH, months);
		if (hours != 0)
			c.add(Calendar.HOUR_OF_DAY, hours);
		
		if (days!=0)
		{
			int toAdd;
			if (days>0)
				toAdd = 1;
			else
			{
				toAdd = -1;
				days = -days;
			}
			
			for (int i=0; i<days; i++)
			{
				c.add(Calendar.DATE, toAdd);
				while (checkHolidays(c))
					c.add(Calendar.DATE, toAdd);
			}
		}
		else
		{
			while (checkHolidays(c))
				c.add(Calendar.DATE, 1);
		}

		return c.getTime().getTime();
	}

	protected boolean checkHolidays(String checkDate, int dow) {
		return (((holidays.containsKey(checkDate)) && (holidays.get(checkDate))) ||
				((!holidays.containsKey(checkDate)) && (weekendHoliday) && ((dow==Calendar.SATURDAY) || (dow==Calendar.SUNDAY))));
	}
	
	protected boolean checkHolidays(Calendar checkDate) 
	{
		SimpleDateFormat holidayDF = new SimpleDateFormat(HOLIDAY_DATE_PATTERN);
		return checkHolidays(holidayDF.format(checkDate.getTime()), checkDate.get(Calendar.DAY_OF_WEEK));
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "long time, String format",
			description = "Returns formatted timestamp according to a given number of milliseconds and a format pattern.",
			usage = "format(time(0),'yyyy/MM/dd')"
	)
	public String format(long time, String formatStr)
	{
		return new SimpleDateFormat(formatStr).format(time);
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "String time, String currentFormat, String newFormat",
			description = "Changes format of given timestamp.",
			usage = "reformat('20-12-2018','dd-MM-yyyy','yyyy/MM/dd')"
	)
	public String reformat(String time, String currentFormat, String newFormat) throws FunctionException
	{
		Date date;
		SimpleDateFormat sdf = new SimpleDateFormat(currentFormat);
		sdf.setLenient(false);
		try
		{
			date = sdf.parse(time);
		}
		catch (ParseException e)
		{
			throw new FunctionException(String.format(ERROR_TIME_FORMAT, time, currentFormat), e);
		}
		sdf = new SimpleDateFormat(newFormat);
		
		return sdf.format(date);
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "String time, String format",
			description = "Uses given format to parse timestamp into a number of milliseconds passed since January 1, 1970 till given time.",
			usage = "parseDate('20-12-2018','dd-MM-yyyy')"
	)
	public long parseDate(String time, String format) throws FunctionException
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setLenient(false);
		try
		{
			return sdf.parse(time).getTime();
		}
		catch (ParseException e)
		{
			throw new FunctionException(String.format(ERROR_TIME_FORMAT, time, format), e);
		}
	}
	
	@MethodDataModel(
			group = "Date and Time",
			args = "long time",
			description = "Returns given time (number of milliseconds) shifted to last work day of month.",
			usage = "endOfMonth(time(0))"
	)
	public long endOfMonth(long time) throws FunctionException
	{
		Calendar c = getCalendar();
		c.setTimeInMillis(time);
		c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
		while (checkHolidays(c))
			c.add(Calendar.DATE, -1);
		return c.getTimeInMillis();
	}
	
	public String date(int day) throws FunctionException
	{
		Calendar c = getCalendar();
		long shiftedTime = getTime(c, day, 0, 0, 0);
		return new SimpleDateFormat("yyyyMMdd").format(shiftedTime);
	}
	
	public String date8(long date)
	{
		Calendar cld = getCalendar();
		cld.setTimeInMillis(date);
		return String.format("%04d%02d%02d", cld.get(Calendar.YEAR), cld.get(Calendar.MONTH)+1, 
				cld.get(Calendar.DAY_OF_MONTH));
	}
	
	public String time6(long time)
	{
		Calendar cld = getCalendar();
		cld.setTimeInMillis(time);
		return String.format("%02d%02d%02d", cld.get(Calendar.HOUR_OF_DAY), cld.get(Calendar.MINUTE), 
				cld.get(Calendar.SECOND));
	}
	
	public String trim(String str)
	{
		if (str.contains("//"))
			return str.substring(str.indexOf("//") + 2);
		else
			return str;
	}
	
	@MethodDataModel(
			group = "String",
			args = "String str, int length",
			description = "Trims given string to a specified length if string exceeds it.",
			usage = "trimleft('str', 0)"
	)
	public String trimleft(String str, int length)
	{
		if (str.length()<=length)
			return str;
		else
			return str.substring(0, length);
	}

	@MethodDataModel(
			group = "String",
			args = "String str, int length",
			description = "Trims a given string to a specified length from right if string exceeds it.",
			usage = "trimright('str', 0)"
	)
	public String trimright(String str, int offset)
	{
		if (str.length()<=offset)
			return str;
		else
			return str.substring(str.length()-offset);
	}
	
	public long toInt(String str)
	{
		return Long.parseLong(str);
	}

	@MethodDataModel(
			group = "String",
			args = " ",
			description = "Inserts 'end of line', i.e. 'carriage return + line feed' characters into parameter value.",
			usage = "eol()"
	)
	public static String eol()
	{
		return Utils.EOL;
	}

	@MethodDataModel(
			group = "String",
			args = "String pattern",
			description = "Returns the next value of a common counter inserted into a given pattern. " +
					"Each call of this function increments value of common counter by one. " +
					"The place and the length of the counter value is defined in pattern by a set of 'g' characters. " +
					"For example, the first call of generate() function with pattern 'Value_gggg' will return 'Value_0000', " +
					"next call will return 'Value_0001'. The value of the common counter is persisted even if ClearTH is restarted.",
			usage = "generate('Value_gggg')"
	)
	public String generate(String pattern)
	{
		return generate(DEFAULT_VALUE_GENERATOR, pattern);
	}

	@MethodDataModel(
			group = "String",
			args = "String counter, String pattern",
			description = "The same as generate(pattern) but uses custom named counter instead of common one. " +
					"Useful when you need to generate values independent from each other.",
			usage = "generate('Value_gggg', 'abc')"
	)
	public String generate(String generatorId, String pattern)
	{
		ValueGenerator valueGenerator = valueGenerators.get(generatorId);
		if (valueGenerator == null)
		{
			valueGenerator = valueGenerators().getGenerator(generatorId);
			valueGenerators.put(generatorId, valueGenerator);
		}
		
		if (pattern.isEmpty())
			return "";
		
		StringBuilder result = new StringBuilder();
		int len = 0;
		for (int i = 0; i < pattern.length(); i++)
		{
			if (pattern.charAt(i) == 'g')
				len++;
			else
			{
				if (len > 0)
				{
					result.append(valueGenerator.generateValue(len));
					len = 0;
				}
				result.append(pattern.charAt(i));
			}
		}
		if (len > 0)
			result.append(valueGenerator.generateValue(len));

		return result.toString();
	}

	@MethodDataModel(
			group = "String",
			args = "String value, char character, int length",
			description = "Returns given value appended with specified character till needed length",
			usage = "append('12345', 'X', 10)"
	)
	public String append(String value, char character, int length)
	{
		return value + StringUtils.repeat(Character.toString(character), length-value.length());
	}

	@MethodDataModel(
			group = "String",
			args = "String value, char character, int length",
			description = "Returns given value prepended with specified character till needed length",
			usage = "prepend('12345', 'X', 10)"
	)
	public String prepend(String value, char character, int length)
	{
		return StringUtils.repeat(Character.toString(character), length-value.length()) + value;
	}


	public String addZeros(String value)
	{
		return addZeros(value, 0, ".");
	}
	
	@MethodDataModel(
			group = "String",
			args = "String value, int number",
			description = "Appends a point ('.' character) and a given number of zeros to a given string.",
			usage = "addZeros('123', 3)"
	)
	public String addZeros(String value, int zeros)
	{
		return addZeros(value, zeros, ".");
	}
	
	@MethodDataModel(
			group = "String",
			args = "String value, int number, String delimiter",
			description = "The same as addZeros(value, number), but it also allows to specify a custom delimiter instead of the default point ('.' character).",
			usage = "addZeros('123', 3, ',')"
	)
	public String addZeros(String value, int zeros, String delim)
	{
		if (zeros<=0)
			return value;
		
		int point = value.indexOf(delim);
		if (point>-1)
		{
			point += delim.length();
			zeros -= value.length()-point;  //Decrease needed number of zeros by difference between value length and current number of digits after point
		}
		else
			value += delim;
		
		for (; zeros>0; zeros--)
			value += "0";
		
		return value;
	}
	
	@MethodDataModel(
			group = "String",
			args = "String value",
			description = "Cuts trailing zeros after point ('.' character) and the point itself if no fractional part left.",
			usage = "trimZeros('123.000')"
	)
	public String trimZeros(String value)
	{
		return !value.contains(".") ? value : value.replaceAll("0*$", "").replaceAll("\\.$", "");
	}
	
	@MethodDataModel(
			group = "Math",
			args = "Number a, Number b",
			description = "Returns the lower of two given numbers.",
			usage = "min(1, 2)"
	)
	public Number min(Object a, Object b)
	{
		Number aNum = toNumber(a);
		Number bNum = toNumber(b); 
		if ((aNum instanceof BigDecimal) || (bNum instanceof BigDecimal))
		{
			BigDecimal aDec = toBigDecimal(aNum);
			BigDecimal bDec = toBigDecimal(bNum);
			return aDec.min(bDec);
		}
		else
			return Math.min(aNum.longValue(), bNum.longValue());
	}

	@MethodDataModel(
			group = "Math",
			args = "Number a, Number b",
			description = "Returns a random number from given range",
			usage = "random(10, 500)"
	)
	public Number random(Object a, Object b)
	{
		Number aNum = toNumber(a);
		Number bNum = toNumber(b);

		BigDecimal aDec = getBigDecimal(aNum);
		BigDecimal bDec = getBigDecimal(bNum);

		int aPow = aDec.scale();
		int bPow = bDec.scale();

		if (aPow > 0 || bPow > 0)
		{
			int maxPow = max(aPow, bPow).intValue();
			BigDecimal multiplier = BigDecimal.TEN.pow(maxPow);
			int min = aDec.multiply(multiplier).intValue();
			int max = bDec.multiply(multiplier).intValue();

			return new BigDecimal(new Random().nextInt(max - min + 1) + min).divide(multiplier);
		}

		int min = aNum.intValue();
		int max = bNum.intValue();

		return new Random().nextInt(max-min+1)+min;
	}

	private BigDecimal getBigDecimal(Number num)
	{
		BigDecimal numDec = num instanceof BigDecimal ? (BigDecimal) num : new BigDecimal(num.toString());
		return numDec.stripTrailingZeros();
	}

	@MethodDataModel(
			group = "Math",
			args = "Number a, Number b",
			description = "Returns the greater of two given numbers.",
			usage = "max(1, 2)"
	)
	public Number max(Object a, Object b)
	{
		Number aNum = toNumber(a);
		Number bNum = toNumber(b); 
		if ((aNum instanceof BigDecimal) || (bNum instanceof BigDecimal))
		{
			BigDecimal aDec = toBigDecimal(aNum);
			BigDecimal bDec = toBigDecimal(bNum);
			return (aDec.compareTo(bDec) >= 0) ? aDec : bDec;
		}
		else
			return Math.max(aNum.longValue(), bNum.longValue());
	}
	
	private BigDecimal toBigDecimal(Number number)
	{
		return (number instanceof BigDecimal) ? (BigDecimal) number : new BigDecimal(number.longValue());
	}
	
	private Number toNumber(Object object)
	{
		if ((object instanceof Integer) 
				|| (object instanceof Long) 
				|| (object instanceof BigDecimal))
		{
			return (Number) object;
		}
		else if ((object instanceof Float)
				|| (object instanceof Double))
		{
			return BigDecimal.valueOf(((Number)object).doubleValue());
		}
		else if ((object instanceof String))
		{
			try
			{
				return Long.parseLong((String) object);
			}
			catch (NumberFormatException e)
			{
				return new BigDecimal((String) object);
			}
		} 
		else 
			throw new NumberFormatException(String.format("Unable to parse '%s' as number.", object));
	}

	@MethodDataModel(
			group = "Other",
			args = " ",
			description =  "Returns a random unique UUID.",
			usage = "uuid()"
	)
	public String uuid()
	{
		return UUID.randomUUID().toString();
	}


	public BigDecimal toBigDecimal(String number) throws ParseException
	{
		return toBigDecimal(number, null, null);
	}

	public BigDecimal toBigDecimal(String number, String decimalSeparator) throws ParseException
	{
		return toBigDecimal(number, decimalSeparator, null);
	}

	public BigDecimal toBigDecimal(String number, String decimalSeparator, String groupSeparator) throws ParseException
	{
		char mainDecimalSeparator = '.';

		DecimalFormat format = (DecimalFormat) DecimalFormat.getInstance();
		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();

		if (decimalSeparator != null && decimalSeparator.length() > 0)
			formatSymbols.setDecimalSeparator(decimalSeparator.charAt(0));

		if (groupSeparator != null && groupSeparator.length() > 0)
			formatSymbols.setGroupingSeparator(groupSeparator.charAt(0));
		else if (decimalSeparator != null && decimalSeparator.charAt(0) != mainDecimalSeparator)
			formatSymbols.setGroupingSeparator(mainDecimalSeparator);

		format.setDecimalFormatSymbols(formatSymbols);
		format.setParseBigDecimal(true);

		return (BigDecimal) format.parse(number);
	}
	
	
	public BigDecimal avg(double a, double b)
	{
		return avg(BigDecimal.valueOf(a), BigDecimal.valueOf(b));
	}
	
	public BigDecimal avg(BigDecimal a, BigDecimal b)
	{
		return a.add(b).divide(BigDecimal.valueOf(2));
	}

	public static String resolveFixedId(String id, Map<String, String> fixedIDs)
	{
		if ((fixedIDs!=null) && (fixedIDs.containsKey(id)))
			return fixedIDs.get(id);
		else
			return id;
	}
	
	public static String errorToText(Exception e)
	{
		if (e instanceof PropertyAccessException)
		{
			if ((e.getCause()!=null) && (e.getCause() instanceof InvocationTargetException))
			{
				if ((e.getCause().getCause() != null))
					return e.getCause().getCause().getMessage();
			}
			return "Incorrect formula";
		}
		else
			return e.getMessage();
	}
	
	
	protected String processExpressionResult(Object result)
	{
		if (result == null)
			return "";
		
		if (result instanceof String)
		{
			String processed = result.toString();
			if (processed.indexOf('\'') > -1) {
				processed = processed.replaceAll("\\'","\\\\'");
			}
			processed = "'" + processed + "'";
			return processed;
		}
		
		return result.toString();
	}
	
	public Object calculateExpression(String expression, String paramName, Map<String, Object> mvelVars,
	                                  Map<String, String> fixedIDs, Action currentAction, ObjectWrapper iterationWrapper) throws Exception
	{
		return calculateExpression(expression, paramName, mvelVars, fixedIDs, currentAction, iterationWrapper, true);
	}
	
	private Object calculateExpression(String expression, String paramName, Map<String, Object> mvelVars,
									  Map<String, String> fixedIDs, Action currentAction, ObjectWrapper iterationWrapper, boolean needClassCheck) throws Exception
	{
		Integer iteration = (Integer) iterationWrapper.getObject();
		do
		{
			iteration++;
			if (iteration > maxNumberOfIterations())
				throw new ParametersException("Too many iterations made to evaluate action parameter. It seems like you have circular reference that causes infinite loop.");
			//Searching for expression, it can contain sub-expressions, that's why we do it in a loop
			if (isSpecialValue(expression))        // It isn't a function
				break;


			int start = expression.indexOf(FORMULA_START);
			if (start<0)
				break;

			String before = expression.substring(0, start);
			String formula = expression.substring(start + 1);
			int end = TagUtils.indexClosingTag(formula, "{", "}");
			String after;
			if (end > -1)
			{
				after = formula.substring(end+1);
				formula = formula.substring(1, end);
			}
			else
				throw new ParametersException("Invalid expression. It should be closed with '}' character");

			//Checking if expression contains references to actions which IDs were fixed for MVEL and replacing action IDs with fixed ones if needed
			if (fixedIDs!=null)
			{
				int dot = -1;
				while ((dot = formula.indexOf('.', dot+1))>-1)  //Reference always contains '.'
				{
					//If '.' is located inside string literal - skip it
					if (!StringOperationUtils.checkUnquotedSymbol(formula, dot)) {
						continue;
					}

					int idStart = dot-1;
					while ((idStart>-1) && ((Character.isLetter(formula.charAt(idStart))) || (Character.isDigit(formula.charAt(idStart))) || (formula.charAt(idStart)=='_')))
						idStart--;
					idStart++;
					String id = formula.substring(idStart, dot);
					if (fixedIDs.containsKey(id))
					{
						formula = formula.substring(0, idStart)+fixedIDs.get(id)+formula.substring(dot);
						dot = idStart+fixedIDs.get(id).length();
					}
				}
			}

			if (formula.contains("(") && formula.contains(")")) {
				int bracket = -1;
//				int formLen = formula.length();
				int lastInd = 0;
				StringBuilder sb = new StringBuilder();
				while ((bracket = formula.indexOf('(', bracket+1))>-1) {
					if (!StringOperationUtils.checkUnquotedSymbol(formula, bracket)) {
						continue;
					}
					boolean isStringLiteral = false;
					int isFuncParameters = 0;
					int currInd = bracket + 1;
					sb.append(formula.substring(lastInd, bracket + 1));
					lastInd = bracket + 1;
					boolean isDone = false;

					while (!isDone) {
						char currChar = formula.charAt(currInd);
						if ((currChar == ',' || currChar == ')') && !isStringLiteral && isFuncParameters == 0) {
							String param = formula.substring(lastInd, currInd);
							if (!param.isEmpty()) {
								iterationWrapper.setObject(iteration);
								param = FORMULA_START + param + FORMULA_END;
								Object paramObj = calculateExpression(param, paramName + "_TMP_MVEL", mvelVars, fixedIDs, currentAction, iterationWrapper, false);
								param = processExpressionResult(paramObj);
							}

							sb.append(param).append(currChar);
							iteration = (Integer) iterationWrapper.getObject();
							lastInd = currInd + 1;
							if (currChar == ')') {
								isDone = true;
							}
						} else if (currChar == '\'') {
							if (!isStringLiteral || currInd <= 0 || formula.charAt(currInd - 1) != '\\') {
								isStringLiteral = !isStringLiteral;
							}
						} else if (currChar == '(' && !isStringLiteral) {
							isFuncParameters++;
						} else if (currChar == ')' && !isStringLiteral) {
							isFuncParameters--;
						}
						currInd++;
					}
					bracket = currInd - 1;
				}

				sb.append(formula.substring(lastInd));

				formula = sb.toString();
			}

			if (comparisonUtils().isSpecialFunction(expression)) {
				expression = before + FORMULA_START + formula + FORMULA_END + after;
				break;
			}

			//Compile and execute expression
			Serializable compiledExp = MVEL.compileExpression(formula, functionsContext);
			this.currentAction = currentAction;

			validator.validateExpression(compiledExp);

			Object resultObj = MVEL.executeExpression(compiledExp, this, mvelVars);  //if mvelVars is null it will only calculate function results, but will not follow references

			if(needClassCheck && (resultObj instanceof Class))
					throw new ParametersException("Incorrect formula");

			if (before.isEmpty() && after.isEmpty() && !(resultObj instanceof String)) {
				return resultObj;
			}

			String result = resultObj != null ? resultObj.toString() : null;
			//Getting result string
			expression = before+result+after;
		}
		while (true);

		iterationWrapper.setObject(iteration);
		return expression;
	}
	
	protected boolean isSpecialValue(String expression)
	{
		return comparisonUtils().isSpecialValue(expression)
				|| SpecialValue.isSpecialValue(expression);
	}
	
	
	public static DateFormat getHolidayDF()
	{
		return new SimpleDateFormat(HOLIDAY_DATE_PATTERN);
	}
	
	public Date getBusinessDay()
	{
		return businessDay;
	}
	
	public Map<String, Boolean> getHolidays()
	{
		return holidays;
	}
	
	public void setWeekendHoliday(boolean weekendHoliday)
	{
		synchronized (weekendHolidayMonitor)
		{
			this.weekendHoliday = weekendHoliday;
		}
	}


	public Map<String, Object> getExecutionResultParams(Action action)
	{
		if (action.isPassed())
			return PASSED_ACTION_PARAMS;
		else 
		{
			Result result = action.getResult();
			FailReason failReason = (result != null) ? result.getFailReason() : FailReason.FAILED;
			return FAILED_ACTION_PARAMS.get(failReason);
		}
	}

	public void setCurrentTime(Calendar currentTime) {
		this.currentTime = currentTime;
	}
	
	
	public Date getBaseTime()
	{
		return baseTime != null ? baseTime.getTime() : null;
	}

	//Basetime, if set, works as origin instead of execution start time.
	//Calculating difference between basetime and current real time.
	//It will be subtracted from current time in applyTime() to calculate time value relative to basetime.
	public void setBaseTime(Date baseTime)
	{
		if (baseTime != null)
		{
			this.baseTime = getCalendar();
			this.baseTime.setTime(baseTime);
			
			baseTimeChanged = getCalendar();
		}
		else
		{
			this.baseTime = null;
			this.baseTimeChanged = null;
		}
	}
	
	
	protected String changeScale(BigDecimal bd, int scale)
	{
		if (scale > -1)
			return bd.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
		return bd.toPlainString();
	}
	
	protected String stripFunctions(String d)
	{
		int functionStart = -1;
		String functionName = null;
		for (String fn : ComparisonUtils.SPECIAL_NUMBER_FUNCTION_NAMES)
		{
			functionStart = d.indexOf(fn);
			if (functionStart != -1)
			{
				functionName = fn;
				break;
			}
		}
		
		if (functionStart == -1)
			return d;
		
		//                                             name + (
		int cutFrom = functionStart + functionName.length() + 1;
		
		int cutTo = indexOfAny(d, ",)");
		if (cutTo == -1)
			cutTo = d.length();
		
		String newD = removeQuotes(d.substring(cutFrom, cutTo));
		return stripFunctions(newD);
	}
	
	private String removeQuotes(String s)
	{
		return removeStart(removeEnd(s, "'"), "'");
	}
	
	//This method is meant to remove functions like {asNumber('value')}.
	//They are present when value is obtained by reference to parameter with @{asNumber(value)} thus making math operations impossible
	protected BigDecimal createBigDecimal(String value)
	{
		value = stripFunctions(value);
		return new BigDecimal(value);
	}
	
	@MethodDataModel(
			group = "Math",
			args = "String d, String summand, int scale",
			description =  "Returns a sum with specified scale.",
			usage = "add('1','2',1)"
	)
	public String add(String d, String summand, int scale)
	{
		BigDecimal result = createBigDecimal(d).add(createBigDecimal(summand));
		return changeScale(result, scale);
	}
	
	@MethodDataModel(
			group = "Math",
			args = "String d, String summand",
			description =  "Returns a sum with scale of 5.",
			usage = "add('1','2')"
	)
	public String add(String d, String summand)
	{
		return add(d, summand, 5);
	}
	
	@MethodDataModel(
			group = "Math",
			args = "String d, String deduction, int scale",
			description =  "Returns result of substraction with specified scale.",
			usage = "sub('1','2',1)"
	)
	public String sub(String d, String deduction, int scale)
	{
		BigDecimal result = createBigDecimal(d).subtract(createBigDecimal(deduction));
		return changeScale(result, scale);
	}
	
	@MethodDataModel(
			group = "Math",
			args = "String d, String deduction",
			description =  "Returns result of substraction with scale of 5.",
			usage = "sub('1','2')"
	)
	public String sub(String d, String deduction)
	{
		return sub(d, deduction, 5);
	}
	
	@MethodDataModel(
			group = "Math",
			args = "String d, String multiplicand, int scale",
			description =  "Returns result of multiplication with specified scale.",
			usage = "mul('1','2',1)"
	)
	public String mul(String d, String multiplicand, int scale)
	{
		BigDecimal result = createBigDecimal(d).multiply(createBigDecimal(multiplicand));
		return changeScale(result, scale);
	}
	
	@MethodDataModel(
			group = "Math",
			args = "String d, String multiplicand",
			description =  "Returns result of multiplication with scale of 5.",
			usage = "mul('1','2')"
	)
	public String mul(String d, String multiplicand)
	{
		return mul(d, multiplicand, 5);
	}
	
	@MethodDataModel(
			group = "Math",
			args = "String d, String divisor, int scale",
			description =  "Returns result of division with specified scale.",
			usage = "div('1','2',1)"
	)
	public String div(String d, String divisor, int scale)
	{
		BigDecimal operand1 = createBigDecimal(d),
				operand2 = createBigDecimal(divisor);
		return div(operand1, operand2, scale);
	}

	@MethodDataModel(
			group = "Math",
			args = "BigDecimal operand1, BigDecimal operand1, int scale",
			description =  "Returns result of division with specified scale.",
			usage = "div('1','2',1)"
	)
	public String div(BigDecimal operand1, BigDecimal operand2, int scale)
	{
		if (scale > -1)
			return operand1.divide(operand2, scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
		return operand1.divide(operand2).stripTrailingZeros().toPlainString();
	}

	@MethodDataModel(
			group = "Math",
			args = "String d, String divisor",
			description =  "Returns result of division with scale of 5",
			usage = "div('1','2')"
	)
	public String div(String d, String divisor)
	{
		return div(d, divisor, 5);
	}
	
	@MethodDataModel(
			group = "Math",
			args = "String number, int scale",
			description = "Rounds given number half-up to number of decimals from scale parameter.",
			usage = "round('1.555', 2)"
	)
	public String round(String number, int scale)
	{
		return createBigDecimal(number).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	@MethodDataModel(
			group = "Math",
			args = "String number, int scale",
			description = "Given number is rounded strictly up and scaled to number of decimals from scale parameter.",
			usage = "roundUp('1.555', 2)"
	)
	public String roundUp(String number, int scale)
	{
		return createBigDecimal(number).setScale(scale, RoundingMode.UP).stripTrailingZeros().toPlainString();
	}

	@MethodDataModel(
			group = "Math",
			args = "String number, int scale",
			description = "Rounds given number down to number of decimals from scale parameter.",
			usage = "roundUp('1.555', 2)"
	)
	public String roundDown(String number, int scale)
	{
		return createBigDecimal(number).setScale(scale, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
	}

	protected static final Map<String, Object> PASSED_ACTION_PARAMS = new HashMap<String, Object>() {{
		put(PASSED_PARAM, true);
		put(FAIL_REASON_PARAM, null);
	}};

	protected static final Map<FailReason, Map<String, Object>> FAILED_ACTION_PARAMS =
			new EnumMap<FailReason, Map<String, Object>>(FailReason.class)
			{{
				for (FailReason failReason : FailReason.values())
				{
					put(failReason, new HashMap<String, Object>()
					{{
						put(PASSED_PARAM, false);
						put(FAIL_REASON_PARAM, failReason.name());
					}});
				}
			}};

	public String milliseconds(String value, String format) throws FunctionException
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setLenient(false);
		try
		{
			Date date = sdf.parse(value);
			return String.valueOf(date.getTime());
		}
		catch (ParseException e)
		{
			throw new FunctionException("Could not get time from date '"+value+"' using format '"+format+"'", e);
		}
	}
}
