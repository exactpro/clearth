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

import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NumericStringTableRowMatcher extends DefaultStringTableRowMatcher
{
	private static final Logger logger = LoggerFactory.getLogger(NumericStringTableRowMatcher.class);
	
	protected final Map<String, BigDecimal> numericKeyColumns;
	protected final IValueTransformer bdValueTransformer;
	
	public NumericStringTableRowMatcher(Set<String> keyColumns, Map<String, BigDecimal> numericColumns,
			IValueTransformer bdValueTransformer)
	{
		super(keyColumns);
		this.bdValueTransformer = bdValueTransformer;
		
		// Separate simple key columns from numeric ones
		numericKeyColumns = new HashMap<>();
		for (Iterator<String> keyColumnIter = super.keyColumns.iterator(); keyColumnIter.hasNext(); )
		{
			String column = keyColumnIter.next();
			if (numericColumns.containsKey(column))
			{
				numericKeyColumns.put(column, numericColumns.get(column));
				keyColumnIter.remove();
			}
		}
	}
	
	@Override
	public boolean matchBySecondaryKey(TableRow<String, String> row1, TableRow<String, String> row2)
	{
		for (String numericKeyColumn : numericKeyColumns.keySet())
		{
			String value1 = row1.getValue(numericKeyColumn), value2 = row2.getValue(numericKeyColumn);
			try
			{
				BigDecimal bdValue1 = new BigDecimal(bdValueTransformer != null ? bdValueTransformer.transform(value1) : value1),
						bdValue2 = new BigDecimal(bdValueTransformer != null ? bdValueTransformer.transform(value2) : value2),
						precision = numericKeyColumns.get(numericKeyColumn);
				if (bdValue1.subtract(bdValue2).abs().compareTo(precision) > 0)
					return false;
			}
			catch (Exception e)
			{
				logger.trace("Couldn't present values '{}' and '{}' for numeric key column '{}' as BigDecimal." +
						" They will be compared by default as strings", value1, value2, numericKeyColumn, e);
				if (!StringUtils.equals(value1, value2))
					return false;
			}
		}
		return true;
	}
}
