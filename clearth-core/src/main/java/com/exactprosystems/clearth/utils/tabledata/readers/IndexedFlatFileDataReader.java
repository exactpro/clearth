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

import com.exactprosystems.clearth.connectivity.flat.FlatMessageDesc;
import com.exactprosystems.clearth.utils.tabledata.IndexedTableData;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

public class IndexedFlatFileDataReader<C> extends AbstractFlatFileReader<IndexedTableData<String, String, C>>
{
	protected TableRowMatcher<String, String, C> rowMatcher;
	
	public IndexedFlatFileDataReader(File file, FlatMessageDesc messageDesc, TableRowMatcher<String, String, C> rowMatcher) throws IOException
	{
		super(file, messageDesc);
		this.rowMatcher = rowMatcher;
	}
	
	public IndexedFlatFileDataReader(Reader reader, FlatMessageDesc messageDesc, TableRowMatcher<String, String, C> rowMatcher)
	{
		super(reader, messageDesc);
		this.rowMatcher = rowMatcher;
	}
	
	@Override
	protected IndexedTableData<String, String, C> createTableData(Set<String> header, RowsListFactory<String, String> rowsListFactory)
	{
		return new IndexedTableData<>(header, rowMatcher, rowsListFactory);
	}
}
