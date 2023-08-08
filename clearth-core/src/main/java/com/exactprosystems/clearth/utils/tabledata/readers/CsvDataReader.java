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

import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

/**
 * Reader of table-like data from CSV data source
 * @author vladimir.panarin
 */
public class CsvDataReader extends AbstractStringCsvDataReader<StringTableData>
{
	public CsvDataReader(File f) throws IOException
	{
		super(f);
	}

	public CsvDataReader(Reader reader) throws IOException
	{
		super(reader);
	}

	public CsvDataReader(File f, ClearThCsvReaderConfig config) throws IOException
	{
		super(f, config);
	}
	
	public CsvDataReader(Reader reader, ClearThCsvReaderConfig config) throws IOException
	{
		super(reader, config);
	}

	/**
	 * Reads whole CSV file, closing reader after that
	 * @param f CSV file to read data from
	 * @return TableData object with header that corresponds to CSV file and rows that contain all data
	 * @throws IOException if error occurs while reading data
	 */
	public static StringTableData read(File f) throws IOException
	{
		return read(f, defaultCsvReaderConfig());
	}

	/**
	 * Reads whole CSV file, closing reader after that
	 * @param f CSV file to read data from
	 * @param config {@link ClearThCsvReaderConfig} object to configure the reader
	 * @return TableData object with header that corresponds to CSV file and rows that contain all data
	 * @throws IOException if error occurs while reading data
	 */
	public static StringTableData read(File f, ClearThCsvReaderConfig config) throws IOException
	{
		try (CsvDataReader reader = new CsvDataReader(f, config))
		{
			return reader.readAllData();
		}
	}
	
	/**
	 * Reads CSV data by using given dataReader
	 * @param dataReader CSV data reader to read data from
	 * @return TableData object with header that corresponds to CSV and rows that contain all data
	 * @throws IOException if error occurs while reading data
	 */
	public static StringTableData read(Reader dataReader) throws IOException
	{
		return read(dataReader, defaultCsvReaderConfig());
	}

	/**
	 * Reads CSV data by using given dataReader
	 * @param dataReader CSV data reader to read data from
	 * @param config {@link ClearThCsvReaderConfig} object to configure the reader
	 * @return TableData object with header that corresponds to CSV and rows that contain all data
	 * @throws IOException if error occurs while reading data
	 */
	public static StringTableData read(Reader dataReader, ClearThCsvReaderConfig config) throws IOException
	{
		try (CsvDataReader reader = new CsvDataReader(dataReader, config))
		{
			return reader.readAllData();
		}
	}
	
	@Override
	protected StringTableData createTableData(Set<String> header, RowsListFactory<String, String> rowsListFactory)
	{
		return new StringTableData(header, rowsListFactory);
	}
}
