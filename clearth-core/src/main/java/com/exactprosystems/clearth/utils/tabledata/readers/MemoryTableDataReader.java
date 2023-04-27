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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.TableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Reader class for in-memory table data
 */
public class MemoryTableDataReader<A, B> extends AbstractMemoryTableDataReader<A, B, TableData<A, B>>
{
	public MemoryTableDataReader(Collection<TableRow<A, B>> data) throws IOException
	{
		super(data);
	}
	
	public MemoryTableDataReader(TableData<A, B> data) throws IOException
	{
		super(data);
	}
	
	@Override
	protected TableData<A, B> createTableData(TableHeader<A> header, RowsListFactory<A, B> rowsListFactory)
	{
		return new TableData(header, rowsListFactory);
	}
}
