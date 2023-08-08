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

package com.exactprosystems.clearth.utils.tabledata.typing.writer;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableData;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeader;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeaderItem;
import com.exactprosystems.clearth.utils.tabledata.typing.reader.TypedCsvDataReader;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.exactprosystems.clearth.utils.tabledata.typing.TypedCsvTableDataUtils.*;
import static org.testng.Assert.assertEquals;

public class TypedCsvDataWriterTest
{
	private static final Path OUTPUT_PATH = Paths.get("testOutput").resolve(TypedCsvDataWriterTest.class.getSimpleName());
	
	@BeforeClass
	public static void init() throws IOException
	{
		FileUtils.deleteDirectory(OUTPUT_PATH.toFile());
		Files.createDirectories(OUTPUT_PATH);
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteRowAndHeaderToFile() throws IOException
	{
		TypedTableHeader header = createHeader();
		try (TypedCsvDataWriter writer = new TypedCsvDataWriter(header, new StringWriter(), true))
		{
			writer.write(createRow(header, " ", " ", " "));
			writer.writeHeader();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteTwoHeadersToFile() throws IOException
	{
		TypedTableHeader header = createHeader();
		try(TypedCsvDataWriter writer = new TypedCsvDataWriter(header, new StringWriter(), true))
		{
			writer.writeHeader();
			writer.writeHeader();
		}
	}

	@Test
	public void testWriteHeaderFile() throws IOException
	{
		File outputFile = OUTPUT_PATH.resolve("testWriteHeaderFile.csv").toFile();
		TypedTableHeader header = createHeader();

		try(TypedCsvDataWriter writer = new TypedCsvDataWriter(header, outputFile, true, false))
		{
			writer.writeHeader();
		}
		assertEquals(FileUtils.readFileToString(outputFile, Utils.UTF8), EXPECTED_HEADER);
	}

	@Test
	public void testWriteHeaderWriter() throws IOException
	{
		StringWriter stringWriter = new StringWriter();
		TypedTableHeader header = createHeader();
		try(TypedCsvDataWriter writer = new TypedCsvDataWriter(header, stringWriter, true))
		{
			writer.writeHeader();
		}
		assertEquals(stringWriter.toString(), EXPECTED_HEADER);
	}

	@Test
	public void testWriteAndReadFile() throws IOException
	{
		File outputFile = OUTPUT_PATH.resolve("testWriteAndReadFile.csv").toFile();
		TypedTableHeader header = createHeader();
		List<TableRow<TypedTableHeaderItem, Object>> rows = createRows(header);

		try (TypedCsvDataWriter writer = new TypedCsvDataWriter(header, outputFile, true, false))
		{
			writer.writeRows(rows);
		}

		TypedTableData expectedTableData = createTypedTableData(header, rows);

		try (TypedCsvDataReader reader = new TypedCsvDataReader(outputFile))
		{
			assertTypedTableData(reader.readAllData(), expectedTableData);
		}
	}

	@Test
	public void testWriteRow() throws IOException
	{
		StringWriter stringWriter = new StringWriter();
		TypedTableHeader header = createHeader();

		try (TypedCsvDataWriter writer = new TypedCsvDataWriter(header, stringWriter, true))
		{
			writer.writeRow(createRow(header, "1", "2", "3"));
		}
		assertEquals(stringWriter.toString(), EXPECTED_ROW);
	}

	@Test
	public void testWriteRows() throws IOException
	{
		StringWriter stringWriter = new StringWriter();
		TypedTableHeader header = createHeader();

		try (TypedCsvDataWriter writer = new TypedCsvDataWriter(header, stringWriter, true))
		{
			writer.writeRows(createRows(header));
		}
		assertEquals(stringWriter.toString(), EXPECTED_ROWS);
	}
}