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

import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.CollectionUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.UnsupportedOperationException;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MemoryTableDataReaderTest
{
	@DataProvider(name = "rowCollectionStringExamples")
	Object[][] createRowLists()
	{
		TableHeader<String> listHeader = createHeader("Header1", "Header2");
		return new Object[][]
			{
				{listHeader, createTableDataList(listHeader, Arrays.asList("Value1", "Value2", "Value3", "Value4")), false},
				{listHeader, createTableDataList(listHeader, Arrays.asList("Value1", "Value1", "Value1", "Value1")), false},
				{listHeader, createTableDataList(listHeader, Arrays.asList("Value1", "Value2", "Value3", "Value4")), true},
				{listHeader, createTableDataList(listHeader, Arrays.asList("Value1", "Value1", "Value1", "Value1")), true}
			};
	}

	private TableHeader<String> createHeader(String... columns)
	{
		return new TableHeader<>(CollectionUtils.setOf(columns));
	}

	private TableData<String, String> createTableData(TableHeader<String> header, Collection<TableRow<String, String>> rows)
	{
		TableData<String, String> data = new TableData<>(header);
		for (TableRow<String, String> row : rows)
			data.add(row);
		return data;
	}

	private List<TableRow<String, String>> createTableDataList(TableHeader<String> header, List<String> columns)
	{
		List<TableRow<String, String>> result = new ArrayList<>();
		for (int i = 0; i < columns.size(); i += header.size())
		{
			TableRow<String, String> values = new TableRow<>(header);
			for (int j = 0; j < header.size(); ++j)
				values.setValue(j, columns.get(i + j));
			result.add(values);
		}
		return result;
	}

	@Test(dataProvider = "rowCollectionStringExamples")
	private void checkTableDataReading(TableHeader<String> header, Collection<TableRow<String, String>> data, boolean setCopy) throws IOException
	{
		TableData<String, String> tableData = createTableData(header, data);
		try (MemoryTableDataReader<String, String> reader = new MemoryTableDataReader<>(tableData)) 
		{
			verifyTableDataReaderData(reader, tableData, setCopy);
		}
	}

	@Test(dataProvider = "rowCollectionStringExamples")
	private void checkRowCollectionReading(TableHeader<String> header, Collection<TableRow<String, String>> data, boolean setCopy) throws IOException
	{
		TableData<String, String> expectedData = createTableData(header, data);
		try (MemoryTableDataReader<String, String> reader = new MemoryTableDataReader<>(data)) 
		{
			verifyTableDataReaderData(reader, expectedData, setCopy);
		}
	}

	private void verifyTableDataReaderData(MemoryTableDataReader<String, String> reader, TableData<String, String> expectedData, boolean setCopy) throws IOException
	{
		reader.setCopyRow(setCopy);
		TableData<String, String> actualData = reader.readAllData();
		Assert.assertTrue(actualData.getHeader() == expectedData.getHeader());
		int actualSize = actualData.size();
		Assert.assertEquals(actualSize, expectedData.size());
		for (int i = 0; i < actualSize; ++i)
		{
			Assert.assertEquals(actualData.getRow(i), expectedData.getRow(i));
		}
	}

	@Test(expectedExceptions = IOException.class)
	private void checkEmptyListReading() throws IOException
	{
		readRowCollection(Collections.emptyList());
	}

	@Test
	private void checkEmptyTableReading() throws IOException
	{
		TableData<String, String> data = new TableData<>(createHeader("Header"));
		try (MemoryTableDataReader<String, String> reader = new MemoryTableDataReader<>(data)) 
		{
			reader.readAllData();
		}
	}

	@Test(expectedExceptions = IOException.class)
	private void checkMismatchedHeaderListReading() throws IOException
	{
		Collection<TableRow<String, String>> data = new ArrayList<>();
		data.add(new TableRow<>(createHeader("Header1", "Header2"), Arrays.asList("a", "b")));
		data.add(new TableRow<>(createHeader("Header2", "Header3"), Arrays.asList("c", "d")));
		readRowCollection(data);
	}

	@Test(expectedExceptions = UnsupportedOperationException.class)
	private void checkModifyUncopiedRows() throws IOException
	{
		TableHeader<String> header = createHeader("Header");
		TableData<String, String> data = createTableData(header, createTableDataList(header, Collections.singletonList("a")));
		try (MemoryTableDataReader<String, String> reader = new MemoryTableDataReader<>(data)) 
		{
			reader.setCopyRow(false);
			reader.start();
			TableRow<String, String> row = reader.readRow();
			row.setValue("Header", "c");
		}
	}

	private void readRowCollection(Collection<TableRow<String, String>> data) throws IOException
	{
		try (MemoryTableDataReader<String, String> reader = new MemoryTableDataReader<>(data)) 
		{
			reader.readAllData();
		}
	}
}