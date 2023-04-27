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

package com.exactprosystems.clearth.utils.tabledata;

import java.lang.UnsupportedOperationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Unmodifiable wrapper for row of table-like data
 * @param <A> class of header members
 * @param <B> class of values
 */

public class UnmodifiableTableRow<A, B> extends TableRow<A, B>
{
	private final TableRow<A, B> data;
	
	public UnmodifiableTableRow(TableRow<A, B> data)
	{
		super(null);
		this.data = data;
	}
	
	@Override
	public Iterator<B> iterator()
	{
		return data.iterator();
	}
	
	@Override
	public String toString()
	{
		return data.toString();
	}

	@Override
	public int size()
	{
		return data.size();
	}
	
	@Override
	public TableHeader<A> getHeader()
	{
		return data.getHeader();
	}
	
	@Override
	public B getValue(A column)
	{
		return data.getValue(column);
	}
	
	@Override
	public B getValue(int columnIndex)
	{
		return data.getValue(columnIndex);
	}
	
	@Override
	public void setValue(A column, B value) throws IllegalArgumentException
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setValue(int columnIndex, B value)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setValues(Collection<B> values) throws IllegalArgumentException
	{
		
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<B> getValues()
	{
		return data.getValues();
	}
	
	@Override
	public void clearValues()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	protected List<B> createValuesList(TableHeader<A> header)
	{
		return null;
	}
}
