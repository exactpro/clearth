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

package com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators;

import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.DataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.StringDataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.FieldDesc;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.descs.MappingDesc;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonData;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.MappedTableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.StringValuesComparator;
import org.testng.annotations.Test;

import java.util.Arrays;

import static com.exactprosystems.clearth.utils.CollectionUtils.setOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MappedTableRowsComparatorTest
{
	private static String IGNORE_COLUMN = "A";
	private TableHeader<String> header = new TableHeader<>(setOf(IGNORE_COLUMN));
	@Test
	private void testIgnore()
	{
		DataMapping<String> dataMapping = createDataMapping();
		MappedTableRowsComparator<String, String> comparator = new MappedTableRowsComparator<>(
				new StringValuesComparator(new ComparisonUtils()), dataMapping);
		TableRow<String, String> expectedRow = new TableRow<>(header, setOf("1"));
		TableRow<String, String> actualRow = new TableRow<>(header, setOf("2"));
		
		RowComparisonData<String, String> comparisonData = comparator.compareRows(expectedRow, actualRow, header);
		assertTrue(comparisonData.isSuccess());
		assertEquals(comparisonData.getCompDetails().size(), 0);
	}

	private DataMapping<String> createDataMapping()
	{
		FieldDesc fieldDesc = new FieldDesc();
		fieldDesc.setLocalName(IGNORE_COLUMN);
		fieldDesc.setIgnore(true);
		
		MappingDesc mappingDesc = new MappingDesc();
		mappingDesc.setFields(Arrays.asList(fieldDesc));
		return new StringDataMapping(mappingDesc);
	}
}