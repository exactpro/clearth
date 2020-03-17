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

package com.exactprosystems.clearth.utils.tabledata.readers;

import java.io.IOException;
import java.util.Set;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.tabledata.IndexedTableData;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;

public class IndexedClearThMessageReader<C> extends AbstractClearThMessageReader<IndexedTableData<String, String, C>>
{
	protected final TableRowMatcher<String, String, C> matcher;

	public IndexedClearThMessageReader(ClearThMessage<?> message,
	                                   SubMessageFilter subMessageFilter,
	                                   TableRowFilter<String, String> rowFilter,
	                                   TableRowMatcher<String, String, C> matcher)
	{
		super(message, subMessageFilter, rowFilter);
		this.matcher = matcher;
	}
	
	public IndexedClearThMessageReader(ClearThMessage<?> message, 
	                                   SubMessageFilter filter, 
	                                   TableRowMatcher<String, String, C> matcher)
	{
		this(message, filter, null, matcher);
	}

	public IndexedClearThMessageReader(ClearThMessage<?> message,
	                                   TableRowFilter<String, String> rowFilter,
	                                   TableRowMatcher<String, String, C> matcher)
	{
		this(message, null, rowFilter, matcher);
	}

	public IndexedClearThMessageReader(ClearThMessage<?> message, TableRowMatcher<String, String, C> matcher)
	{
		this(message, null, null, matcher);
	}

	
	public static <C> IndexedTableData<String, String, C> read(ClearThMessage<?> message, TableRowMatcher<String, String, C> matcher) throws IOException
	{
		return read(message, null, null, matcher);
	}

	public static <C> IndexedTableData<String, String, C> read(ClearThMessage<?> message,
	                                                           SubMessageFilter filter,
	                                                           TableRowMatcher<String, String, C> matcher) throws IOException
	{
		return read(message, filter, null, matcher);
	}

	public static <C> IndexedTableData<String, String, C> read(ClearThMessage<?> message,
	                                                           TableRowFilter<String, String> rowFilter,
	                                                           TableRowMatcher<String, String, C> matcher) throws IOException
	{
		return read(message, null, rowFilter, matcher);
	}

	public static <C> IndexedTableData<String, String, C> read(ClearThMessage<?> message,
	                                                           SubMessageFilter filter,
	                                                           TableRowFilter<String, String> rowFilter,
	                                                           TableRowMatcher<String, String, C> matcher) throws IOException
	{
		try (IndexedClearThMessageReader<C> reader = new IndexedClearThMessageReader<C>(message, filter, rowFilter, matcher))
		{
			return reader.readAllData();
		}
	}

	
	@Override
	protected IndexedTableData<String, String, C> createTableData(Set<String> header,
	                                                              RowsListFactory<String, String> rowsListFactory)
	{
		return new IndexedTableData<String, String, C>(header, matcher, rowsListFactory);
	}
}
