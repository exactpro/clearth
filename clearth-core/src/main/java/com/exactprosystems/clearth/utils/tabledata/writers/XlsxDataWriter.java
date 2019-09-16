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

package com.exactprosystems.clearth.utils.tabledata.writers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;

public class XlsxDataWriter extends AbstractExcelDataWriter 
{
	public XlsxDataWriter(TableHeader<String> header, File f, String sheetName, boolean appendSheet,  boolean needHeader, boolean appendFile) 
			throws IOException, InvalidFormatException
	{
		super(header, f, sheetName, appendSheet, needHeader, appendFile);
	}

	/**
	 * Writes whole table to given file, closing writer after that
	 * @param table to write data from
	 * @param f file to write data to
	 * @param sheetName sheet to write data to 
	 * @param appendSheet flag which indicates if existing sheet should be appended or overwritten	
	 * @param appendFile flag which indicates if existing file should be appended or overwritten
	 * @throws IOException if error occurs while writing data
	 * @throws InvalidFormatException
	 */
	public static void write(StringTableData table, File f, String sheetName, boolean appendSheet,  boolean needHeader, boolean appendFile) 
			throws IOException, InvalidFormatException
	{
		XlsxDataWriter writer = null;
		try
		{
			writer = new XlsxDataWriter(table.getHeader(), f, sheetName, appendSheet, needHeader, appendFile);
			writer.write(table.getRows());
		}
		finally
		{
			Utils.closeResource(writer);
		}
	}

	public static void write(StringTableData table, File f, String sheetName,  boolean needHeader, boolean appendFile) 
			throws IOException, InvalidFormatException
	{
		write(table, f, sheetName, false,  needHeader, appendFile);
	}

	@Override
	public Workbook createWorkbook(FileInputStream in, boolean appendFile) throws IOException, InvalidFormatException
	{
		if (!appendFile)
			return createWorkbook();
		
		try
		{
			return new XSSFWorkbook(in);
		} 
		catch (Exception e)
		{
			logger.warn("Unable to read data from file. File is not a valid XML file. New woorkbook will be created.", e);
		}
		finally
		{
			Utils.closeResource(in);
		}
		return createWorkbook();
	}

	public Workbook createWorkbook() throws IOException, InvalidFormatException
	{
		return new XSSFWorkbook();
	}
}
