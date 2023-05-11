/*******************************************************************************
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

package com.exactprosystems.clearth.utils.tabledata.rowMatchers;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.primarykeys.PrimaryKey;
import org.testng.annotations.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static org.testng.Assert.*;

public class StringTableRowMatcherTest
{
	private Set<String> buildLinkedHashSet()
	{
		Set<String> keys = new LinkedHashSet<>();
		keys.add("Key3");
		keys.add("duplicated");
		keys.add("Key2");
		keys.add("some very very long key");
		keys.add("1");
		keys.add("duplicated");
		keys.add("Key1");
		
		return keys;
	}

	@Test
	public void testOrder()
	{
		Set<String> keys = buildLinkedHashSet();
		StringTableRowMatcher matcher = new StringTableRowMatcher(keys);
		Iterator<String> expected = keys.iterator();
		Iterator<String> actual = matcher.keyColumns.iterator();
		
		while (expected.hasNext())
			assertEquals(actual.next(), expected.next());
		
		assertFalse(actual.hasNext());
	}
	
	@Test
	public void testEmpty() throws ParametersException
	{
		StringTableRowMatcher matcherABCD = new StringTableRowMatcher(new HashSet<>(Arrays.asList("A", "B", "C", "D")));
		
		TableHeader<String> headerAB = new TableHeader<>(new HashSet<>(Arrays.asList("A", "B")));
		TableHeader<String> headerABCD = new TableHeader<>(new HashSet<>(Arrays.asList("A", "B", "C", "D")));

		TableRow<String, String> firstIsNullAB = new TableRow<>(headerAB, asList(null, ""));
		
		TableRow<String, String> secondIsNullABCD = new TableRow<>(headerABCD, asList("", null, "", ""));
		TableRow<String, String> secondIsPseudoNullABCD = new TableRow<>(headerABCD, asList("", "null", "", ""));
		TableRow<String, String> secondIsNullABCDDuplicate = new TableRow<>(headerABCD, asList("", null, "", ""));
		TableRow<String, String> thirdIsNullABCD = new TableRow<>(headerABCD, asList("", "", null, ""));
		TableRow<String, String> testRowWithString = new TableRow<>(headerABCD, asList("val", "null", null, ""));

		assertThrows(ParametersException.class, () -> matcherABCD.checkHeader(headerAB));
		assertThrows(IllegalArgumentException.class, () -> matcherABCD.createPrimaryKey(firstIsNullAB));
		
		matcherABCD.checkHeader(headerABCD); // assert does not throw
		
		assertNotEquals(matcherABCD.createPrimaryKey(secondIsNullABCD), matcherABCD.createPrimaryKey(secondIsPseudoNullABCD));
		assertEquals(matcherABCD.createPrimaryKey(secondIsNullABCD), matcherABCD.createPrimaryKey(secondIsNullABCDDuplicate));
		assertNotEquals(matcherABCD.createPrimaryKey(secondIsNullABCD), matcherABCD.createPrimaryKey(thirdIsNullABCD));
		
		PrimaryKey rowPrimaryKey = matcherABCD.createPrimaryKey(testRowWithString);
		String expectedStringValue = "\"val\",\"null\",null,\"\"";
		
		assertEquals(rowPrimaryKey.toString(), expectedStringValue);
	}

	@Test
	public void testMatchTableRowAndCollection()
	{
		StringTableRowMatcher matcher = new StringTableRowMatcher(buildHeaderSet());
		PrimaryKey keyCollection = matcher.createPrimaryKey(buildCollection());
		PrimaryKey keyTableRow = matcher.createPrimaryKey(buildTableRow());

		assertEquals(keyCollection, keyTableRow);
	}

	private Collection<String> buildCollection()
	{
		return Arrays.asList("A", "B", "C", "D");
	}
	private Set<String> buildHeaderSet()
	{
		Set<String> header = new LinkedHashSet<>();
		header.add("Param1");
		header.add("Param2");
		header.add("Param3");
		header.add("Param4");

		return header;
	}
	private TableRow<String, String> buildTableRow()
	{
		Collection<String> collection = buildCollection();
		TableHeader<String> th = new TableHeader<>(buildHeaderSet());
		return new TableRow<>(th, collection);
	}
}
