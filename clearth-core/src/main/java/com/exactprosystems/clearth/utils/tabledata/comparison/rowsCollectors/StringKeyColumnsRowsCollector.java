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

package com.exactprosystems.clearth.utils.tabledata.comparison.rowsCollectors;

import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StringKeyColumnsRowsCollector extends KeyColumnsRowsCollector<String, String, String>
{
	protected static final String ROW_NAME_COLUMN = "## ROW_NAME ##";
	
	public StringKeyColumnsRowsCollector(Set<String> keyColumns) throws IOException
	{
		super(keyColumns);
	}
	
	@Override
	protected String tableRowToString(TableRow<String, String> row, String rowName)
	{
		CommaBuilder builder = new CommaBuilder(COLUMN_VALUE_DELIMITER);
		builder.append(ROW_NAME_COLUMN).add(KEY_ROW_DELIMITER).add(rowName);
		for (String column : row.getHeader())
		{
			if (keyColumns.contains(column)) // Collect only key values to reduce memory usage
				builder.append(column).add(KEY_ROW_DELIMITER).add(row.getValue(column));
		}
		return builder.toString();
	}
	
	@Override
	protected TableRow<String, String> stringToTableRow(String rowString)
	{
		Set<String> columns = new LinkedHashSet<>();
		List<String> values = new ArrayList<>();
		
		if (StringUtils.isNotBlank(rowString))
		{
			String[] columnsValuesArray = rowString.split(COLUMN_VALUE_DELIMITER);
			for (String columnValue : columnsValuesArray)
			{
				String[] columnValueArray = columnValue.split(KEY_ROW_DELIMITER, 2);
				columns.add(columnValueArray[0]);
				values.add(columnValueArray[1]);
			}
		}
		return new TableRow<>(new TableHeader<>(columns), values);
	}
	
	@Override
	protected String getCachedRowName(TableRow<String, String> cachedRow)
	{
		return cachedRow.getValue(ROW_NAME_COLUMN);
	}
	
	@Override
	protected boolean checkPrimaryKey(String fromRowToCheck, String fromCachedFile)
	{
		return StringUtils.equals(fromRowToCheck, fromCachedFile);
	}
}
