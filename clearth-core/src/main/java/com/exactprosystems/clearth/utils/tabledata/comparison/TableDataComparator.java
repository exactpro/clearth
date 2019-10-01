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

import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.DefaultValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;

import java.io.IOException;
import java.util.LinkedHashSet;

/**
 * Basic class of table data comparator.
 * Uses 2 table data readers to read and compare rows line by line.
 * @param <A> class of header members.
 * @param <B> class of values in table rows.
 */
public class TableDataComparator<A, B> implements AutoCloseable
{
	protected final BasicTableDataReader<A, B, ?> expectedReader, actualReader;
	protected final TableHeader<A> expectedHeader, actualHeader;
	protected final LinkedHashSet<A> commonHeader;
	
	protected boolean expectedReadMore, actualReadMore;
	protected final ValuesComparator<A, B> valuesComparator;
	
	public TableDataComparator(BasicTableDataReader<A, B, ?> expectedReader, BasicTableDataReader<A, B, ?> actualReader,
			ValuesComparator<A, B> valuesComparator) throws IOException
	{
		this.expectedReader = expectedReader;
		this.actualReader = actualReader;
		this.valuesComparator = valuesComparator;
		
		this.expectedReader.start();
		this.actualReader.start();
		expectedHeader = expectedReader.getTableData().getHeader();
		actualHeader = actualReader.getTableData().getHeader();
		
		// Create a common header which contains columns to read from expected and actual data sources
		commonHeader = new LinkedHashSet<>();
		expectedHeader.forEach(commonHeader::add);
		actualHeader.forEach(commonHeader::add);
	}
	
	public TableDataComparator(BasicTableDataReader<A, B, ?> expectedReader, BasicTableDataReader<A, B, ?> actualReader) throws IOException
	{
		this(expectedReader, actualReader, new DefaultValuesComparator<>());
	}
	
	/**
	 * Checks if data sets have more rows to be read and compared.
	 * @return {@code true}, if comparison could be continued; {@code false} otherwise.
	 */
	public boolean hasMoreRows() throws IOException
	{
		expectedReadMore = expectedReader.hasMoreData();
		actualReadMore = actualReader.hasMoreData();
		return expectedReadMore || actualReadMore;
	}
	
	/**
	 * Reads and compares next pair of rows from expected and actual sources.
	 * @return {@code RowComparisonData} object presents result of comparing pair of rows.
	 * @throws IOException if any I/O error occurred while reading/storing rows from the sources.
	 */
	public RowComparisonData<A, B> compareRows() throws IOException
	{
		// readRow() doesn't write rows to table data, so we needn't to remove them after comparison
		TableRow<A, B> expectedRow = expectedReadMore ? expectedReader.readRow() : null,
				actualRow = actualReadMore ? actualReader.readRow() : null;
		return compareCoupleOfRows(expectedRow, actualRow);
	}
	
	/**
	 * Closes table data readers used by comparator.
	 */
	@Override
	public void close() throws IOException
	{
		Utils.closeResource(expectedReader);
		Utils.closeResource(actualReader);
	}
	
	
	protected RowComparisonData<A, B> compareCoupleOfRows(TableRow<A, B> expectedRow, TableRow<A, B> actualRow) throws IllegalArgumentException
	{
		if (expectedRow == null && actualRow == null)
			throw new IllegalArgumentException("Both table row objects are null. Could not make comparison.");
		
		RowComparisonData<A, B> compData = new RowComparisonData<>();
		if (expectedRow == null || actualRow == null)
		{
			for (A column : commonHeader)
			{
				compData.addComparisonDetail(column, expectedRow != null ? expectedRow.getValue(column) : null,
						actualRow != null ? actualRow.getValue(column) : null, false);
			}
			return compData;
		}
		
		for (A column : commonHeader)
		{
			// Check if it's "unexpected" column which should be marked as 'INFO' in comparison results
			if (!expectedHeader.containsColumn(column))
			{
				compData.addInfoComparisonDetail(column, actualRow.getValue(column));
				continue;
			}
			
			B expectedValue = expectedRow.getValue(column), actualValue = actualRow.getValue(column);
			boolean identical;
			try
			{
				identical = valuesComparator.compareValues(expectedValue, actualValue, column);
			}
			catch (Exception e)
			{
				identical = false;
				compData.addErrorMsg(ExceptionUtils.getDetailedMessage(e));
			}
			compData.addComparisonDetail(column, expectedValue, actualValue, identical);
		}
		return compData;
	}
}
