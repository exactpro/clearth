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
import java.util.*;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;

public class ClearThMessageReader extends AbstractClearThMessageReader<StringTableData>
{
	public ClearThMessageReader(ClearThMessage<?> message, 
	                            SubMessageFilter subMessageFilter, 
	                            TableRowFilter<String, String> rowFilter)
	{
		super(message, subMessageFilter, rowFilter);
	}

	public ClearThMessageReader(ClearThMessage<?> message, SubMessageFilter subMessageFilter)
	{
		super(message, subMessageFilter);
	}

	public ClearThMessageReader(ClearThMessage<?> message, TableRowFilter<String, String> rowFilter)
	{
		super(message, rowFilter);
	}

	public ClearThMessageReader(ClearThMessage<?> message)
	{
		super(message);
	}
	

	public static StringTableData read(ClearThMessage<?> message) throws IOException
	{
		return read(message, null, null);
	}

	public static StringTableData read(ClearThMessage<?> message, SubMessageFilter subMessageFilter) throws IOException
	{
		return read(message, subMessageFilter, null);
	}

	public static StringTableData read(ClearThMessage<?> message, TableRowFilter<String, String> rowFilter) throws IOException
	{
		return read(message, null, rowFilter);
	}

	public static StringTableData read(ClearThMessage<?> message,
	                                   SubMessageFilter subMessageFilter,
	                                   TableRowFilter<String, String> rowFilter) throws IOException
	{
		try (ClearThMessageReader reader = new ClearThMessageReader(message, subMessageFilter, rowFilter))
		{
			return reader.readAllData();
		}
	}
	

	@Override
	protected StringTableData createTableData(Set<String> header, RowsListFactory<String, String> rowsListFactory)
	{
		return new StringTableData(header, rowsListFactory);
	}
}
