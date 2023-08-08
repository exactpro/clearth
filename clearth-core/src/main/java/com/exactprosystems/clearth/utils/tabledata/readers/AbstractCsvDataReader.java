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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.tabledata.BasicTableData;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

public abstract class AbstractCsvDataReader<A, B, C extends BasicTableData<A, B>> extends BasicTableDataReader<A, B, C>
{
	protected final ClearThCsvReader reader;
	protected CsvRowFilter csvRowFilter;

	public AbstractCsvDataReader(File f) throws IOException
	{
		reader = createReader(f, createCsvReaderConfig());
	}

	public AbstractCsvDataReader(Reader reader) throws IOException
	{
		this.reader = createReader(reader, createCsvReaderConfig());
	}

	public AbstractCsvDataReader(File f, ClearThCsvReaderConfig config) throws IOException
	{
		reader = createReader(f, config);
	}

	public AbstractCsvDataReader(Reader reader, ClearThCsvReaderConfig config) throws IOException
	{
		this.reader = createReader(reader, config);
	}

	public void setCsvRowFilter(CsvRowFilter csvRowFilter)
	{
		this.csvRowFilter = csvRowFilter;
	}
	
	@Override
	public void close() throws IOException
	{
		reader.close();
	}
	
	@Override
	public boolean hasMoreData() throws IOException
	{
		return reader.hasNext();
	}
	

	@Override
	public boolean filter() throws IOException
	{
		return (csvRowFilter == null) || csvRowFilter.filter(reader.getRecord());
	}

	protected ClearThCsvReader createReader(File f, ClearThCsvReaderConfig config) throws IOException
	{
		return new ClearThCsvReader(f.getAbsolutePath(), config);
	}

	protected ClearThCsvReader createReader(Reader reader, ClearThCsvReaderConfig config) throws IOException
	{
		return new ClearThCsvReader(reader, config);
	}

	protected ClearThCsvReaderConfig createCsvReaderConfig()
	{
		return defaultCsvReaderConfig();
	}

	public static ClearThCsvReaderConfig defaultCsvReaderConfig()
	{
		ClearThCsvReaderConfig config = new ClearThCsvReaderConfig();
		config.setSkipEmptyRecords(true);
		config.setFirstLineAsHeader(true);
		config.setIgnoreSurroundingSpaces(true);
		return config;
	}
}
