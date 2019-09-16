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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;

/**
 * Reader of table-like data from CSV data source
 * @author vladimir.panarin
 */
public class CsvDataReader extends AbstractCsvDataReader<StringTableData>
{
	public CsvDataReader(File f) throws FileNotFoundException
	{
		super(f);
	}
	
	public CsvDataReader(Reader reader) throws FileNotFoundException
	{
		super(reader);
	}
	
	
	/**
	 * Reads whole CSV file, closing reader after that
	 * @param f CSV file to read data from
	 * @return TableData object with header that corresponds to CSV file and rows that contain all data
	 * @throws IOException if error occurs while reading data
	 */
	public static StringTableData read(File f) throws IOException
	{
		CsvDataReader reader = null;
		try
		{
			reader = new CsvDataReader(f);
			return reader.readAllData();
		}
		finally
		{
			if (reader != null)
				reader.close();
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
		CsvDataReader reader = null;
		try
		{
			reader = new CsvDataReader(dataReader);
			return reader.readAllData();
		}
		finally
		{
			if (reader != null)
				reader.close();
		}
	}
	
	@Override
	protected StringTableData createTableData(Set<String> header, RowsListFactory<String, String> rowsListFactory)
	{
		return new StringTableData(header, rowsListFactory);
	}
}
