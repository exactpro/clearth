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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.utils.tabledata.TableDataWriter;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

public abstract class AbstractExcelDataWriter extends TableDataWriter<String, String>
{
	public static final Logger logger = LoggerFactory.getLogger(AbstractExcelDataWriter.class);
	protected final Workbook wb;
	protected final OutputStream out;
	protected Sheet sheet;

	protected boolean needHeader;
	protected boolean overwriteSheet;

	protected int rowIndex = 0;

	public AbstractExcelDataWriter(TableHeader<String> header, File f, String sheetName, boolean appendSheet,  boolean needHeader, boolean appendFile) 
			throws IOException, InvalidFormatException
	{
		super(header);
		
		if(f.exists())
		{
			this.wb = createWorkbook(new FileInputStream(f), appendFile);
		}
		else 
		{
			this.wb = createWorkbook();
		}
		this.out = new FileOutputStream(f, false);
		createSheet(wb, sheetName, appendSheet);
		this.needHeader = needHeader;
	}

	@Override
	public void close() throws IOException
	{
		wb.write(out);
		wb.close();
		out.close();
	}

	@Override
	protected int writeRow(TableRow<String, String> row)
	{
		writeNeededHeader();
		doWriteRow(row);
		return rowIndex;
	}

	@Override
	protected int writeRows(Collection<TableRow<String, String>> rows)
	{
		writeNeededHeader();
		for (TableRow<String, String> r : rows)
		{
			doWriteRow(r);
		}
		return rowIndex;
	}

	protected void writeHeader()
	{
		writeIterableToRow(header);
	}

	protected void writeNeededHeader()
	{
		if (!needHeader)
			return;

		writeHeader();
		needHeader = false;
	}

	protected void doWriteRow(TableRow<String, String> row)
	{
		writeIterableToRow(row);
	}

	protected void writeIterableToRow(Iterable<String> row)
	{
		Row r = sheet.createRow(rowIndex);
		Cell c;

		int cellIndex = 0;

		for (String v : row)
		{
			c = r.createCell(cellIndex);
			c.setCellValue(v);
			cellIndex++;
		}
		rowIndex++;
	}

	private void createSheet(Workbook wb, String sheetName, boolean appendSheet)
	{
		if(wb.getSheet(sheetName) != null)
		{
			if(appendSheet)
			{
				sheet = wb.getSheet(sheetName);
				rowIndex = sheet.getPhysicalNumberOfRows();
			}
			else
			{
				wb.removeSheetAt(wb.getSheetIndex(sheetName));
				sheet = wb.createSheet(sheetName);
			}
		}
		else
		{
			sheet = wb.createSheet(sheetName);
		}
	}

	public abstract Workbook createWorkbook(FileInputStream in, boolean appendFile) throws IOException, InvalidFormatException;

	public abstract Workbook createWorkbook() throws IOException, InvalidFormatException;
}
