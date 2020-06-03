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

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateTimeUtils
{
	private static final Logger logger = LoggerFactory.getLogger(DateTimeUtils.class);


	public static long getStartOfDayTimestamp(Date date)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}
	
	public static long getTodaysMillis(Date date)
	{
		Calendar input = Calendar.getInstance();
		input.setTime(date);
		Calendar now = Calendar.getInstance();
		input.set(Calendar.YEAR, now.get(Calendar.YEAR));
		input.set(Calendar.MONTH, now.get(Calendar.MONTH));
		input.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
		return input.getTimeInMillis();
	}
	
	public static long getEndOfDayTimestamp(Date date)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTimeInMillis();
	}
	
	public static long toTimestamp(Date date, Date time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		Calendar timeCalendar = Calendar.getInstance();
		timeCalendar.setTime(time);

		calendar.set(HOUR_OF_DAY, timeCalendar.get(HOUR_OF_DAY));
		calendar.set(MINUTE, timeCalendar.get(MINUTE));
		calendar.set(SECOND, timeCalendar.get(SECOND));
		calendar.set(MILLISECOND, 0);

		return calendar.getTimeInMillis();
	}
	
	
	public static boolean isNewYear()
	{
		Calendar c = Calendar.getInstance();
		int month = c.get(Calendar.MONTH), day = c.get(Calendar.DAY_OF_MONTH);
		return ((month == Calendar.DECEMBER) && (day > 20)) || ((month == Calendar.JANUARY) && (day < 15));
	}
	
	public static boolean isWeekend(Date date)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
		return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
	}

	public static Date getDateFromTimestampOrNull(String timestamp)
	{
		if (timestamp == null || timestamp.isEmpty())
			return null;
		try
		{
			return new Date(Long.parseLong(timestamp));
		}
		catch (NumberFormatException e)
		{
			logger.info("Cannot get date from timestamp", e);
			return null;
		}
	}
}
