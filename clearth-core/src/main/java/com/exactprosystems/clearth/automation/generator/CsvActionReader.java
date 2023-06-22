/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

import com.csvreader.CsvReader;
import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.input.BOMInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CsvActionReader extends ActionReader
{
	private final CsvReader reader;
	private String[] values;
	private String rawLine;
	private int rowIndex;

	public CsvActionReader(String fileName, boolean trimValues) throws IOException
	{
		super(fileName, trimValues);
		reader = createReader(fileName);
		rowIndex = 0;
	}
	
	@Override
	public void close() throws IOException
	{
		Utils.closeResource(reader);
	}
	
	@Override
	public boolean readNextLine() throws IOException
	{
		if (!reader.readRecord())
			return false;

		rowIndex++;
		values = reader.getValues();
		rawLine = reader.getRawRecord();
		return true;
	}
	
	@Override
	public boolean isCommentLine()
	{
		//Can't use rawLine here because value can be in quotes and should be processed as a CSV value
		return (values.length > 0) && (values[0].trim().startsWith(ActionGenerator.COMMENT_INDICATOR));
	}
	
	@Override
	public boolean isHeaderLine()
	{
		//Can't use rawLine here because value can be in quotes and should be processed as a CSV value
		return (values.length > 0) && (values[0].trim().startsWith(ActionGenerator.HEADER_DELIMITER));
	}
	
	@Override
	public boolean isEmptyLine()
	{
		for (String v : values)
		{
			if (!v.isEmpty())
				return false;
		}
		return true;
	}
	
	@Override
	public List<String> parseLine(boolean header) throws IOException
	{
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < values.length; i++)
		{
			String value = values[i];  //Values are already trimmed by reader if needed, see createReader()
			value = processValue(value, header);
			result.add(value);
		}
		return result;
	}

	@Override
	public String getRawLine() throws IOException
	{
		return rawLine;
	}
	
	protected CsvReader createReader(String fileName) throws IOException
	{
		CsvReader result = new CsvReader(new InputStreamReader(new BOMInputStream(new FileInputStream(fileName))));
		result.setSafetySwitch(false);
		result.setDelimiter(ActionGenerator.DELIMITER);
		result.setTextQualifier(ActionGenerator.TEXT_QUALIFIER);
		result.setSkipEmptyRecords(false);
		result.setTrimWhitespace(isTrimValues());
		return result;
	}
	
	
	public CsvReader getReader()
	{
		return reader;
	}

	
	public String[] getValues()
	{
		return values;
	}

	public void setValues(String[] values)
	{
		this.values = values;
	}

	public int getRowIndex()
	{
		return rowIndex;
	}
}
