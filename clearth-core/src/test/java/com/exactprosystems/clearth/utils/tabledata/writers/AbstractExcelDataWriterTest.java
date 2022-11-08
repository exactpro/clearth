/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

public class AbstractExcelDataWriterTest
{
	private static final Path OUTPUT_PATH = Paths.get("testOutput"),
			EXPECTED_FILES_PATH = Paths.get("TableDataTest");
	private static TableHeader<String> header;
	private static TableRow<String, String> row;
	
	private final File fileXlsx = OUTPUT_PATH.resolve("file1.xls").toFile(), 
			fileXls = OUTPUT_PATH.resolve("file2.xlsx").toFile(),
			expectedFileXls = EXPECTED_FILES_PATH.resolve("expectedXls.xls").toFile(),
			expectedFileXlsx = EXPECTED_FILES_PATH.resolve("expectedXlsx.xlsx").toFile();
	private String sheetName = "sheet1";

	@BeforeClass
	public static void init() throws IOException
	{
		createDir(OUTPUT_PATH);

		Set<String> setHeader = new LinkedHashSet<>();
		setHeader.add("Param1");
		setHeader.add("Param2");
		setHeader.add("Param3");
		setHeader.add("Param4");

		List<String> listValues = new ArrayList<>();
		listValues.add("1");
		listValues.add("2");
		listValues.add("3");
		listValues.add("4");

		header = new TableHeader<>(setHeader);
		row = new TableRow<>(header, listValues);
	}

	private static void createDir(Path dir) throws IOException
	{
		if (!dir.toFile().exists())
			Files.createDirectories(dir);
	}
	
	
	@Test(expected = IllegalStateException.class)
	public void testWriteWriteRowAndHeaderToXlsxFile() throws IOException, InvalidFormatException
	{
		try(XlsxDataWriter xlsx_tdw = new XlsxDataWriter(header, fileXlsx, sheetName, true, true, true))
		{
			xlsx_tdw.write(row);
			xlsx_tdw.writeHeader();
		}
		finally
		{
			fileXlsx.delete();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteWriteRowAndHeadersToXlsFile() throws IOException, InvalidFormatException
	{
		try(XlsDataWriter xls_tdw = new XlsDataWriter(header, fileXls, sheetName, true, true, true))
		{
			xls_tdw.write(row);
			xls_tdw.writeHeader();
		}
		finally
		{
			fileXls.delete();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteTwoHeadersToXlsxFile() throws IOException, InvalidFormatException
	{
		try(XlsxDataWriter xlsx_tdw = new XlsxDataWriter(header, fileXlsx, sheetName, true, true, true))
		{
			xlsx_tdw.writeHeader();
			xlsx_tdw.writeHeader();
		}
		finally
		{
			fileXlsx.delete();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteTwoHeadersToXlsFile() throws IOException, InvalidFormatException
	{
		try(XlsDataWriter xls_tdw = new XlsDataWriter(header, fileXls, sheetName, true, true, true))
		{
			xls_tdw.writeHeader();
			xls_tdw.writeHeader();
		}
		finally
		{
			fileXls.delete();
		}
	}

	@Test
	public void testWriteHeaderXlsx() throws IOException, InvalidFormatException
	{
		try(XlsxDataWriter xlsx_tdw = new XlsxDataWriter(header, fileXlsx, sheetName, true, true, true))
		{
			xlsx_tdw.writeHeader();
		}
		
		try
		{
			String actualFile = readXlsx(fileXlsx);
			String expectedFile = readXlsx(new File(resourceToAbsoluteFilePath(expectedFileXlsx.getPath())));
			
			Assert.assertEquals(expectedFile, actualFile);
		}
		finally
		{
			fileXlsx.delete();
		}
	}

	@Test
	public void testWriteHeaderXls() throws IOException, InvalidFormatException
	{
		try(XlsDataWriter xls_tdw = new XlsDataWriter(header, fileXls,sheetName,true, true, true))
		{
			xls_tdw.writeHeader();
		}
		
		try
		{
			String actualFile = readXls(fileXls);
			String expectedFile = readXls(new File(resourceToAbsoluteFilePath(expectedFileXls.getPath())));
			
			Assert.assertEquals(expectedFile, actualFile);
		}
		finally
		{
			fileXls.delete();
		}
	}

	private String readXls(File file) throws IOException
	{
		try (HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(file)) )
		{
			return readWorkbook(workbook);
		}
	}

	private String readXlsx(File file) throws IOException
	{
		try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(file)))
		{
			return readWorkbook(workbook);
		}
	}

	private String readWorkbook(Workbook workbook)
	{
		Row row = workbook.getSheet(sheetName).getRow(0);

		CommaBuilder builder = new CommaBuilder();
		for (int i = 0; i < row.getLastCellNum(); i++)
		{
			Cell cell = row.getCell(i);
			if (cell.getCellType() == CellType.STRING)
			{
				String name = cell.getStringCellValue();
				builder.append(name);
			}
		}
		return builder.toString();
	}
}