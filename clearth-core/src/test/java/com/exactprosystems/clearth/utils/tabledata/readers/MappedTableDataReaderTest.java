/*******************************************************************************
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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.CollectionUtils.setOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class MappedTableDataReaderTest
{
	private static final String OLD_COL = "oldName", NEW_COL = "newName",
			FIRST_COL = "unchangedName1", THIRD_COL = "unchangedName2";
	private static final Path RESOURCE_DIR = Paths.get("src", "test", "resources")
			.resolve(MappedTableDataReaderTest.class.getSimpleName());
	private static final Map<String, String> conversionMap = map(OLD_COL, NEW_COL);
	private static final HeaderMapper<String> headerConverter = new HeaderMapper<String>(conversionMap);
	
	private TableHeader<String> createHeader(String... columns)
	{
		return new TableHeader<>(setOf(columns));
	}
	
	@Test
	private void test() throws IOException
	{
		File file = RESOURCE_DIR.resolve("tableData.csv").toFile();
		CsvDataReader aggregatedReader = null;
		MappedTableDataReader<String, String, StringTableData> mappedReader = null;
		try
		{
			aggregatedReader = new CsvDataReader(file);
			mappedReader = new MappedTableDataReader<>(aggregatedReader, headerConverter);
			mappedReader.readAllData();
			StringTableData mappedTableData = mappedReader.getTableData();
					
			assertEquals(aggregatedReader.getTableData().getHeader(),
					createHeader(FIRST_COL, OLD_COL, THIRD_COL));
			assertEquals(mappedTableData.getHeader(),
					createHeader(FIRST_COL, NEW_COL, THIRD_COL));
			
			assertEquals(aggregatedReader.getTableData().size(), 0, "Aggregated reader's data should remain empty");
			assertEquals(mappedTableData.size(), 2);
			
			assertEquals(mappedTableData.getRow(0).getValues(), Arrays.asList("1:1", "1:2", "1:3"));
			
			TableRow<String, String> secondRow = mappedTableData.getRow(1);
			assertNull(secondRow.getValue(OLD_COL));
			assertEquals(secondRow.getValue(FIRST_COL), "2:1");
			assertEquals(secondRow.getValue(NEW_COL), "2:2");
			assertEquals(secondRow.getValue(THIRD_COL), "2:3");
		}
		finally
		{
			Utils.closeResource(mappedReader);
			Utils.closeResource(aggregatedReader);
		}
	}
}