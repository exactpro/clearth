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

import com.exactprosystems.clearth.utils.tabledata.IndexedTableData;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.primarykeys.PrimaryKey;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

public class IndexedXlsxDataReader<C extends PrimaryKey> extends AbstractExcelDataReader<IndexedTableData<String, String, C>> {
	
	protected final TableRowMatcher<String, String, C> matcher;

	public IndexedXlsxDataReader(File f, String sheetName, TableRowMatcher<String, String, C> matcher) throws IOException, InvalidFormatException
	{
		super(f, sheetName);
		this.matcher = matcher; 
	}

	public IndexedXlsxDataReader(FileInputStream in, String sheetName, TableRowMatcher<String, String, C> matcher) throws IOException
	{
		super(in, sheetName);
		this.matcher = matcher; 
	}

	@Override
	protected IndexedTableData<String, String, C> createTableData(Set<String> header,
	                                                              RowsListFactory<String, String> rowsListFactory)
	{
		return new IndexedTableData<String, String, C>(header, matcher, rowsListFactory); 
	}

	@Override
	public Workbook createWorkbook(File f) throws IOException, InvalidFormatException
	{
		return new XSSFWorkbook(f);
	}

	@Override
	public Workbook createWorkbook(FileInputStream in) throws IOException
	{
		return new XSSFWorkbook(in);
	}
}
