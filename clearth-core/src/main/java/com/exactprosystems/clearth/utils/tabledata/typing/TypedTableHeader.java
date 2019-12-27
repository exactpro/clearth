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

import com.exactprosystems.clearth.utils.tabledata.TableHeader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TypedTableHeader extends TableHeader<TypedTableHeaderItem>
{
	private final Map<String,TypedTableHeaderItem> typedColumns;

	public TypedTableHeader(Set<TypedTableHeaderItem> typedColumns)
	{
		super(typedColumns);
		this.typedColumns = createHeaderMap(typedColumns);
	}

	private Map<String, TypedTableHeaderItem> createHeaderMap(Set<TypedTableHeaderItem> columnItems)
	{
		Map<String, TypedTableHeaderItem> headerMap = new LinkedHashMap<>();
		for (TypedTableHeaderItem item : columnItems)
		{
			headerMap.put(item.getName(), item);
		}
		return headerMap;
	}

	public TableDataType getColumnType(String key)
	{
		return typedColumns.get(key).getType();
	}

	public Set<String> getColumnNames()
	{
		return Collections.unmodifiableSet(typedColumns.keySet());
	}

	public int getColumnIndex(String columnName)
	{
		return super.columnIndex(typedColumns.get(columnName));
	}
}