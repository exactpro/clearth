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
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

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
			assertThat(expected.next()).isEqualTo(actual.next());
		
		assertThat(actual.hasNext()).isFalse();
	}
	
	@Test
	public void testNull()
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

		assertThatExceptionOfType(ParametersException.class).isThrownBy(() -> matcherABCD.checkHeader(headerAB));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> matcherABCD.createPrimaryKey(firstIsNullAB));
		assertThatCode(() -> matcherABCD.checkHeader(headerABCD)).doesNotThrowAnyException();
		
		assertThat(matcherABCD.createPrimaryKey(secondIsNullABCD)).isNotEqualTo(matcherABCD.createPrimaryKey(secondIsPseudoNullABCD));
		assertThat(matcherABCD.createPrimaryKey(secondIsNullABCD)).isEqualTo(matcherABCD.createPrimaryKey(secondIsNullABCDDuplicate));
		assertThat(matcherABCD.createPrimaryKey(secondIsNullABCD)).isNotEqualTo(matcherABCD.createPrimaryKey(thirdIsNullABCD));
		assertThat(matcherABCD.createPrimaryKey(testRowWithString)).isEqualTo("\"val\",\"null\",null,\"\"");
	}

	@Test
	public void testMatchTableRowAndCollection()
	{
		StringTableRowMatcher matcher = new StringTableRowMatcher(buildHeaderSet());
		String keyCollection = matcher.createPrimaryKey(buildCollection());
		String keyTableRow = matcher.createPrimaryKey(buildTableRow());

		Assert.assertEquals(keyCollection, keyTableRow);
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
