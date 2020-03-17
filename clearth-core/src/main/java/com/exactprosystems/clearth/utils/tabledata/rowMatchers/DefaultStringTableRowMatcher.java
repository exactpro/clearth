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

package com.exactprosystems.clearth.utils.tabledata.rowMatchers;

import com.exactprosystems.clearth.utils.tabledata.TableRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DefaultStringTableRowMatcher implements TableRowMatcher<String, String, String>
{
	protected final Set<String> keyColumns;
	
	public DefaultStringTableRowMatcher(Set<String> keyColumns)
	{
		this.keyColumns = keyColumns;
	}
	
	@Override
	public String createPrimaryKey(TableRow<String, String> row)
	{
		List<String> keyValues = new ArrayList<>();
		for (String keyColumn : keyColumns)
		{
			String value = row.getValue(keyColumn);
			if (value != null)
				keyValues.add(value);
		}
		return String.join(",", keyValues);
	}

	@Override
	public boolean matchBySecondaryKey(TableRow<String, String> row1, TableRow<String, String> row2)
	{
		// Return true as we should compare non-key values by another way
		return true;
	}
}
