/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

public final class DefaultCreateTableQueryGenerator implements CreateTableQueryGenerator
{
	private final SqlSyntax sqlSyntax;

	public DefaultCreateTableQueryGenerator(SqlSyntax sqlSyntax)
	{
		this.sqlSyntax = sqlSyntax;
	}

	@Override
	public String generateQuery(TableHeader<TypedTableHeaderItem> header, String tableName)
	{
		StringBuilder fields = new StringBuilder();

		for (TypedTableHeaderItem item : header)
		{
			if (fields.length() != 0)
				fields.append(", ");
			String fieldName = item.getName();
			if (!sqlSyntax.isValidFieldName(fieldName))
				fieldName = sqlSyntax.normalizeFieldName(fieldName);
			
			fields.append(fieldName).append(" ").append(sqlSyntax.getDbType(item.getType()));
		}
		
		return String.format("CREATE TABLE %s (%s)", tableName, fields);
	}
}
