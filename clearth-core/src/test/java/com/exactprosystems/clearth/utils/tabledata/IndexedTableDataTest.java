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

package com.exactprosystems.clearth.utils.tabledata;

import com.exactprosystems.clearth.utils.tabledata.rowMatchers.StringTableRowMatcher;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Arrays;

public class IndexedTableDataTest
{
	@Test (expected = IllegalStateException.class)
	public void testFindNewCollection()
	{
		Set<String> header = buildHeaderSet();
		TableHeader<String> th = new TableHeader<>(header);
		TableRow<String, String> tr = new TableRow<>(th, buildCollectionValues());

		IndexedTableData<String, String, String> strIndexedTableData = new IndexedTableData<>(th, new StringTableRowMatcher(header));
		strIndexedTableData.add(tr);

		strIndexedTableData.findAll(buildNewCollection());
	}

	@Test
	public void testFindAllValuesWithCollection()
	{
		Collection<String> collectionValues = buildCollectionValues();
		List<TableRow<String, String>> tableRows = buildIndexedTableData().findAll(collectionValues);
		Assert.assertNotNull(tableRows);
	}

	@Test
	public void testFindAllValuesWithCollections()
	{
		IndexedTableData<String, String, String> tableData = buildIndexedTableData();

		Assert.assertEquals(1, tableData.findAll(buildCollectionValues()).size());
		Assert.assertEquals(1, tableData.findAll(Arrays.asList("Two1", "Two2", "Two3")).size());
		Assert.assertEquals(2, tableData.findAll(Arrays.asList("Three1", "Three2", "Three3")).size());
	}

	@Test
	public void testFindTableRow()
	{
		Collection<String> values = buildCollectionValues();
		TableHeader<String> th = new TableHeader<>(buildHeaderSet());
		TableRow<String, String> tr = new TableRow<>(th, values);
		TableRow<String, String> tableRow = buildIndexedTableData().find(tr);
		Assert.assertNotNull(tableRow);
	}

	private Collection<String> buildCollectionValues()
	{
		Collection<String> collValues = Arrays.asList("One1", "One2", "One3");
		return collValues;
	}

	private Collection<String> buildNewCollection()
	{
		Collection<String> collection = Arrays.asList("One1", "One2", "One3", "One4");
		return collection;
	}

	private Set<String> buildHeaderSet()
	{
		Set<String> header = new LinkedHashSet<>();
		header.add("Param1");
		header.add("Param2");
		header.add("Param3");

		return header;
	}

	private IndexedTableData<String, String, String> buildIndexedTableData()
	{
		Collection<String> collValues1 = buildCollectionValues();
		Collection<String> collValues2 = Arrays.asList("Two1", "Two2", "Two3");
		Collection<String> collValues3 = Arrays.asList("Three1", "Three2", "Three3");

		Set<String> header = buildHeaderSet();
		TableHeader<String> th = new TableHeader<>(header);

		TableRowMatcher<String, String, String> matcher =  new StringTableRowMatcher(header);
		TableRow<String, String> tr1 = new TableRow<>(th, collValues1);
		TableRow<String, String> tr2 = new TableRow<>(th, collValues2);
		TableRow<String, String> tr3 = new TableRow<>(th, collValues3);

		IndexedTableData<String, String, String> stringIndexedTableData = new IndexedTableData<>(th, matcher);
		stringIndexedTableData.add(tr1);
		stringIndexedTableData.add(tr2);
		stringIndexedTableData.add(tr3);
		stringIndexedTableData.add(tr3);

		return stringIndexedTableData;
	}
}