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

import com.exactprosystems.clearth.utils.tabledata.IndexedTableData;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.primarykeys.PrimaryKey;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

/**
 * Reader of indexed table-like data from CSV data source
 * @author vladimir.panarin
 */
public class IndexedCsvDataReader<C extends PrimaryKey> extends AbstractStringCsvDataReader<IndexedTableData<String, String, C>>
{
	protected final TableRowMatcher<String, String, C> matcher;
	
	public IndexedCsvDataReader(File f, TableRowMatcher<String, String, C> matcher) throws IOException
	{
		super(f);
		this.matcher = matcher;
	}
	
	public IndexedCsvDataReader(Reader reader, TableRowMatcher<String, String, C> matcher) throws IOException
	{
		super(reader);
		this.matcher = matcher;
	}

	@Override
	protected IndexedTableData<String, String, C> createTableData(Set<String> header, 
	                                                              RowsListFactory<String, String> rowsListFactory)
	{
		return new IndexedTableData<String, String, C>(header, matcher, rowsListFactory);
	}
}
