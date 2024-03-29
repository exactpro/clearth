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

package com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.tabledata.IndexedStringTableData;
import com.exactprosystems.clearth.utils.tabledata.IndexedTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.TableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.StringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.StringValueParser;
import com.exactprosystems.clearth.utils.tabledata.primarykeys.PrimaryKey;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;

import java.io.IOException;

/**
 * Comparator for indexed data sets which works with columns and row values represented as strings.
 */
public class IndexedStringTableDataComparator<C extends PrimaryKey> extends IndexedTableDataComparator<String, String, C>
{
	public IndexedStringTableDataComparator(BasicTableDataReader<String, String, ?> expectedReader,
			BasicTableDataReader<String, String, ?> actualReader,
			                                TableRowMatcher<String, String, C> rowMatcher,
			TableRowsComparator<String, String> rowsComparator) throws IOException, ParametersException
	{
		super(expectedReader, actualReader, rowMatcher, rowsComparator, new StringValueParser());
	}
	
	public IndexedStringTableDataComparator(BasicTableDataReader<String, String, ?> expectedReader,
			BasicTableDataReader<String, String, ?> actualReader,
			                                TableRowMatcher<String, String, C> rowMatcher,
			ComparisonUtils comparisonUtils) throws IOException, ParametersException
	{
		super(expectedReader, actualReader, rowMatcher,
				new TableRowsComparator<>(new StringValuesComparator(comparisonUtils)), new StringValueParser());
	}
	
	
	@Override
	protected IndexedTableData<String, String, C> createExpectedStorage(TableHeader<String> header,
			TableRowMatcher<String, String, C> rowMatcher) throws IOException
	{
		return new IndexedStringTableData<>(header, rowMatcher);
	}

	@Override
	protected IndexedTableData<String, String, C> createActualStorage(TableHeader<String> header,
			TableRowMatcher<String, String, C> rowMatcher) throws IOException
	{
		return new IndexedStringTableData<>(header, rowMatcher);
	}
}
