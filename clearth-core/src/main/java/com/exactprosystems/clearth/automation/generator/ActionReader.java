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

package com.exactprosystems.clearth.automation.generator;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import com.exactprosystems.clearth.automation.ActionGenerator;

public abstract class ActionReader implements Closeable
{
	private final String source;
	private final boolean trimValues;
	
	public ActionReader(String source, boolean trimValues)
	{
		this.source = source;
		this.trimValues = trimValues;
	}
	
	public static String processActionValue(String value, boolean header)
	{
		if (header)
		{
			//If it is header column, need to return its value without #
			if (value.trim().startsWith(ActionGenerator.HEADER_DELIMITER))
				return value.substring(
						value.indexOf(ActionGenerator.HEADER_DELIMITER)+ActionGenerator.HEADER_DELIMITER.length());  //Do not trim result value, need to pass it as is!
			else
				return "";
		}
		else
		{
			//If value is escaped with PREFIX, need to return it without PREFIX
			if (value.trim().startsWith(ActionGenerator.PREFIX))
				return value.substring(value.indexOf(ActionGenerator.PREFIX)+ActionGenerator.PREFIX.length());  //Do not trim result value, need to pass it as is!
			return value;
		}
	}
	
	public abstract boolean readNextLine() throws IOException;
	public abstract boolean isCommentLine();
	public abstract String getRawLine() throws IOException;
	public abstract boolean isHeaderLine();
	public abstract boolean isEmptyLine();
	public abstract List<String> parseLine(boolean header) throws IOException;

	
	public String getSource()
	{
		return source;
	}

	public boolean isTrimValues()
	{
		return trimValues;
	}
	
	
	protected String processValue(String value, boolean header)
	{
		return processActionValue(value, header);
	}
}
