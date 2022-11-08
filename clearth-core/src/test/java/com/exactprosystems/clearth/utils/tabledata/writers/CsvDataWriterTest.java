/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static org.testng.Assert.assertEquals;

public class CsvDataWriterTest
{
	private static final Path OUTPUT_PATH = Paths.get("testOutput"),
			EXPECTED_FILES_PATH = Paths.get("TableDataTest");
	private static final File EXPECTED_CSV_FILE = EXPECTED_FILES_PATH.resolve("expectedCsv.csv").toFile(),
			EXPECTED_CSV_HEADER_FILE = EXPECTED_FILES_PATH.resolve("expectedHeaderCsv.csv").toFile();
	private TableHeader<String> header;
	private File outputHeaderFile, expectedHeaderCsv, outputCsvFile, expectedCsvFile;
	private List<String> buildValues, buildGapValues, buildEmptyValues;
	private List<TableRow<String, String>> buildRows;
	private StringTableData buildDataForWriting;

	@BeforeClass
	public void init() throws IOException
	{
		createDir(OUTPUT_PATH);

		header = buildHeader();
		buildValues = buildValues();
		buildGapValues = buildGapValues();
		buildEmptyValues = buildEmptyValues();
		buildRows = buildRows(header);
		buildDataForWriting = buildDataForWriting();
		
		outputCsvFile = OUTPUT_PATH.resolve("outputCsv.csv").toFile();
		outputHeaderFile = OUTPUT_PATH.resolve("outputHeaderCsv.csv").toFile();
		expectedHeaderCsv = new File(resourceToAbsoluteFilePath(EXPECTED_CSV_HEADER_FILE.getPath()));
		expectedCsvFile = new File(resourceToAbsoluteFilePath(EXPECTED_CSV_FILE.getPath()));
	}

	private void createDir(Path dir) throws IOException
	{
		if (!dir.toFile().exists())
			Files.createDirectories(dir);
	}

	private void forWriteHeaderTests() throws IOException
	{
		try
		{
			String actualTableData = FileUtils.readFileToString(outputHeaderFile, Utils.UTF8);
			String expectedTableData = FileUtils.readFileToString(expectedHeaderCsv, Utils.UTF8);
			assertEquals(actualTableData, expectedTableData);
		}
		finally
		{
			outputHeaderFile.delete();
		}
	}

	private void forWriterTests() throws IOException
	{
		try
		{
			String actualTableData = FileUtils.readFileToString(outputCsvFile, Utils.UTF8);
			String expectedTableData = FileUtils.readFileToString(expectedCsvFile, Utils.UTF8);
			
			assertEquals(actualTableData, expectedTableData);
		}
		finally
		{
			outputCsvFile.delete();
		}
	}

	@Test (expectedExceptions = IllegalStateException.class)
	public void testWriteRowAndHeaderToFile() throws IOException
	{
		try (CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputCsvFile), true))
		{
			writer.write(buildRows);
			writer.writeHeader();
		}
		finally
		{
			outputCsvFile.delete();
		}
	}

	@Test (expectedExceptions = IllegalStateException.class)
	public void testWriteTwoHeadersToFile() throws IOException
	{
		try(CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputHeaderFile), true))
		{
			writer.writeHeader();
			writer.writeHeader();
		}
		finally
		{
			outputHeaderFile.delete();
		}
	}

	@Test
	public void testWriteHeaderFile() throws IOException
	{
		try(CsvDataWriter writer = new CsvDataWriter(header, outputHeaderFile, true, true))
		{
			writer.writeHeader();
		}
		forWriteHeaderTests();
	}

	@Test
	public void testWriteHeaderWriter() throws IOException
	{
		try (CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputHeaderFile), true))
		{
			writer.writeHeader();
		}
		forWriteHeaderTests();
	}

	@Test
	public void testWrite() throws IOException
	{
		CsvDataWriter.write(buildDataForWriting, new FileWriter(outputCsvFile), true);
		forWriterTests();
	}

	@Test
	public void testWrite1() throws IOException
	{
		CsvDataWriter.write(buildDataForWriting, outputCsvFile, true, false);
		forWriterTests();
	}

	@Test
	public void testWriteRow() throws IOException
	{
		try (CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputCsvFile), true))
		{
			for (TableRow<String, String> row : buildRows)
			{
				writer.writeRow(row);
			}
		}
		forWriterTests();
	}

	@Test
	public void testWriteRows() throws IOException
	{
		try (CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputCsvFile), true))
		{
			writer.writeRows(buildRows);
		}
		forWriterTests();
	}

	@Test
	public void testWriteHeader() throws IOException
	{
		try (CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputCsvFile), true))
		{
			writer.writeHeader();
		}
		
		try
		{
			String actualTableData = FileUtils.readFileToString(outputCsvFile, Utils.UTF8);
			String expectedTableData = FileUtils.readFileToString(expectedHeaderCsv, Utils.UTF8);
			
			assertEquals(actualTableData, expectedTableData);
		}
		finally
		{
			outputCsvFile.delete();
		}
	}


	public static TableHeader<String> buildHeader()
	{
		return new TableHeader<>(buildHeaderSet());
	}

	public StringTableData buildDataForWriting()
	{
		StringTableData tableData = new StringTableData(header);
		tableData.add(new TableRow<String, String>(header, buildValues));
		tableData.add(new TableRow<String, String>(header, buildEmptyValues));
		tableData.add(new TableRow<String, String>(header, buildValues));
		tableData.add(new TableRow<String, String>(header, buildGapValues));
		return tableData;
	}

	public static Set<String> buildHeaderSet()
	{
		Set<String> headerSet = new LinkedHashSet<>();
		headerSet.add("Param1");
		headerSet.add("Param2");
		headerSet.add("");
		headerSet.add("Param4");
		return headerSet;
	}

	private List<TableRow<String, String>> buildRows(TableHeader<String> header)
	{
		List<TableRow<String, String>> rowList = new ArrayList<>();
		rowList.add(new TableRow<>(header, buildValues));
		rowList.add(new TableRow<>(header, null));
		rowList.add(new TableRow<>(header, buildValues));
		rowList.add(new TableRow<>(header, buildGapValues));
		return rowList;
	}

	private List<String> buildValues()
	{
		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("Value1");
		expectedValues.add("");
		expectedValues.add("Value3");
		expectedValues.add("Value4");
		return expectedValues;
	}

	private List<String> buildEmptyValues()
	{
		List<String> emptyValues = new ArrayList<>();
		emptyValues.add("");
		emptyValues.add("");
		emptyValues.add("");
		emptyValues.add("");
		return emptyValues;
	}

	private List<String> buildGapValues()
	{
		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("Val ue1");
		expectedValues.add(" ");
		expectedValues.add(" Value3 ");
		expectedValues.add("Value4");
		return expectedValues;
	}
}