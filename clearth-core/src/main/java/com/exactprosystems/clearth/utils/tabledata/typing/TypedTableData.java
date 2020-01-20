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

import java.util.Set;

import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.TableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

public class TypedTableData extends TableData<TypedTableHeaderItem,Object>
{
	public TypedTableData(Set<TypedTableHeaderItem> header)
	{
		super(header);
	}
	
	public TypedTableData(TypedTableHeader header)
	{
		super(header);
	}

	public TypedTableData(Set<TypedTableHeaderItem> header, RowsListFactory<TypedTableHeaderItem, Object> rowsListFactory) 
	{
		super(header, rowsListFactory);
	}

	public TableDataType getType(int i, String headerKey)
	{
		TypedTableHeader typedTableHeader = (TypedTableHeader) getRow(i).getHeader();
		return typedTableHeader.getColumnType(headerKey);
	}

	@Override
	protected TableHeader<TypedTableHeaderItem> createHeader(Set<TypedTableHeaderItem> header)
	{
		return new TypedTableHeader(header);
	}
	
	@Override
	protected TableRow<TypedTableHeaderItem, Object> createRow(TableHeader<TypedTableHeaderItem> header)
	{
		return new TypedTableRow((TypedTableHeader)header);
	}
}