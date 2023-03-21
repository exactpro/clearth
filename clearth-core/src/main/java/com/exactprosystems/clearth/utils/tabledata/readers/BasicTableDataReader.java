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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.tabledata.*;

import java.io.IOException;
import java.util.Set;

/**
 * Abstract ancestor for classes that read table-like data from various sources
 * @author vladimir.panarin
 * @param <A> class of header members
 * @param <B> class of values
 * @param <C> class of table data returned
 */
public abstract class BasicTableDataReader<A, B, C extends BasicTableData<A, B>> implements TableDataReader<C>
{
	protected C tableData;
	protected RowsListFactory<A, B> rowsListFactory;
	protected TableRowConverter<A, B> tableRowConverter;
	
	public void setRowsListFactory(RowsListFactory<A, B> rowsListFactory)
	{
		this.rowsListFactory = rowsListFactory;
	}
	
	public void setTableRowConverter(TableRowConverter<A, B> tableRowConverter)
	{
		this.tableRowConverter = tableRowConverter;
	}
	
	/**
	 * Reads the whole data source
	 * @return table with header that corresponds to data source and rows that contain all data
	 * @throws IOException
	 */
	@Override
	public C readAllData() throws IOException
	{
		start();
		while (hasMoreData())
		{
			if (!filter())
				continue;
			fillRow();
		}
		return getTableData();
	}
	
	/**
	 * Starts reading of table-like data from source. close() must be called when reading is finished
	 * @return table that will store rows read from source
	 * @throws IOException if error occurred while reading data header
	 */
	public C start() throws IOException
	{
		Set<A> header = readHeader();
		if (rowsListFactory == null)
			rowsListFactory = RowsListFactories.linkedListFactory();
		tableData = createTableData(header, rowsListFactory);
		return tableData;
	}
	
	public C getTableData()
	{
		return tableData;
	}
	
	/**
	 * Creates new row and fills it with current data from source. Row is not added to table being read
	 * @return row filled with current data from source
	 * @throws IOException if error occurred while reading data
	 */
	public TableRow<A, B> readRow() throws IOException
	{
		TableRow<A, B> row = tableData.createRow();
		fillRow(row);
		if (tableRowConverter != null)
			row = tableRowConverter.convert(row);
		return row;
	}
	
	/**
	 * Adds new row to table being read and fills it with current data from source
	 * @throws IOException if error occurred while reading data
	 */
	public void fillRow() throws IOException
	{
		TableRow<A, B> row = readRow();
		tableData.add(row);
	}
	
	
	/**
	 * Reads set of header cells to be used as header in table
	 * @return set of header cells obtained from data source
	 * @throws IOException if error occurred while reading header
	 */
	protected abstract Set<A> readHeader() throws IOException;
	/**
	 * Indicates if data source has data to make row by
	 * @return true if data source has more data to process, false otherwise. Need to call close() if false is returned
	 * @throws IOException
	 */
	public abstract boolean hasMoreData() throws IOException;
	/**
	 * Fills given TableRow with data obtained from data source.
	 * @param row to fill with data
	 * @throws IOException
	 */
	protected abstract void fillRow(TableRow<A, B> row) throws IOException;
	/**
	 * Allows to prevent reading of a row if it doesn't fit some criteria
	 * @return true if current data row needs to be added to result table
	 * @throws IOException
	 */
	public abstract boolean filter() throws IOException;
	
	/**
	 * Creates table bound to given header. This object will store data from source and will be returned when reading is finished
	 * @param header to bound to method's result
	 * @return new storage for data from source
	 */
	protected abstract C createTableData(Set<A> header, RowsListFactory<A, B> rowsListFactory);
}
