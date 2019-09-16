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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.list.UnmodifiableList;

/**
 * Storage of table-like data with predefined header
 * @author vladimir.panarin
 * @param <A> class of header members
 * @param <B> class of values in table rows
 */
public class TableData<A, B> extends BasicTableData<A, B> implements Iterable<TableRow<A, B>>
{
	protected final List<TableRow<A, B>> rows;  //Must not provide direct access to this one else any row with any header can be added
	
	
	public TableData(Set<A> header)
	{
		this(header, RowsListFactories.<A, B>linkedListFactory());
	}
	
	public TableData(Set<A> header, RowsListFactory<A, B> rowsListFactory)
	{
		super(header);
		this.rows = rowsListFactory.createRowsList();
	}
	
	public TableData(TableHeader<A> header)
	{
		this(header, RowsListFactories.<A, B>linkedListFactory());
	}
	
	public TableData(TableHeader<A> header, RowsListFactory<A, B> rowsListFactory)
	{
		super(header);
		this.rows = rowsListFactory.createRowsList();
	}
	
	
	@Override
	public Iterator<TableRow<A, B>> iterator()
	{
		return rows.iterator();
	}
	
	@Override
	public void add(TableRow<A, B> row) throws IllegalArgumentException
	{
		checkRowHeader(row, header);
		rows.add(row);
	}
	
	@Override
	public void clear()
	{
		rows.clear();
	}
	
	public TableRow<A, B> removeRow(int index)
	{
		return rows.remove(index);
	}
	
	public TableRow<A, B> getRow(int index)
	{
		return rows.get(index);
	}
	
	/**
	 * @return unmodifiable list of table rows
	 */
	public List<TableRow<A, B>> getRows()
	{
		return new UnmodifiableList<TableRow<A, B>>(rows);
	}
	
	/**
	 * @return number of rows in table
	 */
	@Override
	public int size()
	{
		return rows.size();
	}
	
	@Override
	public boolean isEmpty()
	{
		return rows.isEmpty();
	}
}
