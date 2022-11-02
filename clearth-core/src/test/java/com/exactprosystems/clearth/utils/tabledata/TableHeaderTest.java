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

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class TableHeaderTest {
	@Test
	public void toCollection() throws ResultException {
		Set<String> set = new HashSet<>();
		set.add("A");
		set.add("B");
		set.add("C");

		TableHeader th = new TableHeader<>(set);
		List actual = new ArrayList<>(th.toCollection());
		List expected = new ArrayList<>(set);
		Assert.assertEquals("Result of toCollection()", expected, actual);
	}
}