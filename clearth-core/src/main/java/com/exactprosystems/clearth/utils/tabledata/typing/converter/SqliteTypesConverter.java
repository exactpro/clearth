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

package com.exactprosystems.clearth.utils.tabledata.typing.converter;

import com.exactprosystems.clearth.utils.tabledata.typing.TableDataType;

import static java.sql.Types.*;

public class SqliteTypesConverter implements DbTypesConverter
{
	@Override
	public TableDataType getType(int dbTypeIndex)
	{
		switch (dbTypeIndex)
		{
			case INTEGER:
			case BIGINT:
			case TINYINT:
			case SMALLINT:
				return TableDataType.INTEGER;
			case DATE:
				return TableDataType.LOCALDATETIME;
			case NUMERIC:	
			case CHAR:
			case VARCHAR:
			case LONGVARCHAR:
			case NCHAR:
			case NVARCHAR:
				return TableDataType.STRING;
			case FLOAT:	
				return TableDataType.FLOAT;
			case REAL:
			case DOUBLE:
				return TableDataType.DOUBLE;
		}
		return TableDataType.OBJECT;
	}
}
