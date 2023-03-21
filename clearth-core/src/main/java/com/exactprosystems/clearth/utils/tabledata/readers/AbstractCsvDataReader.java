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

import com.csvreader.CsvReader;
import com.exactprosystems.clearth.utils.tabledata.BasicTableData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

public abstract class AbstractCsvDataReader<A, B, C extends BasicTableData<A, B>> extends BasicTableDataReader<A, B, C>
{
	protected final CsvReader reader;
	protected CsvRowFilter csvRowFilter;
	
	public AbstractCsvDataReader(File f) throws FileNotFoundException
	{
		reader = createReader(f);
	}
	
	public AbstractCsvDataReader(Reader reader)
	{
		this.reader = createReader(reader);
	}
	
	
	public void setCsvRowFilter(CsvRowFilter csvRowFilter)
	{
		this.csvRowFilter = csvRowFilter;
	}
	
	public void setDelimiter(char delimiter)
	{
		this.reader.setDelimiter(delimiter);
	}
	
	@Override
	public void close() throws IOException
	{
		reader.close();
	}
	
	@Override
	public boolean hasMoreData() throws IOException
	{
		return reader.readRecord();
	}
	
	
	@Override
	public boolean filter() throws IOException
	{
		return (csvRowFilter == null) || csvRowFilter.filter(reader);
	}
	
	
	protected CsvReader createReader(File f) throws FileNotFoundException
	{
		return new CsvReader(f.getAbsolutePath());
	}
	
	protected CsvReader createReader(Reader reader)
	{
		return new CsvReader(reader);
	}
}
