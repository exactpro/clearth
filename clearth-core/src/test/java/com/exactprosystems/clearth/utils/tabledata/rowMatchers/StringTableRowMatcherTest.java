/*******************************************************************************
 * Copyright 2009-2021 Exactpro Systems Limited
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

import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
}
