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
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	public static final String OUTPUT_CSV_FILE = "testOutput/outputCsv.csv";
	private static final String EXPECTED_CSV_FILE = "TableDataTest/expectedCsv.csv";
	private static final String EXPECTED_CSV_HEADER_FILE = "TableDataTest/expectedHeaderCsv.csv";

	@Test
	public void testWrite() throws IOException
	{
		File outputFile = Paths.get(OUTPUT_CSV_FILE).toFile();
		CsvDataWriter.write(buildDataForWriting(), new FileWriter(outputFile), true);

		String actualTableData = FileUtils.readFileToString(Paths.get(OUTPUT_CSV_FILE).toFile(),
				Utils.UTF8);
		String expectedTableData = FileUtils.readFileToString(Paths.get(resourceToAbsoluteFilePath(EXPECTED_CSV_FILE)).toFile(),
				Utils.UTF8);

		assertEquals(actualTableData, expectedTableData);
		outputFile.delete();
	}

	@Test
	public void testWrite1() throws IOException
	{
		File outputFile = Paths.get(OUTPUT_CSV_FILE).toFile();
		CsvDataWriter.write(buildDataForWriting(), outputFile, true, false);
		String actualTableData = FileUtils.readFileToString(outputFile, Utils.UTF8);
		String expectedTableData = FileUtils.readFileToString(Paths.get(resourceToAbsoluteFilePath(EXPECTED_CSV_FILE)).toFile(),
				Utils.UTF8);

		assertEquals(actualTableData, expectedTableData);
		outputFile.delete();
	}

	@Test
	public void testWriteRow() throws IOException
	{
		File outputFile = Paths.get(OUTPUT_CSV_FILE).toFile();
		TableHeader<String> header = buildHeader();
		try (CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputFile), true))
		{
			List<TableRow<String, String>> rows = buildRows(header);
			for (TableRow<String, String> row : rows)
			{
				writer.writeRow(row);
			}
		}
		
		String actualTableData = FileUtils.readFileToString(outputFile, Utils.UTF8);
		String expectedTableData = FileUtils.readFileToString(Paths.get(resourceToAbsoluteFilePath(EXPECTED_CSV_FILE)).toFile(),
				Utils.UTF8);

		assertEquals(actualTableData, expectedTableData);
		outputFile.delete();
	}

	@Test
	public void testWriteRows() throws IOException
	{
		File outputFile = Paths.get(OUTPUT_CSV_FILE).toFile();
		TableHeader<String> header = buildHeader();
		try (CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputFile), true))
		{
			writer.writeRows(buildRows(header));
		}
		String actualTableData = FileUtils.readFileToString(outputFile, Utils.UTF8);
		String expectedTableData = FileUtils.readFileToString(Paths.get(resourceToAbsoluteFilePath(EXPECTED_CSV_FILE)).toFile(),
				Utils.UTF8);

		assertEquals(actualTableData, expectedTableData);
		outputFile.delete();
	}

	@Test
	public void testWriteHeader() throws IOException
	{
		File outputFile = Paths.get(OUTPUT_CSV_FILE).toFile();
		Path expectedFile = Paths.get(resourceToAbsoluteFilePath(EXPECTED_CSV_HEADER_FILE));
		TableHeader<String> header = buildHeader();
		try (CsvDataWriter writer = new CsvDataWriter(header, new FileWriter(outputFile), true))
		{
			writer.writeHeader();
		}

		String actualTableData = FileUtils.readFileToString(outputFile, Utils.UTF8);
		String expectedTableData = FileUtils.readFileToString(expectedFile.toFile(), Utils.UTF8);

		assertEquals(actualTableData, expectedTableData);
		outputFile.delete();
	}

	public static TableHeader<String> buildHeader()
	{
		return new TableHeader<>(buildHeaderSet());
	}

	public StringTableData buildDataForWriting()
	{
		TableHeader<String> header = buildHeader();
		StringTableData tableData = new StringTableData(header);
		tableData.add(new TableRow<String, String>(header, buildValues()));
		tableData.add(new TableRow<String, String>(header, buildEmptyValues()));
		tableData.add(new TableRow<String, String>(header, buildValues()));
		tableData.add(new TableRow<String, String>(header, buildGapValues()));
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

	public static List<TableRow<String, String>> buildRows(TableHeader<String> header)
	{
		List<TableRow<String, String>> rowList = new ArrayList<TableRow<String, String>>();
		rowList.add(new TableRow<String, String>(header, buildValues()));
		rowList.add(new TableRow<String, String>(header, null));
		rowList.add(new TableRow<String, String>(header, buildValues()));
		rowList.add(new TableRow<String, String>(header, buildGapValues()));
		return rowList;
	}

	public static List<String> buildValues()
	{
		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("Value1");
		expectedValues.add("");
		expectedValues.add("Value3");
		expectedValues.add("Value4");
		return expectedValues;
	}

	private static List<String> buildEmptyValues()
	{
		List<String> emptyValues = new ArrayList<>();
		emptyValues.add("");
		emptyValues.add("");
		emptyValues.add("");
		emptyValues.add("");
		return emptyValues;
	}

	public static List<String> buildGapValues()
	{
		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("Val ue1");
		expectedValues.add(" ");
		expectedValues.add(" Value3 ");
		expectedValues.add("Value4");
		return expectedValues;
	}
}