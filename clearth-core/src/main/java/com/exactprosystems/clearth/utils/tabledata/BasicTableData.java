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

package com.exactprosystems.clearth.utils.tabledata;

import java.util.Set;

/**
 * Ancestor for all classes that implement storage of table-like data
 * @author vladimir.panarin
 * @param <A> class of header members
 * @param <B> class of values in table rows
 */
public abstract class BasicTableData<A, B>
{
	protected final TableHeader<A> header;
	
	public BasicTableData(Set<A> header)
	{
		this.header = createHeader(header);
	}
	
	public BasicTableData(TableHeader<A> header)
	{
		this.header = header;
	}
	
	
	/**
	 * Creates empty row that corresponds to table header
	 * @return newly created row
	 */
	public TableRow<A, B> createRow()
	{
		return createRow(header);
	}
	
	/**
	 * @return header the table is bound to
	 */
	public TableHeader<A> getHeader()
	{
		return header;
	}
	
	/**
	 * Adds given row to table. Row header must be the same as table header. Use createRow() to guarantee that row can be added to table
	 * @param row to add
	 * @throws IllegalArgumentException if row header is not the same as table header
	 */
	public abstract void add(TableRow<A, B> row) throws IllegalArgumentException;
	/**
	 * Removes all rows from table
	 */
	public abstract void clear();
	
	public abstract boolean isEmpty();
	
	/**
	 * Initializes table header according to given set of cells
	 * @param header set of cells to be used as table header
	 * @return table header to use while adding new rows
	 */
	protected TableHeader<A> createHeader(Set<A> header)
	{
		return new TableHeader<A>(header);
	}
	
	/**
	 * Creates empty row that is bound to given header
	 * @param header table header to create row for
	 * @return new row bound to given header
	 */
	protected TableRow<A, B> createRow(TableHeader<A> header)
	{
		return new TableRow<A, B>(header);
	}
	
	/**
	 * Verifies that row header is the same as given header
	 * @param row to check header
	 * @param header to compare with row header
	 * @throws IllegalArgumentException if row header is not the same as given header
	 */
	protected void checkRowHeader(TableRow<A, B> row, TableHeader<A> header) throws IllegalArgumentException
	{
		if (row.getHeader() != header)
			throw new IllegalArgumentException("Row header must be the same as table header");
	}

	/**
	 * @return number of rows in the table
	 */
	public abstract int size();
}
