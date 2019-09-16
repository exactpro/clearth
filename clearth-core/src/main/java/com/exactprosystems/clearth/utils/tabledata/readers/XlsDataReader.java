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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;

public class XlsDataReader extends AbstractExcelDataReader<StringTableData>
{
	public XlsDataReader(File f, String sheetName) throws IOException, InvalidFormatException
	{
		super(f, sheetName);
	}

	public XlsDataReader(FileInputStream in, String sheetName) throws IOException
	{
		super(in, sheetName);
	}

	@Override
	public Workbook createWorkbook(File f) throws IOException
	{
		FileInputStream in = new FileInputStream(f);
		return new HSSFWorkbook(in);
	}

	@Override
	public Workbook createWorkbook(FileInputStream in) throws IOException
	{
		return new HSSFWorkbook(in);
	}

	/**
	 * Reads whole XLS file, closing reader after that
	 * @param f XLS file to read data from
	 * @return TableData object with header that corresponds to XLS file and rows that contain all data
	 * @throws IOException if error occurs while reading data
	 * @throws InvalidFormatException
	 */
	public static StringTableData read(File f, String sheetName) throws IOException, InvalidFormatException
	{
		XlsDataReader reader = null;
		try
		{
			reader = new XlsDataReader(f, sheetName);
			return reader.readAllData();
		}
		finally
		{
			Utils.closeResource(reader);
		}
	}

	/**
	 * Reads XLS data by using given FileInputStream
	 * @param in FileInputStream to read data from
	 * @return TableData object with header that corresponds to Xls and rows that contain all data
	 * @throws IOException if error occurs while reading data
	 */
	public static StringTableData read(FileInputStream in, String sheetName) throws IOException
	{
		XlsDataReader reader = null;
		try
		{
			reader = new XlsDataReader(in, sheetName);
			return reader.readAllData();
		}
		finally
		{
			Utils.closeResource(reader);
		}
	}

	@Override
	protected StringTableData createTableData(Set<String> header, RowsListFactory<String, String> rowsListFactory)
	{
		return new StringTableData(header, rowsListFactory);
	}
}
