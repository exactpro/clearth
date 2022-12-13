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

import com.exactprosystems.clearth.utils.BigDecimalValueTransformer;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class NumericStringTableRowMatcherTest
{
	public static final String NUMERIC_KEY_COLUMN = "NumericKeyColumn";
	private Set<String> keys;
	private Map<String, BigDecimal> numericColumns;
	private Set<String> columns;
	NumericStringTableRowMatcher matcher;

	private Set<String> createKeys()
	{
		Set<String> keys = new LinkedHashSet<>();
		keys.add("KeyColumn");
		keys.add(NUMERIC_KEY_COLUMN);

		return keys;
	}

	private Map<String, BigDecimal> createNumericColumns()
	{
		Map<String, BigDecimal> result = new LinkedHashMap<>();
		result.put(NUMERIC_KEY_COLUMN, BigDecimal.ONE);
		result.put("NumericColumnNotPresentedInKey", BigDecimal.ONE);
		return result;
	}
	
	private Set<String> createHeaderColumns(Set<String> keys)
	{
		Set<String> result = new LinkedHashSet<>(keys);
		result.add("NotImportantColumn");
		return result;
	}

	private IValueTransformer createValueTransformer()
	{
		return new BigDecimalValueTransformer();
	}

	@BeforeClass
	public void initialize()
	{
		keys = createKeys();
		numericColumns = createNumericColumns();
		columns = createHeaderColumns(keys);
		matcher = new NumericStringTableRowMatcher(keys, numericColumns, createValueTransformer());
	}


	@Test
	public void testValues()
	{
		TableHeader<String> header = new TableHeader<>(columns);
		Set<String> expectedValues = new LinkedHashSet<>();
		Set<String> actualValues = new LinkedHashSet<>();
		header.forEach((e) -> {
			if (numericColumns.containsKey(e))
			{
				expectedValues.add("1");
				actualValues.add("1.001");
			}
			else
			{
				expectedValues.add(e + "_value");
				actualValues.add(e + "_value");
			}
		});

		TableRow<String, String> expected = new TableRow<>(header, expectedValues);
		TableRow<String, String> actual = new TableRow<>(header, actualValues);

		assertThat(matcher.createPrimaryKey(expected)).isEqualTo(matcher.createPrimaryKey(actual));
		assertThat(matcher.matchBySecondaryKey(expected, actual)).isTrue();

		actual.setValue(NUMERIC_KEY_COLUMN, "5");
		assertThat(matcher.createPrimaryKey(expected)).isEqualTo(matcher.createPrimaryKey(actual));
		assertThat(matcher.matchBySecondaryKey(expected, actual)).isFalse();
	}
}