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

import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static org.assertj.core.api.Assertions.assertThat;

public class CsvDataReaderTest
{
	private static final String TEST_CSV_FILE = "TableDataTest/testCsv.csv";
	private static final String TEST_WITH_DOTS_CSV_FLE = "TableDataTest/testDotsCsv.csv";

	@Test(dataProvider = "pathsAndDelimiters")
	public void testReadAllDataWithFile(Path filePath, char delimiter) throws IOException
	{
		StringTableData expectedTableData = getExpectedTableData();
		try (CsvDataReader csvDataReader =
				     new CsvDataReader(filePath.toFile()))
		{
			csvDataReader.setDelimiter(delimiter);
			StringTableData actualTableData = csvDataReader.readAllData();
			assertThat(actualTableData)
					.usingRecursiveComparison()
					.isEqualTo(expectedTableData);
		}
	}

	@Test(dataProvider = "pathsAndDelimiters")
	public void testReadAllDataWithReader(Path filePath, char delimiter) throws IOException
	{
		try (CsvDataReader csvDataReader =
				     new CsvDataReader(new FileReader(filePath.toFile())))
		{
			csvDataReader.setDelimiter(delimiter);
			assertThat(csvDataReader.readAllData())
					.usingRecursiveComparison()
					.isEqualTo(getExpectedTableData());
		}
	}

	@Test
	public void testReadWithFile() throws IOException
	{
		assertThat(CsvDataReader.read(Paths.get(resourceToAbsoluteFilePath(TEST_CSV_FILE)).toFile()))
				.usingRecursiveComparison()
				.isEqualTo(getExpectedTableData());
	}

	@Test
	public void testReadWithReader() throws IOException
	{
		try (FileReader csvDataReader = new FileReader(Paths.get(resourceToAbsoluteFilePath(TEST_CSV_FILE)).toFile()))
		{
			assertThat(CsvDataReader.read(csvDataReader))
					.usingRecursiveComparison()
					.isEqualTo(getExpectedTableData());
		}
	}

	@DataProvider(name = "pathsAndDelimiters")
	Object[][] getPaths() throws FileNotFoundException
	{
		return new Object[][]
				{
						{
								Paths.get(resourceToAbsoluteFilePath(TEST_CSV_FILE)), ','
						},
						{
								Paths.get(resourceToAbsoluteFilePath(TEST_WITH_DOTS_CSV_FLE)), ';'
						}
				};
	}

	public StringTableData getExpectedTableData()
	{
		TableHeader<String> header = buildHeader();
		StringTableData tableData = new StringTableData(header);
		tableData.add(new TableRow<String, String>(header, buildValues()));
		tableData.add(new TableRow<String, String>(header, buildEmptyValues()));
		tableData.add(new TableRow<String, String>(header, buildValues()));
		tableData.add(new TableRow<String, String>(header, buildGapValues()));
		return tableData;
	}

	private TableHeader<String> buildHeader()
	{
		return new TableHeader<>(buildHeaderSet());
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

	private List<String> buildValues()
	{
		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("Value1");
		expectedValues.add("");
		expectedValues.add("Value3");
		expectedValues.add("Value4");
		return expectedValues;
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

	private List<String> buildEmptyValues()
	{
		List<String> emptyValues = new ArrayList<>();
		emptyValues.add("");
		emptyValues.add("");
		emptyValues.add("");
		emptyValues.add("");
		return emptyValues;
	}
}