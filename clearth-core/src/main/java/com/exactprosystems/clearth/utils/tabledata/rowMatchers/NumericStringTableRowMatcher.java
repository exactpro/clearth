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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class NumericStringTableRowMatcher extends DefaultStringTableRowMatcher
{
	protected final Map<String, Integer> numericColumns;
	
	public NumericStringTableRowMatcher(Set<String> keyColumns, Map<String, Integer> numericColumns)
	{
		super(keyColumns);
		this.numericColumns = numericColumns;
	}
	
	@Override
	public String createPrimaryKey(TableRow<String, String> row)
	{
		List<String> keyValues = new ArrayList<>();
		for (String keyColumn : keyColumns)
		{
			String value = Optional.ofNullable(row.getValue(keyColumn)).orElse("");
			if (numericColumns.containsKey(keyColumn))
			{
				try
				{
					BigDecimal bdValue = new BigDecimal(transformValue(value));
					// Apply precision (scale) if needed
					Integer precision = numericColumns.get(keyColumn);
					if (precision != null)
						bdValue = bdValue.setScale(precision, RoundingMode.HALF_UP);
					value = bdValue.toPlainString();
				}
				catch (Exception e)
				{
					// Couldn't represent such value as BigDecimal so don't modify it
				}
			}
			keyValues.add(value);
		}
		return String.join(",", keyValues);
	}
	
	protected String transformValue(String originalValue)
	{
		return originalValue.replace(',', '.');
	}
}
