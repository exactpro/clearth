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

package com.exactprosystems.clearth.utils.tabledata.comparison.rowsCollectors;

import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.StringTableRowMatcher;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static com.exactprosystems.clearth.utils.CollectionUtils.setOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class StringKeyColumnsRowsCollectorTest
{
	private static final Path TEST_DIR = Paths.get("testOutput", StringKeyColumnsRowsCollectorTest.class.getSimpleName());
	private static final File CACHE_FILE = TEST_DIR.resolve("cache.tmp").toFile();
	private static final String FIRST_ROW = "First row", SECOND_ROW = "Second row";

	protected StringKeyColumnsRowsCollector createRowsCollector(Set<String> keyColumns)
			throws IOException
	{
		return new StringKeyColumnsRowsCollector(keyColumns, 1, CACHE_FILE);
	}
	
	@BeforeClass
	protected void beforeClass() throws IOException
	{
		FileUtils.deleteQuietly(TEST_DIR.toFile());
		Files.createDirectories(TEST_DIR);
	}
	
	@Test
	public void testSerialization() throws IOException
	{
		Set<String> keyColumnsAB = setOf("A", "B");
		StringTableRowMatcher rowMatcher =
				new StringTableRowMatcher(keyColumnsAB);

		try (StringKeyColumnsRowsCollector collector = createRowsCollector(keyColumnsAB))
		{
			TableHeader<String> header = new TableHeader<>(setOf("A", "B", "C"));
			Set<String> row1Values = setOf("A1", "B1", "C1");
			Set<String> row2Values = setOf("A2", "B2", "C2");
			
			TableRow<String, String> row1 = new TableRow<>(header, row1Values);
			TableRow<String, String> row2 = new TableRow<>(header, row2Values);

			collector.tableRowToString(row1, FIRST_ROW);
			assertNull(collector.checkForDuplicatedRow(row1, rowMatcher.createPrimaryKey(row1),
					rowMatcher::matchBySecondaryKey));

			collector.addRow(row1, rowMatcher.createPrimaryKey(row1), FIRST_ROW);
			// this check will be working with cached row
			assertEquals(collector.checkForDuplicatedRow(row1, rowMatcher.createPrimaryKey(row1), rowMatcher::matchBySecondaryKey),
					FIRST_ROW);
			
			// due to max cache size = 1 this will fill cache hence collector will have to search for next check for 
			// duplicates in tmp file
			collector.addRow(row2, rowMatcher.createPrimaryKey(row2), SECOND_ROW);
			collector.addRow(row2, rowMatcher.createPrimaryKey(row2), SECOND_ROW);
			// if this fails then problem is in caching to file
			assertEquals(collector.checkForDuplicatedRow(row1, rowMatcher.createPrimaryKey(row1),
					rowMatcher::matchBySecondaryKey), FIRST_ROW, "Problem with caching rows to file");
		}
	}
}