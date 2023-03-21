/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonData;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.TableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.SimpleValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.ValueParser;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Basic class of table data comparator.
 * Uses 2 table data readers to read and compare rows line by line.
 * @param <A> class of header members.
 * @param <B> class of values in table rows.
 */
public class TableDataComparator<A, B> implements AutoCloseable
{
	protected final BasicTableDataReader<A, B, ?> expectedReader, actualReader;
	protected final TableHeader<A> expectedHeader, actualHeader, commonHeader;
	protected TableRow<A, B> currentRow;
	
	protected boolean expectedReadMore, actualReadMore;
	protected final TableRowsComparator<A, B> rowsComparator;
	protected final ValueParser<A, B> valueParser;
	
	public TableDataComparator(BasicTableDataReader<A, B, ?> expectedReader, BasicTableDataReader<A, B, ?> actualReader,
			TableRowsComparator<A, B> rowsComparator, ValueParser<A, B> valueParser) throws IOException
	{
		this.expectedReader = expectedReader;
		this.actualReader = actualReader;
		this.rowsComparator = rowsComparator;
		this.valueParser = valueParser;
		
		this.expectedReader.start();
		this.actualReader.start();
		expectedHeader = expectedReader.getTableData().getHeader();
		actualHeader = actualReader.getTableData().getHeader();
		
		// Create a common header which contains columns to read from expected and actual data sources
		Set<A> commonHeaderSet = new LinkedHashSet<>();
		expectedHeader.forEach(commonHeaderSet::add);
		actualHeader.forEach(commonHeaderSet::add);
		commonHeader = new TableHeader<>(commonHeaderSet);
	}
	
	public TableDataComparator(BasicTableDataReader<A, B, ?> expectedReader,
	                           BasicTableDataReader<A, B, ?> actualReader, 
	                           ValueParser<A, B> valueParser)
			throws IOException
	{
		this(expectedReader, actualReader, new TableRowsComparator<>(new SimpleValuesComparator<>()), valueParser);
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
	 * @return {@link RowComparisonData} object presents result of comparing pair of rows.
	 * @throws IOException if any I/O error occurred while reading/storing rows from the sources.
	 */
	public RowComparisonData<A, B> compareRows() throws IOException
	{
		// readRow() doesn't write rows to table data, so we needn't to remove them after comparison
		TableRow<A, B> expectedRow = expectedReadMore ? expectedReader.readRow() : null,
				actualRow = actualReadMore ? actualReader.readRow() : null;
		
		currentRow = expectedRow != null ? expectedRow : actualRow;
		return rowsComparator.compareRows(expectedRow, actualRow, commonHeader);
	}
	
	public TableRow<A, B> getCurrentRow()
	{
		return currentRow;
	}
	
	public ValuesComparator<A, B> getValuesComparator()
	{
		return rowsComparator.getValuesComparator();
	}
	
	public ValueParser<A, B> getValueParser()
	{
		return valueParser;
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
}
