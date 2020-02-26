/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils.tabledata.comparison;

import com.exactprosystems.clearth.utils.tabledata.*;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.DefaultValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;

import java.io.IOException;

/**
 * Table data comparator used for indexed data sets.
 * Rows to compare are matched by their primary keys.
 * @param <A> class of header members.
 * @param <B> class of values in table rows.
 * @param <C> class of primary key.
 */
public abstract class IndexedTableDataComparator<A, B, C> extends TableDataComparator<A, B>
{
	protected IndexedTableData<A, B, C> expectedStorage, actualStorage;
	protected TableRowMatcher<A, B, C> rowMatcher;
	
	
	public IndexedTableDataComparator(BasicTableDataReader<A, B, ?> expectedReader, BasicTableDataReader<A, B, ?> actualReader,
			TableRowMatcher<A, B, C> rowMatcher, ValuesComparator<A, B> valuesComparator) throws IOException
	{
		super(expectedReader, actualReader, valuesComparator);
		this.rowMatcher = rowMatcher;
		
		expectedStorage = createExpectedStorage(expectedHeader, rowMatcher);
		actualStorage = createActualStorage(actualHeader, rowMatcher);
	}
	
	public IndexedTableDataComparator(BasicTableDataReader<A, B, ?> expectedReader, BasicTableDataReader<A, B, ?> actualReader,
			TableRowMatcher<A, B, C> rowMatcher) throws IOException
	{
		this(expectedReader, actualReader, rowMatcher, new DefaultValuesComparator<>());
	}
	
	/**
	 * Checks if data sets have more rows to be read and compared or expected/actual storages aren't empty.
	 */
	@Override
	public boolean hasMoreRows() throws IOException
	{
		return super.hasMoreRows() || !expectedStorage.isEmpty() || !actualStorage.isEmpty();
	}
	
	/**
	 * Reads and tries to compare next pair of rows.
	 * Rows are found by their primary keys in sources or storages.
	 */
	@Override
	public RowComparisonData<A, B> compareRows() throws IOException
	{
		TableRow<A, B> expectedRow = null, actualRow;
		do
		{
			if (!expectedReadMore && !actualReadMore)
			{
				// Get rows for comparison from table data objects because sources have no more data to read
				if (!expectedStorage.isEmpty())
				{
					expectedRow = getSomeTableRow(expectedStorage);
					actualRow = actualStorage.findAndRemove(expectedRow);
				}
				else
					actualRow = getSomeTableRow(actualStorage);
				break;
			}
			
			// Read next rows from sources and try to find ones which match
			if (expectedReadMore)
			{
				expectedRow = expectedReader.readRow();
				if (actualReadMore)
					actualStorage.add(actualReader.readRow());
				actualRow = actualStorage.findAndRemove(expectedRow);
				
				if (actualRow == null)
					expectedStorage.add(expectedRow);
			}
			else
			{
				actualRow = actualReader.readRow();
				expectedRow = expectedStorage.findAndRemove(actualRow);
				
				if (expectedRow == null)
					actualStorage.add(actualRow);
			}
		}
		while ((expectedRow == null || actualRow == null) && hasMoreRows());
		if(expectedRow != null)
		{
			currentRow = expectedRow;
		}
		else
		{
			currentRow = actualRow;
		}
		return compareCoupleOfRows(expectedRow, actualRow);
	}
	
	/**
	 * Returns some table row (usually a first one) from table data storage.
	 * Could be used to pull stored rows when sources read fully.
	 * @param tableData {@code IndexedTableData} where to search for some row
	 */
	protected TableRow<A, B> getSomeTableRow(IndexedTableData<A, B, C> tableData)
	{
		// Just return first found table row and remove it from table data object
		TableRow<A, B> foundRow = tableData.findAll(tableData.iterator().next()).iterator().next();
		tableData.findAndRemove(foundRow);
		return foundRow;
	}
	
	
	/**
	 * Creates expected table data to store non-compared yet rows.
	 * @param header of the storage.
	 * @param rowMatcher to be used for finding appropriate rows for comparison.
	 * @throws IOException if any I/O error occurred while instantiating new table data object.
	 */
	protected abstract IndexedTableData<A, B, C> createExpectedStorage(TableHeader<A> header, TableRowMatcher<A, B, C> rowMatcher) throws IOException;
	
	/**
	 * Creates actual table data to store non-compared yet rows.
	 * @param header of the storage.
	 * @param rowMatcher to be used for finding appropriate rows for comparison.
	 * @throws IOException if any I/O error occurred while instantiating new table data object.
	 */
	protected abstract IndexedTableData<A, B, C> createActualStorage(TableHeader<A> header, TableRowMatcher<A, B, C> rowMatcher) throws IOException;
}
