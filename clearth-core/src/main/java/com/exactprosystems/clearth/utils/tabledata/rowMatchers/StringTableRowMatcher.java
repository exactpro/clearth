/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils.tabledata.rowMatchers;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.primarykeys.CollectionPrimaryKey;

import java.util.*;


public class StringTableRowMatcher implements TableRowMatcher<String, String, CollectionPrimaryKey<String>>
{
	protected final Set<String> keyColumns;
	
	protected final Set<TableHeader<String>> checkedHeadersCache = new HashSet<>();

	public StringTableRowMatcher(Set<String> keyColumns)
	{
		this.keyColumns = new LinkedHashSet<>(keyColumns);
	}

	@Override
	public CollectionPrimaryKey<String> createPrimaryKey(TableRow<String, String> row)
	{
		if (row == null)
			throw new IllegalArgumentException("Row must be not null");

		try
		{
			checkHeader(row.getHeader());
		}
		catch (ParametersException e)
		{
			throw new IllegalArgumentException("Invalid row to create primary key", e);
		}

		List<String> keyValues = new ArrayList<>();
		for (String keyColumn : keyColumns)
			addColumnValue(keyValues, row, keyColumn);
		
		return buildKey(keyValues);
	}
	
	@Override
	public CollectionPrimaryKey<String> createPrimaryKey(Collection<String> rowValues)
	{
		if (rowValues.size() != keyColumns.size())
			throw new IllegalStateException("Number of values ("+rowValues.size()+") doesn't match number of key columns ("+keyColumns.size()+")");

		return buildKey(rowValues);
	}


	protected void addColumnValue(List<String> keyValues, TableRow<String, String> row, String keyColumn)
	{
		String value = getValue(row, keyColumn);
		keyValues.add(value);
	}

	protected String getValue(TableRow<String, String> row, String column)
	{
		return row.getValue(column);
	}

	protected CollectionPrimaryKey<String> buildKey(Collection<String> keyValues)
	{
		return new CollectionPrimaryKey<>(new ArrayList<>(keyValues));
	}

	@Override
	public boolean matchBySecondaryKey(TableRow<String, String> row1, TableRow<String, String> row2)
	{
		// Return true as we should compare non-key values by another way
		return true;
	}

	@Override
	public void checkHeader(TableHeader<String> header)
			throws ParametersException
	{
		if (checkedHeadersCache.contains(header))
			return;
		
		List<String> errorColumns = null;
		for (String keyColumn : keyColumns)
		{
			if (!header.containsColumn(keyColumn))
			{
				if (errorColumns == null)
					errorColumns = new ArrayList<>();
				errorColumns.add(keyColumn);
			}
		}

		if (errorColumns != null)
			throw new ParametersException("Primary key columns are missing in header: " + errorColumns);
		
		checkedHeadersCache.add(header);
	}
}
