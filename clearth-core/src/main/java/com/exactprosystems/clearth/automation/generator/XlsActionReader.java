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

import com.exactprosystems.clearth.automation.ActionGenerator;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XlsActionReader extends ActionReader
{
	private final InputStream inputStream;
	private final Workbook workbook;
	private final Sheet sheet;
	private final Iterator<Row> rowIterator;
	private Row row;
	private String firstCellValue;

	public XlsActionReader(String source, boolean trimValues) throws IOException
	{
		super(source, trimValues);
		this.inputStream = createInputStream(source);
		this.workbook = createWorkbook(source, inputStream);
		this.sheet = getSheetToRead(workbook);
		this.rowIterator = this.sheet.iterator();
	}
	

	@Override
	public void close() throws IOException
	{
		Utils.closeResource(inputStream);
	}
	
	@Override
	public boolean readNextLine() throws IOException
	{
		while (rowIterator.hasNext())
		{
			row = rowIterator.next();
			if (!isRowToSkip(row))
			{
				firstCellValue = getCellDataAsString(row.getCell(row.getFirstCellNum()));
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean isCommentLine()
	{
		return firstCellValue.trim().startsWith(ActionGenerator.COMMENT_INDICATOR);
	}

	@Override
	public boolean isHeaderLine()
	{
		return firstCellValue.trim().startsWith(ActionGenerator.HEADER_DELIMITER);
	}
	
	@Override
	public boolean isEmptyLine()
	{
		for (Cell cell : row)
		{
			if (cell != null && !cell.getCellType().equals(CellType.BLANK) && StringUtils.isNotBlank(cell.toString()))
				return false;
		}
		return true;
	}
	
	@Override
	public List<String> parseLine(boolean header) throws IOException
	{
		List<String> result = new ArrayList<String>();
		for (Cell cell : row)
		{
			String value = getCellDataAsString(cell);
			value = processValue(value, header);
			result.add(value);
		}
		return result;
	}

	@Override
	public String getRawLine() throws IOException
	{
		return firstCellValue;
	}

	protected InputStream createInputStream(String source) throws IOException
	{
		return new FileInputStream(source);
	}
	
	protected Workbook createWorkbook(String source, InputStream is) throws IOException
	{
		return source.toLowerCase().endsWith("xls") ? new HSSFWorkbook(is) : new XSSFWorkbook(is);
	}
	
	protected Sheet getSheetToRead(Workbook workbook)
	{
		return workbook.getSheetAt(0);
	}


	public InputStream getInputStream()
	{
		return inputStream;
	}

	public Workbook getWorkbook()
	{
		return workbook;
	}

	public Sheet getSheet()
	{
		return sheet;
	}
	
	
	protected boolean isRowToSkip(Row row)
	{
		return (row == null) || (row.getFirstCellNum() < 0) || (row.getLastCellNum() <= 0);
	}

	protected String getCellDataAsString(Cell cell)
	{
		cell.setCellType(CellType.STRING);
		String data = cell.getStringCellValue();
		if (isTrimValues())
			return data.trim();
		else
			return data;
	}

	public int getRowIndex()
	{
		return row.getRowNum() + 1;
	}
}
