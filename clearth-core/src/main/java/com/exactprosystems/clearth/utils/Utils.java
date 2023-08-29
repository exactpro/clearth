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

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.Statement;

public class Utils
{
	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	public static final String EOL = "\r\n", UTF8 = "UTF-8";
	

	public static <T> T nvl(T valueToCheck, T defaultValue)
	{
		return (valueToCheck != null) ? valueToCheck : defaultValue;
	}
	
	
	public static void closeResource(AutoCloseable resource)
	{
		if (resource != null)
		{
			try
			{
				resource.close();
			}
			catch (Exception e)
			{
				logger.error("Error while closing resource", e);
			}
		}
	}

	public static void closeResource(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (Exception e) {
				logger.error("Error while closing statement", e);
			}
		}
	}

	public static void closeResource(CsvReader reader)
	{
		if (reader != null)
			reader.close();
	}

	public static void closeResource(CsvWriter writer)
	{
		if (writer != null)
			writer.close();
	}

	public static void closeStatement(Statement statement)
	{
		if (statement != null)
		{
			try
			{
				statement.close();
			}
			catch (SQLException e)
			{
				logger.error("Error while closing statement", e);
			}
		}
	}

	public static String host()
	{
		try
		{
			return java.net.InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e)
		{
			return "unknown";
		}
	}
}
