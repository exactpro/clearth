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
package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.tabledata.IndexedStringTableData;
import com.exactprosystems.clearth.utils.tabledata.IndexedTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.DefaultStringTableRowMatcher;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static org.assertj.core.api.Assertions.assertThat;

public class IndexedCsvDataReaderTest
{
	private static final String TEST_CSV_FILE = "TableDataTest/testCsv.csv";

	@Test (dataProvider = "matchers")
	public void testReadAllData(TableRowMatcher<String, String, String> matcher) throws IOException
	{
		try(IndexedCsvDataReader<String> indexedCsvDataReader = new IndexedCsvDataReader<String>(
				Paths.get(resourceToAbsoluteFilePath(TEST_CSV_FILE)).toFile(), matcher))
		{
			assertThat(indexedCsvDataReader.readAllData())
					.usingRecursiveComparison()
					.isEqualTo(getExpectedTableData(matcher));
		}
	}

	@DataProvider (name = "matchers")
	Object[][] getMatchers()
	{
		return new Object[][]
				{
						{
								new DefaultStringTableRowMatcher(createKeys())
						},
						{
								new DefaultStringTableRowMatcher(createKey())
						}
				};
	}

	private IndexedTableData<String, String, String> getExpectedTableData(TableRowMatcher<String, String, String> matcher)
	{
		TableHeader<String> header = buildHeader();
		List<String> expectedValues = buildValues();
		IndexedTableData<String, String, String> expectedTableData = new IndexedStringTableData(header, matcher);

		expectedTableData.add(new TableRow<String, String>(header,expectedValues));
		expectedTableData.add(new TableRow<String, String>(header,buildEmptyValues()));
		expectedTableData.add(new TableRow<String, String>(header,expectedValues));
		expectedTableData.add(new TableRow<String, String>(header,buildGapValues()));
		return expectedTableData;
	}

	private Set<String> createKeys()
	{
		Set<String> keyColumns = new HashSet<>();
		keyColumns.add("Param1");
		keyColumns.add("Param2");
		keyColumns.add("");
		return keyColumns;
	}

	private Set<String> createKey()
	{
		Set<String> keyColumns = new HashSet<>();
		keyColumns.add("Param1");
		return keyColumns;
	}

	public static TableHeader<String> buildHeader()
	{
		return new TableHeader<>(buildHeaderSet());
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

	private List<String> buildGapValues()
	{
		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("Val ue1");
		expectedValues.add("");
		expectedValues.add("Value3");
		expectedValues.add("Value4");
		return  expectedValues;
	}
}