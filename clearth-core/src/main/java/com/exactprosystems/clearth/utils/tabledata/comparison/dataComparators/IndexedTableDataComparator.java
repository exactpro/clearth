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
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.IndexedTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonData;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.TableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.SimpleValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.ValueParser;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;

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
			TableRowMatcher<A, B, C> rowMatcher, TableRowsComparator<A, B> rowsComparator, ValueParser<A, B> valueParser)
			throws IOException, ParametersException
	{
		super(expectedReader, actualReader, rowsComparator, valueParser);
		this.rowMatcher = rowMatcher;
		checkHeaders();
		
		expectedStorage = createExpectedStorage(expectedHeader, rowMatcher);
		actualStorage = createActualStorage(actualHeader, rowMatcher);
	}
	
	protected void checkHeaders() throws ParametersException
	{
		checkHeader(expectedHeader, true);
		checkHeader(actualHeader, false);
	}
	
	protected void checkHeader(TableHeader<A> header, boolean forExpected) throws ParametersException
	{
		try
		{
			rowMatcher.checkHeader(header);
		}
		catch (ParametersException e)
		{
			throw new ParametersException("Problem occurred while validating " +
					(forExpected ? "expected" : "actual") +
					" header " + header + ": " + e.getMessage(), e);
		}
	}
	
	public IndexedTableDataComparator(BasicTableDataReader<A, B, ?> expectedReader, BasicTableDataReader<A, B, ?> actualReader,
			TableRowMatcher<A, B, C> rowMatcher, ValueParser<A, B> valueParser) throws IOException, ParametersException
	{
		this(expectedReader, actualReader, rowMatcher, new TableRowsComparator<>(new SimpleValuesComparator<>()), valueParser);
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
				actualRow = findActualRowByExpectedRow(expectedRow);
			}
			else
			{
				actualRow = actualReader.readRow();
				expectedRow = findExpectedRowByActualRow(actualRow);
			}
		}
		while ((expectedRow == null || actualRow == null) && hasMoreRows());
		
		currentRow = expectedRow != null ? expectedRow : actualRow;
		return rowsComparator.compareRows(expectedRow, actualRow, commonHeader);
	}
	
	/**
	 * Returns {@link TableRowMatcher} used by this indexed comparator to match rows before comparison.
	 */
	public TableRowMatcher<A, B, C> getRowMatcher()
	{
		return rowMatcher;
	}

	protected TableRow<A, B> findActualRowByExpectedRow(TableRow<A, B> expectedRow)
	{
		return findByRow(expectedRow, true);
	}

	protected TableRow<A, B> findExpectedRowByActualRow(TableRow<A, B> actualRow)
	{
		return findByRow(actualRow, false);
	}

	protected TableRow<A, B> findByRow(TableRow<A, B> inputRow, boolean isExpectedRow)
	{
		IndexedTableData<A, B, C> storageToSave;
		IndexedTableData<A, B, C> storageToRemove;

		if (isExpectedRow)
		{
			storageToSave = expectedStorage;
			storageToRemove = actualStorage;
		}
		else
		{
			storageToSave = actualStorage;
			storageToRemove = expectedStorage;
		}

		TableRow<A, B> targetRow = findAndRemoveFromStorage(storageToRemove, inputRow, isExpectedRow);

		if (targetRow == null)
			storageToSave.add(inputRow);

		return targetRow;
	}

	protected TableRow<A, B> findAndRemoveFromStorage(IndexedTableData<A, B, C> storage, TableRow<A, B> row, boolean isExpectedRow)
	{
		return storage.findAndRemove(row);
	}

	/**
	 * Returns some table row (e.g. a first one) from table data storage.
	 * Could be used to pull stored rows when sources were read completely.
	 * @param tableData {@link IndexedTableData} where to search for some row.
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
