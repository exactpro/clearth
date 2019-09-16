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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.iterators.UnmodifiableIterator;

/**
 * Header for table-like data. Maps header members with column indexes to quickly access row values by column.
 * @author vladimir.panarin
 * @param <A> class of header members
 */
public class TableHeader<A> implements Iterable<A>
{
	private final Map<A, Integer> columns;  //Stores names of columns and corresponding index for fast array list access
	
	public TableHeader(Set<A> columns)
	{
		this.columns = createColumnsMap();
		int i = -1;
		for (A c : columns)
		{
			i++;
			this.columns.put(c, i);
		}
	}
	
	
	@Override
	public Iterator<A> iterator()
	{
		return UnmodifiableIterator.unmodifiableIterator(columns.keySet().iterator());
	}
	
	@Override
	public String toString()
	{
		return columns.keySet().toString();
	}
	
	
	/**
	 * Gives index of column to access value by
	 * @param column header cell to get index of
	 * @return index of given header cell, -1 if no such column is present in header
	 */
	public int columnIndex(A column)
	{
		Integer index = columns.get(column);
		if (index == null)
			return -1;
		return index;
	}
	
	public boolean containsColumn(A column)
	{
		return columns.containsKey(column);
	}
	
	public int size()
	{
		return columns.size();
	}
	
	
	/**
	 * Initializes map to store header cells and corresponding column indexes
	 * @return map to store header cells with column indexes
	 */
	protected Map<A, Integer> createColumnsMap()
	{
		return new LinkedHashMap<A, Integer>();
	}
}
