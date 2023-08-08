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
package com.exactprosystems.clearth.utils.tabledata.writers;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class CsvDataWriterTest
{
	private static final Path OUTPUT_PATH = Paths.get("testOutput").resolve(CsvDataWriterTest.class.getSimpleName());
	private static final String LN_SEPARATOR = System.lineSeparator(),
								EXPECTED_HEADER = "Param1,Param2,,Param4" + LN_SEPARATOR,
								EXPECTED_TABLE_DATA = "Param1,Param2,,Param4" + LN_SEPARATOR + "Value1,,Value3,Value4" + LN_SEPARATOR + "\"\",,," + LN_SEPARATOR +
										"Value1,,Value3,Value4" + LN_SEPARATOR + "Val ue1,,Value3,Value4" + LN_SEPARATOR;

	@BeforeClass
	public void init() throws IOException
	{
		FileUtils.deleteDirectory(OUTPUT_PATH.toFile());
		Files.createDirectories(OUTPUT_PATH);
	}

	@Test (expectedExceptions = IllegalStateException.class)
	public void testWriteRowAndHeader() throws IOException
	{
		TableHeader<String> header = buildHeader();
		try (CsvDataWriter writer = new CsvDataWriter(header, new StringWriter(), true))
		{
			writer.write(buildRows(header));
			writer.writeHeader();
		}
	}

	@Test (expectedExceptions = IllegalStateException.class)
	public void testWriteTwoHeaders() throws IOException
	{
		TableHeader<String> header = buildHeader();
		try(CsvDataWriter writer = new CsvDataWriter(header, new StringWriter(), true))
		{
			writer.writeHeader();
			writer.writeHeader();
		}
	}

	@Test
	public void testWriteHeaderFile() throws IOException
	{
		File outputFile = OUTPUT_PATH.resolve("testWriteHeaderFile.csv").toFile();
		TableHeader<String> header = buildHeader();

		try(CsvDataWriter writer = new CsvDataWriter(header, outputFile, true, false))
		{
			writer.writeHeader();
		}
		assertEquals(FileUtils.readFileToString(outputFile, Utils.UTF8), EXPECTED_HEADER);
	}

	@Test
	public void testWriteHeader() throws IOException
	{
		StringWriter stringWriter = new StringWriter();
		TableHeader<String> header = buildHeader();

		try (CsvDataWriter writer = new CsvDataWriter(header, stringWriter, true))
		{
			writer.writeHeader();
		}
		assertEquals(stringWriter.toString(), EXPECTED_HEADER);
	}

	@Test
	public void testWrite() throws IOException
	{
		TableHeader<String> header = buildHeader();
		StringWriter stringWriter = new StringWriter();
		CsvDataWriter.write(buildDataForWriting(header, buildRows(header)), stringWriter, true);
		assertEquals(stringWriter.toString(), EXPECTED_TABLE_DATA);
	}

	@Test
	public void testWrite1() throws IOException
	{
		TableHeader<String> header = buildHeader();
		File outputFile = OUTPUT_PATH.resolve("testWrite1.csv").toFile();
		CsvDataWriter.write(buildDataForWriting(header, buildRows(header)), outputFile, true, false);
		assertEquals(FileUtils.readFileToString(outputFile, Utils.UTF8), EXPECTED_TABLE_DATA);
	}

	@Test
	public void testWriteRow() throws IOException
	{
		StringWriter stringWriter = new StringWriter();
		TableHeader<String> header = buildHeader();

		try (CsvDataWriter writer = new CsvDataWriter(header, stringWriter, true))
		{
			for (TableRow<String, String> row : buildRows(header))
			{
				writer.writeRow(row);
			}
		}
		assertEquals(stringWriter.toString(), EXPECTED_TABLE_DATA);
	}

	@Test
	public void testWriteRows() throws IOException
	{
		StringWriter stringWriter = new StringWriter();
		TableHeader<String> header = buildHeader();

		try (CsvDataWriter writer = new CsvDataWriter(header, stringWriter, true))
		{
			writer.writeRows(buildRows(header));
		}
		assertEquals(stringWriter.toString(), EXPECTED_TABLE_DATA);
	}

	@Test
	public void testWriteAndReadFile() throws IOException
	{
		File outputFile = OUTPUT_PATH.resolve("testWriteAndReadFile.csv").toFile();
		TableHeader<String> header = buildHeader();
		List<TableRow<String, String>> rows = buildExpectedRows(header);

		try(CsvDataWriter writer = new CsvDataWriter(header, outputFile, true, false))
		{
			writer.writeRows(buildRows(header));
		}
		try(CsvDataReader reader = new CsvDataReader(outputFile))
		{
			Assertions.assertThat(reader.readAllData()).usingRecursiveComparison()
					.isEqualTo(buildDataForWriting(header, rows));
		}
	}


	private TableHeader<String> buildHeader()
	{
		return new TableHeader<>(buildHeaderSet());
	}

	private StringTableData buildDataForWriting(TableHeader<String> header, List<TableRow<String, String>> rows)
	{
		StringTableData tableData = new StringTableData(header);
		for (TableRow<String, String> row : rows)
			tableData.add(row);
		return tableData;
	}

	private Set<String> buildHeaderSet()
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
		rowList.add(new TableRow<>(header, buildValues()));
		rowList.add(new TableRow<>(header, null));
		rowList.add(new TableRow<>(header, buildValues()));
		rowList.add(new TableRow<>(header, buildGapValues()));
		return rowList;
	}

	private List<TableRow<String, String>> buildExpectedRows(TableHeader<String> header)
	{
		List<TableRow<String, String>> rowList = new ArrayList<>();
		rowList.add(new TableRow<>(header, buildValues()));
		rowList.add(new TableRow<>(header, buildEmptyValues()));
		rowList.add(new TableRow<>(header, buildValues()));
		rowList.add(new TableRow<>(header, buildExpectedValues()));
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

	private List<String> buildExpectedValues()
	{
		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("Val ue1");
		expectedValues.add("");
		expectedValues.add("Value3");
		expectedValues.add("Value4");
		return expectedValues;
	}
}