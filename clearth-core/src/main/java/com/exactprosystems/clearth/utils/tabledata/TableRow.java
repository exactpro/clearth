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

import org.apache.commons.collections4.iterators.UnmodifiableIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Row of table-like data bound to given header. Always has the same size as header
 * @author vladimir.panarin
 * @param <A> class of header members
 * @param <B> class of values
 */
public class TableRow<A, B> implements Iterable<B>
{
	private final TableHeader<A> header;
	private final List<B> values;
	
	public TableRow(TableHeader<A> header)
	{
		this.header = header;
		values = createValuesList(header);
	}
	
	public TableRow(TableHeader<A> header, Collection<B> values) throws IllegalArgumentException
	{
		this.header = header;
		this.values = createValuesList(header, values);
	}
	
	
	@Override
	public Iterator<B> iterator()
	{
		return UnmodifiableIterator.unmodifiableIterator(values.iterator());  //This prevents columns removal from row
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		for (A column : header)
		{
			if (first)
				first = false;
			else
				sb.append(',');

			B value = getValue(column);

			sb.append("'").append(column).append("'")
					.append('=')
					.append("'").append(value).append("'");
		}
		sb.append('}');
		return sb.toString();
	}
	
	/**
	 * @return number of columns in row
	 */
	public int size()
	{
		return values.size();
	}
	
	/**
	 * @return header the row is bound to
	 */
	public TableHeader<A> getHeader()
	{
		return header;
	}
	
	/**
	 * Gives value of row cell obtained by column
	 * @param column to obtain value of
	 * @return value of row cell, null if header doesn't contain such column
	 */
	public B getValue(A column)
	{
		int index = header.columnIndex(column);
		if (index < 0)
			return null;
		return values.get(index);
	}
	
	/**
	 * Gives value of row cell obtained by column index
	 * @param columnIndex to obtain value of
	 * @return value of row cell
	 */
	public B getValue(int columnIndex)
	{
		return values.get(columnIndex);
	}
	
	/**
	 * Stores value in given row column
	 * @param column to store value in
	 * @param value to store
	 * @throws IllegalArgumentException if header doesn't contain given column 
	 */
	public void setValue(A column, B value) throws IllegalArgumentException
	{
		int index = header.columnIndex(column);
		if (index < 0)
			throw new IllegalArgumentException("Row doesn't contain column '"+column+"'");
		setValue(index, value);
	}
	
	/**
	 * Stores value in given row column
	 * @param columnIndex to store value in
	 * @param value       to store
	 */
	public void setValue(int columnIndex, B value)
	{
		values.set(columnIndex, value);
	}
	
	
	/**
	 * Create list of values. Values list must be null or list size must be equal to size of given header
	 * @param header returned list will have the same number of elements as number of columns in header
	 * @param values collection of values. Can be null
	 * @return list to store row values
	 */
	protected List<B> createValuesList(TableHeader<A> header, Collection<B> values) throws IllegalArgumentException
	{
		if (values != null)
			return createValuesList(values);
		else
			return createValuesList(header);
	}
	
	/**
	 * Create list of values. List size must be equal to size of given header
	 * @param values collection of values to fill row
	 * @return list to store row values
	 */
	protected List<B> createValuesList(Collection<B> values) throws IllegalArgumentException
	{
		checkValues(header, values);
		return new ArrayList<B>(values);
	}
	
	/**
	 * Initializes list to store row values. List size must be equal to size of given header
	 * @param header returned list will have the same number of elements as number of columns in header
	 * @return list to store row values
	 */
	protected List<B> createValuesList(TableHeader<A> header)
	{
		List<B> result = new ArrayList<B>(header.size());
		for (int i = 0; i < header.size(); i++)
			result.add(null);
		return result;
	}
	
	/**
	 * Verifies that values is not null and values size is equal to header size
	 * @param header to compare size
	 * @param values to compare size
	 * @throws IllegalArgumentException if values size is not equal to header size
	 */
	protected void checkValues(TableHeader<A> header, Collection<B> values) throws IllegalArgumentException
	{
		if (values == null)
			throw new IllegalArgumentException("Values is null");
		if (values.size() != header.size())
			throw new IllegalArgumentException("Values size is not equal to header size");
	}
	
	public void setValues(Collection<B> values) throws IllegalArgumentException
	{
		if (values == null)
			return;
		checkValues(header, values);
		int i = 0;
		for (Iterator<B> it = values.iterator(); it.hasNext(); i++)
			setValue(i, it.next());
	}
	
	public List<B> getValues()
	{
		return Collections.unmodifiableList(values);
	}
	
	public void clearValues()
	{
		for (int i = 0; i < values.size(); i++)
			setValue(i, null);
	}
}
