/******************************************************************************
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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.tabledata.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public abstract class AbstractMemoryTableDataReader<A, B, C extends BasicTableData<A, B>> extends BasicTableDataReader<A, B, C>
{
	protected final Collection<TableRow<A, B>> data;
	protected TableRowFilter<A, B> rowFilter;
	protected Iterator<TableRow<A, B>> iterator;
	protected TableRow<A, B> currentRow;
	protected TableHeader<A> header;
	protected boolean copyRow = true;

	public AbstractMemoryTableDataReader(Collection<TableRow<A, B>> data) throws IOException
	{
		//Header cannot be created in this case, so reader immediately fails
		if (data.isEmpty())
			throw new IOException("Empty data list cannot be used for reader");

		this.data = data;
		this.iterator = this.data.iterator();
		updateCurrentRow();
		this.header = currentRow.getHeader();
	}
	
	public AbstractMemoryTableDataReader(TableData<A, B> data) throws IOException
	{
		//Collection of rows instead of table is used to make data read only
		this.data = data.getRows();
		this.iterator = this.data.iterator();
		this.header = data.getHeader();
		updateCurrentRow();
	}

	public void setCopyRow(boolean value)
	{
		this.copyRow = value;
	}
	
	public void setRowFilter(TableRowFilter<A, B> rowFilter)
	{
		this.rowFilter = rowFilter;
	}

	@Override
	protected Set<A> readHeader() throws IOException
	{
		return this.header.toSet();
	}

	@Override
	public C start() throws IOException
	{
		if (rowsListFactory == null)
			rowsListFactory = RowsListFactories.linkedListFactory();
		tableData = createTableData(this.header, rowsListFactory);
		return tableData;
	}

	@Override
	public void close()
	{
	}
	
	@Override
	public boolean hasMoreData() throws IOException
	{
		return currentRow != null;
	}
	
	protected void updateCurrentRow() throws IOException
	{
		if (iterator.hasNext())
			currentRow = iterator.next();
		else
			currentRow = null;
	}
	
	
	@Override
	public TableRow<A, B> readRow() throws IOException
	{
		checkCurrentRowHeader();
		TableRow<A, B> result = null;
		if (!copyRow)
		{
			if (tableRowConverter != null)
				throw new IOException("TableRowConverter cannot be used when reader doesn't copy rows.");
			result = getUnmodifiableRow();
			
		}
		else
		{
			result = super.readRow();
		}
		updateCurrentRow();
		return result;
	}

	protected TableRow<A, B> getUnmodifiableRow() throws IOException
	{
		TableRow<A, B> result = new UnmodifiableTableRow(currentRow);
		return result;
	}

	@Override
	protected void fillRow(TableRow<A, B> row) throws IOException
	{
		for (A column : row.getHeader())
			row.setValue(column, currentRow.getValue(column));
	}
	
	protected void checkCurrentRowHeader() throws IOException
	{
		if (currentRow.getHeader() != header)
			throw new IOException("Header of current row is not the same as for previous row");
	}

	@Override
	public boolean filter() throws IOException
	{
		return (rowFilter == null) || rowFilter.filter(currentRow);
	}

	@Override
	protected final C createTableData(Set<A> header, RowsListFactory<A, B> rowsListFactory)
	{
		throw new UnsupportedOperationException();
	}

	protected abstract C createTableData(TableHeader<A> header, RowsListFactory<A, B> rowsListFactory);

}
