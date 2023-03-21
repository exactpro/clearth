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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.tabledata.BasicTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

import java.io.*;

public abstract class AbstractFixedLengthDataReader<C extends BasicTableData<String, String>> extends BasicTableDataReader<String, String, C>
{
	protected BufferedReader reader;
	protected FixedLengthRowFilter rowFilter;
	protected String currentLine;
	
	public AbstractFixedLengthDataReader(File file) throws IOException
	{
		reader = createReader(file);
	}
	
	public AbstractFixedLengthDataReader(Reader reader)
	{
		this.reader = createReader(reader);
	}
	
	public void setFixLengthRowFilter(FixedLengthRowFilter rowFilter)
	{
		this.rowFilter = rowFilter;
	}
	
	
	@Override
	public boolean hasMoreData() throws IOException
	{
		currentLine = readNextLine();
		return currentLine != null;
	}
	
	protected String readNextLine() throws IOException
	{
		return reader.readLine();
	}
	
	@Override
	public boolean filter() throws IOException
	{
		return rowFilter == null || rowFilter.filter(currentLine);
	}
	
	@Override
	protected void fillRow(TableRow<String, String> row) throws IOException
	{
		for (String column : row.getHeader())
			row.setValue(column, parseColumnValue(column, currentLine));
	}
	
	protected abstract String parseColumnValue(String column, String line) throws IOException;
	
	@Override
	public void close() throws IOException
	{
		if (reader != null)
			reader.close();
	}
	
	
	protected BufferedReader createReader(File file) throws IOException
	{
		return new BufferedReader(new FileReader(file));
	}
	
	protected BufferedReader createReader(Reader reader)
	{
		return new BufferedReader(reader);
	}
}
