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

import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;
import org.apache.commons.collections4.list.UnmodifiableList;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;


/**
 * Indexed storage of table-like data with predefined header.
 * Rows are arranged into buckets (thus rows order is not always kept) by primary key which is created by matcher required for this class
 * @author vladimir.panarin
 * @param <A> class of header members
 * @param <B> class of values in table rows
 * @param <C> class of primary key
 */
public class IndexedTableData<A, B, C> extends BasicTableData<A, B> implements Iterable<C>
{
	protected final Map<C, List<TableRow<A, B>>> rows;  //Must not provide direct access to this map else any row with any header can be added
	protected final TableRowMatcher<A, B, C> matcher;
	protected final RowsListFactory<A, B> rowsListFactory;
	protected int rowsCount;

	public IndexedTableData(Set<A> header, TableRowMatcher<A, B, C> matcher)
	{
		this(header, matcher, RowsListFactories.<A, B>linkedListFactory());
	}

	public IndexedTableData(Set<A> header,
	                        TableRowMatcher<A, B, C> matcher,
	                        RowsListFactory<A, B> rowsListFactory)
	{
		super(header);
		this.rows = createRowsMap();
		this.matcher = matcher;
		this.rowsListFactory = rowsListFactory;
	}

	public IndexedTableData(TableHeader<A> header, TableRowMatcher<A, B, C> matcher)
	{
		this(header, matcher, RowsListFactories.<A, B>linkedListFactory());
	}

	public IndexedTableData(TableHeader<A> header,
	                        TableRowMatcher<A, B, C> matcher,
	                        RowsListFactory<A, B> rowsListFactory)
	{
		super(header);
		this.rows = createRowsMap();
		this.matcher = matcher;
		this.rowsListFactory = rowsListFactory;
	}


	@Override
	public Iterator<C> iterator()
	{
		return new IndexedTableDataIterator();
	}

	/**
	 * Adds given row to table. Row header must be the same as table header. Use createRow() to guarantee that row can be added to table.
	 * Bucket to store row is selected by primary key created for given row by matcher.
	 * @param row to add
	 * @throws IllegalArgumentException if row header is not the same as table header
	 */
	@Override
	public void add(TableRow<A, B> row) throws IllegalArgumentException
	{
		checkRowHeader(row, header);

		C primaryKey = matcher.createPrimaryKey(row);
		List<TableRow<A, B>> bucket = rows.get(primaryKey);
		if (bucket == null)
		{
			bucket = rowsListFactory.createRowsList();
			rows.put(primaryKey, bucket);
		}
		bucket.add(row);
		rowsCount++;
	}

	@Override
	public void clear()
	{
		rows.clear();
		rowsCount = 0;
	}

	/**
	 * Finds in table a row that matches given row. Optionally removes found row from table. Correspondence in found by table's matcher.
	 * @param row to find corresponding row for
	 * @param remove flag to trigger removal of found row
	 * @return row found in table, null if no row matches both primary and secondary keys of given row
	 */
	public TableRow<A, B> find(TableRow<A, B> row, boolean remove)
	{
		C primaryKey = matcher.createPrimaryKey(row);
		List<TableRow<A, B>> bucket = rows.get(primaryKey);
		if (bucket == null)
			return null;

		Iterator<TableRow<A, B>> it = bucket.iterator();
		while (it.hasNext())
		{
			TableRow<A, B> r = it.next();
			if (!matcher.matchBySecondaryKey(row, r))
				continue;

			if (remove)
			{
				it.remove();
				if (bucket.isEmpty())
					rows.remove(primaryKey);
				rowsCount--;
			}
			return r;
		}
		return null;
	}

	/**
	 * Finds in table a row that matches given row
	 * @param row to find corresponding row for
	 * @return row found in table, null if no row matches both primary and secondary keys of given row
	 */
	public TableRow<A, B> find(TableRow<A, B> row)
	{
		return find(row, false);
	}

	/**
	 * Finds in table a row that matches given row and removes that row from table
	 * @param row to find corresponding row for
	 * @return row found in table, null if no row matches both primary and secondary keys of given row
	 */
	public TableRow<A, B> findAndRemove(TableRow<A, B> row)
	{
		return find(row, true);
	}

	/**
	 * Finds bucket of rows that correspond to given primary key
	 * @param primaryKey to find bucket by
	 * @return unmodifiable bucket of table rows, null if no bucket matches primary key
	 */
	public List<TableRow<A, B>> findAll(C primaryKey)
	{
		List<TableRow<A, B>> bucket = rows.get(primaryKey);
		if (bucket == null)
			return null;
		return new UnmodifiableList<TableRow<A, B>>(bucket);
	}
	
	/**
	 * Finds bucket of rows that correspond to primary key of given row
	 * @param row is used to create primary key to find bucket by
	 * @return unmodifiable bucket of table rows
	 */
	public List<TableRow<A, B>> findAll(TableRow<A, B> row)
	{
		C primaryKey = matcher.createPrimaryKey(row);
		return findAll(primaryKey);
	}

	/**
	 * Finds bucket of rows that correspond to primary key built for given values collection
	 * @param rowValues is used to create primary key to find bucket by
	 * @return unmodifiable bucket of table rows
	 */
	public List<TableRow<A, B>> findAll(Collection<B> rowValues)
	{
		C primaryKey = matcher.createPrimaryKey(rowValues);
		return findAll(primaryKey);
	}

	@Override
	public boolean isEmpty()
	{
		return rows.isEmpty();
	}

	/**
	 * Initializes map to store indexed table data
	 * @return map which provides access to bucket (list) of data rows by primary key
	 */
	protected Map<C, List<TableRow<A, B>>> createRowsMap()
	{
		return new LinkedHashMap<C, List<TableRow<A, B>>>();
	}

	/**
	 * @return the number of rows in this table
	 */
	@Override
	public int size() {
		return rowsCount;
	}

	final class IndexedTableDataIterator implements Iterator<C> {

		private Iterator<C> iterator;
		private C currentKey;

		IndexedTableDataIterator()
		{
			iterator = rows.keySet().iterator();
		}

		@Override
		public boolean hasNext()
		{
			return iterator.hasNext();
		}

		@Override
		public C next()
		{
			currentKey = iterator.next();
			return currentKey;
		}

		@Override
		public void remove()
		{
			List<TableRow<A, B>> tableRows = rows.get(currentKey);
			if(tableRows != null)
				rowsCount -= tableRows.size();
			iterator.remove();
		}
	}
}
