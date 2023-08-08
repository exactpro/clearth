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

package com.exactprosystems.clearth.utils.tabledata.typing.reader;

import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableData;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeader;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeaderItem;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.exactprosystems.clearth.utils.tabledata.typing.TypedCsvTableDataUtils.*;

public class TypedCsvDataReaderTest
{
	private Path resDir;

	@BeforeClass
	public void init() throws IOException
	{
		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(TypedCsvDataReaderTest.class.getSimpleName()));
	}

	@Test (expectedExceptions = IOException.class,
			expectedExceptionsMessageRegExp = "Could not read CSV header")
	public void testReadEmptyFile() throws IOException
	{
		StringReader stringReader = new StringReader("");
		try (TypedCsvDataReader reader = new TypedCsvDataReader(stringReader))
		{
			reader.readAllData();
		}
	}

	@Test
	public void testReadFile() throws IOException
	{
		File file = resDir.resolve("testReadCsvFile.csv").toFile();
		TypedTableHeader header = createHeader();
		List<TableRow<TypedTableHeaderItem, Object>> rows = createRows(header);
		TypedTableData expectedTableData = createTypedTableData(header, rows);

		try (TypedCsvDataReader reader = new TypedCsvDataReader(file))
		{
			assertTypedTableData(reader.readAllData(), expectedTableData);
		}
	}

	@Test
	public void tesReadHeaderFromFile() throws IOException
	{
		File file = resDir.resolve("testReadCsvFile.csv").toFile();
		try (TypedCsvDataReader reader = new TypedCsvDataReader(file))
		{
			assertHeaders(reader.readHeader(), createHeader().toSet());
		}
	}
}