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
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeader;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeaderItem;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableRow;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

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
import static com.exactprosystems.clearth.utils.tabledata.typing.TableDataType.*;
import static org.testng.Assert.assertEquals;

public class TypedCsvDataWriterTest
{
	private static final Path OUTPUT_PATH = Paths.get("testOutput"),
			EXPECTED_FILES_PATH = Paths.get("TableDataTest");
	private File outputFile = OUTPUT_PATH.resolve("outputFileTypedCsv.csv").toFile(),
			expectedFile = EXPECTED_FILES_PATH.resolve("expectedTypedFile.csv").toFile();
	private static TypedTableRow row;
	private static TypedTableHeader typedHeader;
	
	@BeforeClass
	public static void init() throws IOException
	{
		createDir(OUTPUT_PATH);
		
		Set<TypedTableHeaderItem> headerSetItems = new LinkedHashSet<>();
		headerSetItems.add(new TypedTableHeaderItem("name1", STRING));
		headerSetItems.add(new TypedTableHeaderItem("name2", INTEGER));
		headerSetItems.add(new TypedTableHeaderItem("name3", BOOLEAN));

		List<Object> listValues = new ArrayList<>();
		listValues.add("1");
		listValues.add("2");
		listValues.add("3");

		typedHeader = new TypedTableHeader(headerSetItems);
		row = new TypedTableRow(typedHeader, listValues);
	}

	private static void createDir(Path dir) throws IOException
	{
		if (!dir.toFile().exists())
			Files.createDirectories(dir);
	}

	private void forTestsWriteHeader() throws IOException
	{
		try
		{
			String actualTableData = FileUtils.readFileToString(outputFile, Utils.UTF8);
			String expectedTableData = FileUtils.readFileToString(new File(resourceToAbsoluteFilePath(expectedFile.getPath())), Utils.UTF8);
			assertEquals(actualTableData, expectedTableData);
		}
		finally
		{
			outputFile.delete();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteRowAndHeaderToFile() throws IOException
	{
		try (TypedCsvDataWriter writer = new TypedCsvDataWriter(typedHeader, outputFile, true, true))
		{
			writer.write(row);
			writer.writeHeader();
		}
		finally
		{
			outputFile.delete();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testWriteTwoHeadersToFile() throws IOException
	{
		try(TypedCsvDataWriter writer = new TypedCsvDataWriter(typedHeader, new FileWriter(outputFile), true))
		{
			writer.writeHeader();
			writer.writeHeader();
		}
		finally
		{
			outputFile.delete();
		}
	}

	@Test
	public void testWriteHeaderFile() throws IOException
	{
		try(TypedCsvDataWriter writer = new TypedCsvDataWriter(typedHeader, outputFile, true, true))
		{
			writer.writeHeader();
		}
		forTestsWriteHeader();
	}

	@Test
	public void testWriteHeaderWriter() throws IOException
	{
		try(TypedCsvDataWriter writer = new TypedCsvDataWriter(typedHeader, new FileWriter(outputFile), true))
		{
			writer.writeHeader();
		}
		forTestsWriteHeader();
	}
}