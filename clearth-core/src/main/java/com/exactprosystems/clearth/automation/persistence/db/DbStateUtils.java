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

package com.exactprosystems.clearth.automation.persistence.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.exactprosystems.clearth.utils.XmlUtils;

public class DbStateUtils
{
	public static final String PATTERN_DATE = "yyyy.MM.dd",
			PATTERN_TIMESTAMP = "yyyy.MM.dd HH:mm:ss.SSSSSS";
	
	@SuppressWarnings("rawtypes")
	public static byte[] saveToXml(Object info, Class[] allowedClasses) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		XmlUtils.objectToXml(info, baos, null, allowedClasses);
		return baos.toByteArray();
	}
	
	@SuppressWarnings("rawtypes")
	public static <T> T loadFromXml(byte[] bytes, Class[] allowedClasses) throws IOException
	{
		return (T)XmlUtils.xmlToObject(new ByteArrayInputStream(bytes), null, allowedClasses);
	}
	
	
	public static DateFormat createDateFormat()
	{
		return new SimpleDateFormat(PATTERN_DATE);
	}
	
	public static DateFormat createTimestampFormat()
	{
		return new SimpleDateFormat(PATTERN_TIMESTAMP);
	}
	
	
	public static String formatTimestamp(Date timestamp, DateFormat format)
	{
		if (timestamp == null)
			return null;
		return format.format(timestamp);
	}
	
	public static Date parseTimestamp(String timestamp, DateFormat format) throws ParseException
	{
		if (StringUtils.isEmpty(timestamp))
			return null;
		return format.parse(timestamp);
	}
}