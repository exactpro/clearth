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

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;

public class InputParamsHandlerTest {
	
	private static Date getDate(int year, int month, int day) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.YEAR,	year);
		c.set(Calendar.MONTH,	month);
		c.set(Calendar.DATE,	day);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE,	0);
		c.set(Calendar.SECOND,	0);
		c.set(Calendar.MILLISECOND, 0);
		return c.getTime();
	}
	
	@BeforeClass
	public static void setLocale() {
		Locale.setDefault(Locale.ENGLISH);
	}
	
	@Test
	public void testGetRequiredStringSuccess() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "2", "C", "1"));
		Assert.assertEquals("1", handler.getRequiredString("C"));
		checkSuccess(handler);
	}

	@Test
	public void testGetRequiredStringFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "1", "B", null, "C", ""));
		Assert.assertNull(handler.getRequiredString("B"));
		Assert.assertEquals("", handler.getRequiredString("C"));
		Assert.assertEquals("1", handler.getRequiredString("A"));
		checkFailed(handler, "The following required parameters are empty: 'B', 'C'");
	}
	
	@Test
	public void testGetRequiredStringFromListSuccess() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "buy", "B", "Sell", "C", "BUY", "D", "1"));
		List<String> list =  Arrays.asList("buy", "sell");
		Assert.assertEquals("buy", handler.getRequiredString("A", list));
		Assert.assertEquals("sell", handler.getRequiredString("B", list));
		Assert.assertEquals("buy", handler.getRequiredString("C", list));
		checkSuccess(handler);
	}

	@Test
	public void testGetRequiredStringFromListFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "buy", "B", "Sell", "C", "1", "D", "1"));
		List<String> list =  Arrays.asList("buy", "sell");
		Assert.assertEquals("buy", handler.getRequiredString("A", list));
		Assert.assertNull(handler.getRequiredString("B", list, true));
		Assert.assertNull(handler.getRequiredString("C", list));
		checkFailed(handler, "The following parameters have unexpected values: " +
				"'B' \\(actual = 'Sell', possible values: 'buy', 'sell'\\), 'C' \\(actual = '1', possible values: 'buy', 'sell'\\)");
	}

	@Test
	public void testGetStringSuccess() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "1", "B", null, "C", "", "E", "sell"));
		List<String> list = Arrays.asList("buy", "sell");
		Assert.assertNull(handler.getString("B"));
		Assert.assertEquals("", handler.getString("C"));
		Assert.assertEquals("1", handler.getString("A"));
		Assert.assertEquals("2", handler.getString("D", "2"));
		Assert.assertEquals("sell", handler.getString("E", list));
		Assert.assertEquals("buy", handler.getString("F", "buy", list));
		checkSuccess(handler);
	}
	
	@Test
	public void testGetStringFromListFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "a", "B", "b", "D", "Sell"));
		List<String> list = Arrays.asList("1", "2");
		Assert.assertNull(handler.getString("C", list));
		Assert.assertNull(handler.getString("A", list));
		Assert.assertNull(handler.getString("B", Arrays.asList("B", "C"), true));
		checkFailed(handler, "The following parameters have unexpected values: " +
				"'A' \\(actual = 'a', possible values: '1', '2'\\), 'B' \\(actual = 'b', possible values: 'B', 'C'\\)");
	}

	@Test
	public void testGetRequiredDateFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("G", null, "F", "19990105"));
		Assert.assertNull(handler.getDate("D", "dd.MM.yyyy"));
		Assert.assertNull(handler.getRequiredDate("E", "yyyy-MM-dd HH:mm:ss"));
		Assert.assertEquals(getDate(1999, 0/*JANUARY*/, 5), handler.getRequiredDate("F", "yyyyMMdd"));
		checkFailed(handler, "The following required parameters are empty: 'E'");
	}

	@Test
	public void testGetRequiredDateFailedFormat() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("F", "19990105", "E", "19990105"));
		Assert.assertNull(handler.getRequiredDate("E", "yyyy-MM-dd HH:mm:ss"));
		Assert.assertEquals(getDate(1999, 0/*JANUARY*/, 5), handler.getRequiredDate("F", "yyyyMMdd"));
		Assert.assertEquals(getDate(1999, 0/*JANUARY*/, 5), handler.getDate("G", getDate(1999, 0/*JANUARY*/, 5), "yyyyMMdd"));
		checkFailed(handler, "The following parameters have incorrect format: 'E' \\(yyyy.*");
	}

	@Test
	public void testGetRequiredLocalDateFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("G", null, "F", "19990105"));
		Assert.assertNull(handler.getLocalDate("D", "dd.MM.yyyy"));
		Assert.assertNull(handler.getRequiredLocalDate("E", "yyyy-MM-dd"));
		Assert.assertEquals(LocalDate.of(1999, Month.JANUARY, 5), handler.getRequiredLocalDate("F", "yyyyMMdd"));
		checkFailed(handler, "The following required parameters are empty: 'E'");
	}

	@Test
	public void testGetRequiredLocalDateFailedFormat() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("F", "19990105", "E", "19990105"));
		Assert.assertNull(handler.getRequiredDate("E", "yyyy-MM-dd"));
		Assert.assertEquals(LocalDate.of(1999, Month.JANUARY, 5), handler.getRequiredLocalDate("F", "yyyyMMdd"));
		Assert.assertEquals(LocalDate.of(1999, Month.JANUARY, 5), handler.getLocalDate("G", "yyyyMMdd",
				LocalDate.of(1999, Month.JANUARY, 5)));
		checkFailed(handler, "The following parameters have incorrect format: 'E' \\(yyyy.*");
	}

	@Test
	public void testGetRequiredLocalTimeFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("G", null, "F", "235959"));
		Assert.assertNull(handler.getLocalTime("D", "HH:mm:ss"));
		Assert.assertNull(handler.getRequiredLocalTime("E", "HH.mm.ss"));
		Assert.assertEquals(LocalTime.of(23, 59, 59), handler.getRequiredLocalTime("F", "HHmmss"));
		checkFailed(handler, "The following required parameters are empty: 'E'");
	}

	@Test
	public void testGetRequiredLocalTimeFailedFormat() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("F", "235959", "E", "235959"));
		Assert.assertNull(handler.getRequiredDate("E", "HH:mm:ss"));
		Assert.assertEquals(LocalTime.of(23, 59, 59), handler.getRequiredLocalTime("F", "HHmmss"));
		Assert.assertEquals(LocalTime.of(23, 59, 59), handler.getLocalTime("G", "HHmmss", LocalTime.of(23, 59, 59)));
		checkFailed(handler, "The following parameters have incorrect format: 'E' \\(HH.*");
	}

	@Test
	public void testGetRequiredLocalDateTimeFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("G", null, "F", "Dec 31, 2014 - 23:59"));
		Assert.assertNull(handler.getLocalDateTime("D", "MMMddyyyy - HH:mm"));
		Assert.assertNull(handler.getRequiredLocalDateTime("E", "MMMddyyyy HH:mm"));
		Assert.assertEquals(LocalDateTime.of(2014, Month.DECEMBER, 31, 23, 59),
				handler.getRequiredLocalDateTime("F", "MMM dd, yyyy - HH:mm"));
		checkFailed(handler, "The following required parameters are empty: 'E'");
	}

	@Test
	public void testGetRequiredLocalDateTimeFailedFormat() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("F", "Dec 31, 2014 - 23:59", "E", "Dec 31, 2014 - 23:59"));
		Assert.assertNull(handler.getRequiredLocalDateTime("E", "MMMddyyyy - HH:mm"));
		Assert.assertEquals(LocalDateTime.of(2014, Month.DECEMBER, 31, 23, 59),
				handler.getRequiredLocalDateTime("F", "MMM dd, yyyy - HH:mm"));
		Assert.assertEquals(LocalDateTime.of(2014, Month.DECEMBER, 31, 23, 59),
				handler.getLocalDateTime("G", "MMM dd, yyyy - HH:mm", LocalDateTime.of(2014, Month.DECEMBER, 31, 23, 59)));
		checkFailed(handler, "The following parameters have incorrect format: 'E' \\(MMM.*");
	}

	@Test
	public void testGetRequiredZonedDateTimeFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("G", null, "F", "14-05-01 03:55"));
		Assert.assertNull(handler.getZonedDateTime("D", "yy-MM-dd hhmmE a", ZoneId.of("GMT-7")));
		Assert.assertNull(handler.getRequiredZonedDateTime("E", " yy-MM-dd hh:mm", ZoneId.of("GMT-7")));
		Assert.assertEquals(ZonedDateTime.of(2014, 5, 1, 3, 55, 0, 0, ZoneId.of("GMT-7")),
				handler.getRequiredZonedDateTime("F", "yy-MM-dd HH:mm", ZoneId.of("GMT-7")));
		checkFailed(handler, "The following required parameters are empty: 'E'");
	}

	@Test
	public void testGetRequiredZonedDateTimeFailedFormat() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("F", "14-05-01 03:55", "E", "14-05-01 03:55"));
		Assert.assertNull(handler.getRequiredZonedDateTime("E", "yy-MM-dd hhmmE a x", ZoneId.of("GMT-7")));
		Assert.assertEquals(ZonedDateTime.of(2014, 5, 1, 3, 55, 0, 0, ZoneId.of("GMT-7")),
				handler.getRequiredZonedDateTime("F", "yy-MM-dd HH:mm", ZoneId.of("GMT-7")));
		Assert.assertEquals(ZonedDateTime.of(2014, 5, 1, 3, 55, 0, 0, ZoneId.of("GMT-7")),
				handler.getZonedDateTime("G", "yy-MM-dd HH:mm", ZoneId.of("GMT-7"),
						ZonedDateTime.of(2014, 5, 1, 3, 55, 0, 0, ZoneId.of("GMT-7"))));
		checkFailed(handler, "The following parameters have incorrect format: 'E' \\(yy.*");
	}

	@Test
	public void testGetRequiredDateAsStringFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "19990105"));
		Assert.assertEquals("19990105", handler.getRequiredDateAsString("A", "yyyyMMdd"));
		Assert.assertEquals("19940101", handler.getDateAsString("B", "19940101", "yyyyMMdd"));
		Assert.assertEquals(null, handler.getRequiredDateAsString("C", "yyyyMMdd"));
		checkFailed(handler, "The following required parameters are empty: 'C'");
	}
	
	@Test
	public void testGetRequiredDateAsStringFailedFormat() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "19990105", "B", "19990909"));
		Assert.assertEquals("19990105", handler.getRequiredDateAsString("A", "yyyyMMdd"));
		Assert.assertNull(handler.getRequiredDateAsString("B", "yyyy/MM/dd"));
		checkFailed(handler, "The following parameters have incorrect format: 'B' \\(yyyy/MM/dd\\)");
	}
	
	@Test
	public void testGetDateAsStringFailedFormat() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "19990105", "B", "19980205", "C", "20042016 12:50:45"));
		Assert.assertEquals("19990105", handler.getDateAsString("A", "yyyyMMdd"));
		Assert.assertNull(handler.getDateAsString("B", "ddMMyyyy"));
		Assert.assertEquals("20042016", handler.getDateAsString("C", "ddMMyyyy"));
		checkFailed(handler, "The following parameters have incorrect format: 'B' \\(ddMMyyyy\\)");
	}
	
	@Test
	public void testGetRequiredDoubleFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("K", null, "L", "17.05000", "M", "1e-5", "N", "nine"));
		Assert.assertNull(handler.getDouble("A"));
		Assert.assertEquals(Double.valueOf(12.01), handler.getDouble("P", 12.01));
		Assert.assertNull(handler.getRequiredDouble("B"));
		Assert.assertNull(handler.getRequiredDouble("K"));
		Assert.assertEquals(Double.valueOf(17.05), handler.getRequiredDouble("L"));
		Assert.assertEquals(Double.valueOf(0.00001), handler.getRequiredDouble("M"));
		checkFailed(handler, "The following required parameters are empty: 'B', 'K'");
	}
	
	@Test
	public void testGetRequiredDoubleFailedFormat() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("K", null, "L", "17.05000", "M", "1e-5", "N", "nine"));
		Assert.assertEquals(Double.valueOf(17.05), handler.getRequiredDouble("L"));
		Assert.assertNull(handler.getRequiredDouble("N"));
		checkFailed(handler, "The following parameters have incorrect format.*");
	}
	
	@Test
	public void testGetRequiredIntegerFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "12", "B", "9", "C", "a", "G", "8"));
		List<Integer> list = Arrays.asList(1, 2, 3);
		Assert.assertNull(handler.getRequiredInteger("D"));
		Assert.assertNull(handler.getRequiredInteger("B", list));
		Assert.assertNull(handler.getInteger("E"));
		Assert.assertEquals(Integer.valueOf(1994), handler.getInteger("F", 1994));
		Assert.assertNull(handler.getInteger("G", list));
		Assert.assertEquals(Integer.valueOf(1), handler.getInteger("H", 1, list));
		checkFailed(handler, "The following required parameters are empty: 'D'; \n" +
				"The following parameters have unexpected values: 'B' \\(actual = '9', possible values: '1', '2', '3'\\), " +
				"'G' \\(actual = '8', possible values: '1', '2', '3'\\)");
	}
	
	@Test 
	public void testGetBooleanFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "True", "B", "tr", "C", "no", "D", "not"));
		Assert.assertNull(handler.getRequiredBooleanOrNull("E"));
		Assert.assertFalse(handler.getRequiredBoolean("F", false));
		Assert.assertNull(handler.getBooleanOrNull("G"));
		Assert.assertTrue(handler.getBoolean("H", true));
		Assert.assertEquals(Boolean.TRUE, handler.getRequiredBooleanOrNull("A"));
		Assert.assertNull(handler.getBooleanOrNull("B"));
		Assert.assertFalse(handler.getBoolean("C", true));
		Assert.assertTrue(handler.getBoolean("D", true));
		// Should not generate NPE
		@SuppressWarnings("unused") boolean a = handler.getBoolean("F", true) && handler.getRequiredBoolean("J", false);
		checkFailed(handler, "The following required parameters are empty: 'E', 'F', 'J'; \n" +
				"The following parameters have unexpected values: " +
				"'D' \\(actual = 'not', possible values: 'y', 'yes', 'true', '1', 'n', 'no', 'false', '0'\\)");
	}
	
	@Test
	public void testGetBigDecimalSuccess() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "123.12"));
		Assert.assertEquals(BigDecimal.valueOf(123.12), handler.getBigDecimal("A"));
		Assert.assertNull(handler.getBigDecimal("B"));
		Assert.assertEquals(BigDecimal.valueOf(123.12), handler.getBigDecimal("C", BigDecimal.valueOf(123.12)));
		checkSuccess(handler);
	}
	
	@Test
	public void testGetLongFailed() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(map("A", "123", "B", "345", "C", "123.56f"));
		Assert.assertEquals(Long.valueOf(123L), handler.getRequiredLong("A"));
		Assert.assertNull(handler.getRequiredLong("D"));
		Assert.assertEquals(Long.valueOf(345L), handler.getLong("B"));
		Assert.assertNull(handler.getLong("E"));
		Assert.assertEquals(Long.valueOf(12L), handler.getLong("F", 12L));
		Assert.assertNull(handler.getLong("C"));
		checkFailed(handler, "The following required parameters are empty: 'D'; \n" +
				"The following parameters have incorrect format: 'C' \\(long integer\\)");
	}
	
	@Test
	public void testRequiredMulti() throws ResultException
	{
		InputParamsHandler handler = new InputParamsHandler(
				map("Null", null, "Double1", "17.05000", "Double2", "12.5", "Integer1", "73", "Date1", "199915", "Bool1", "Yes", "Bool2", "Noooooo", "BigDecimal1", "15.23", "BigDecimal2", "15,23"));
		Assert.assertEquals(Double.valueOf(17.05), handler.getRequiredDouble("Double1"));
		Assert.assertNull(handler.getRequiredDouble("Double0"));
		Assert.assertNull(handler.getRequiredInteger("Double2"));
		Assert.assertEquals(Integer.valueOf(73), handler.getRequiredInteger("Integer1"));
		Assert.assertNull(handler.getRequiredDate("Date0", "yyyyMMdd"));
		Assert.assertNull(handler.getRequiredDate("Date1", "yyyyMMdd"));
		Assert.assertEquals(Boolean.TRUE, handler.getRequiredBooleanOrNull("Bool1"));
		Assert.assertNull(handler.getRequiredBooleanOrNull("Bool2"));
		Assert.assertNull(handler.getRequiredBooleanOrNull("Bool0"));
		Assert.assertEquals(new BigDecimal("15.23"), handler.getRequiredBigDecimal("BigDecimal1"));
		Assert.assertNull(handler.getRequiredBigDecimal("BigDecimal2"));
		Assert.assertNull(handler.getRequiredBigDecimal("BigDecimal0"));
		checkFailed(handler, "The following required parameters are empty: 'Double0', 'Date0', 'Bool0', 'BigDecimal0'; \n" +
				"The following parameters have incorrect format: 'Double2' \\(integer\\), 'Date1' \\(yyyyMMdd\\), 'BigDecimal2' \\(BigDecimal\\); \n" +
				"The following parameters have unexpected values: 'Bool2' \\(actual = 'Noooooo', possible values: 'y', 'yes', 'true', '1', 'n', 'no', 'false', '0'\\)");
	}
	
	@Test
	public void testCheckAdditionalParams()
	{
		try
		{
			InputParamsHandler handler = new InputParamsHandler(map("A", "1", "B", "2"));
			handler.check("A", "B", "C", "D");
			throw new RuntimeException("check() method is finished without exception");
		}
		catch (ResultException e)
		{
			Assert.assertEquals("The following required parameters are empty: 'C', 'D'", e.getMessage());
		}
	}

	private void checkSuccess(InputParamsHandler handler) throws ResultException
	{
		handler.check();
	}

	private void checkFailed(InputParamsHandler handler, String messagePattern)
	{
		try 
		{
			handler.check();
			throw new RuntimeException("check() method is finished without exception");
		} 
		catch (ResultException e)
		{
			String msg = e.getMessage();
			if (msg == null)
				throw new RuntimeException("check() method is thrown exception with null message");
			if (!msg.matches(messagePattern))
				throw new RuntimeException("check() method is finished with unexpected message: " + msg);
		}
	}
}
