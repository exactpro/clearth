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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.exactprosystems.clearth.utils.tabledata.BasicTableData;
import com.exactprosystems.clearth.utils.tabledata.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

public abstract class AbstractExcelDataReader<C extends BasicTableData<String, String>> extends BasicTableDataReader<String, String, C>
{
	protected final Workbook wb;
	protected final String sheetName;
	protected ExcelRowFilter excelRowFilter;
	
	protected Sheet sheet;
	protected Row row;

	protected int rowIndex = 0;

	//Used to handle headers duplication. 
	//If duplication is detected, the first column will be used.
	protected List<String> headerInds;

	protected DataFormatter df = new DataFormatter();

	public AbstractExcelDataReader(File f, String sheetName) throws IOException, InvalidFormatException
	{
		this.wb = createWorkbook(f);
		this.sheetName = sheetName;
	}

	public AbstractExcelDataReader(FileInputStream in, String sheetName) throws IOException
	{
		this.wb = createWorkbook(in);
		this.sheetName = sheetName;
	}


	public void setExcelRowFilter(ExcelRowFilter excelRowFilter)
	{
		this.excelRowFilter = excelRowFilter;
	}

	@Override
	public void close() throws IOException
	{
		wb.close();
	}

	@Override
	protected Set<String> readHeader()
	{
		sheet = wb.getSheet(sheetName);
		rowIndex = sheet.getFirstRowNum();
		row = sheet.getRow(rowIndex);
		
		Set<String> headers = new LinkedHashSet<String>();
		
		this.headerInds = new ArrayList<String>();
		
		String tmp;
		
		for(Cell c : row)
		{
			if (c != null) 
			{
				headers.add(tmp = c.getStringCellValue());
				headerInds.add(tmp);
			} 
			else
			{ 
				headerInds.add(null);
			}
		}
		rowIndex++;
		return headers;
	}

	@Override
	public boolean hasMoreData()
	{
		return sheet.getLastRowNum() >= rowIndex;
	}

	@Override
	protected void fillRow(TableRow<String, String> newRow)
	{
		row = sheet.getRow(rowIndex);

		for (int i = 0, length = this.headerInds.size(); i < length; ++i) 
		{
			String headerName = this.headerInds.get(i);
			if(row != null)
			{
				//Handling case with duplicate column names in XLS/XLSX. Only first occurrence will be used
				if(headerName != null && newRow.getHeader().columnIndex(headerName) != -1) 
				{
					Cell c = row.getCell(i);
					newRow.setValue(headerName, c == null ? "" : df.formatCellValue(c));
				}
			}
			else
			{
				newRow.setValue(headerName, "");
			}
		}
		rowIndex++;
	}
	
	@Override
	public boolean filter()
	{
		return (excelRowFilter == null) || excelRowFilter.filter(row);
	}

	public abstract Workbook createWorkbook(File f) throws IOException, InvalidFormatException;

	public abstract Workbook createWorkbook(FileInputStream in) throws IOException;
}
