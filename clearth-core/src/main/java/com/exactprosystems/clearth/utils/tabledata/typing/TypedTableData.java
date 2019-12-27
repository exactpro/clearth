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
package com.exactprosystems.clearth.utils.tabledata.typing;

import com.exactprosystems.clearth.utils.tabledata.BasicTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TypedTableData extends BasicTableData<TypedTableHeaderItem,Object>
{
	private final List<TypedTableRow> tableRows;
	private final TypedTableHeader tableHeader;

	public TypedTableData(Set<TypedTableHeaderItem> header)
	{
		super(header);
		tableRows = new LinkedList<>();
		tableHeader = new TypedTableHeader(header);
	}

	@Override
	protected TableRow<TypedTableHeaderItem, Object> createRow(TableHeader<TypedTableHeaderItem> header)
	{
		return new TypedTableRow(new TypedTableHeader(getHeaderColumns(header)));
	}

	@Override
	public void add(TableRow row) throws IllegalArgumentException
	{
		tableRows.add((TypedTableRow) row);
	}

	@Override
	public void clear()
	{
		tableRows.clear();
	}

	@Override
	public boolean isEmpty()
	{
		return tableRows.isEmpty();
	}

	@Override
	public int size()
	{
		return tableRows.size();
	}

	public TableDataType getType(int i, String headerKey)
	{
		TypedTableHeader typedTableHeader = tableRows.get(i).getHeader();
		return typedTableHeader.getColumnType(headerKey);
	}

	public TypedTableHeader getTableHeader()
	{
		return tableHeader;
	}

	public List<TypedTableRow> getTableRows()
	{
		return tableRows;
	}

	private Set<TypedTableHeaderItem> getHeaderColumns(TableHeader<TypedTableHeaderItem> header){

		Set<TypedTableHeaderItem> itemSet = new LinkedHashSet<>();
		for (TypedTableHeaderItem item : header)
		{
			itemSet.add(item);
		}
		return itemSet;
	}
}