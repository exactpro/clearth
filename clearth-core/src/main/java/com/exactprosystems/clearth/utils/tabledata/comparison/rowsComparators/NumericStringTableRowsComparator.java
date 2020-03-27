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

package com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators;

import com.exactprosystems.clearth.automation.MatrixFunctions;
import com.exactprosystems.clearth.utils.IValueTransformer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

public class NumericStringTableRowsComparator extends DefaultStringTableRowsComparator
{
	private final static Logger logger = LoggerFactory.getLogger(NumericStringTableRowsComparator.class);
	
	protected final Map<String, BigDecimal> numericColumns;
	protected final IValueTransformer bdValueTransformer;
	
	public NumericStringTableRowsComparator(Map<String, BigDecimal> numericColumns, IValueTransformer bdValueTransformer)
	{
		this.numericColumns = numericColumns;
		this.bdValueTransformer = bdValueTransformer;
	}
	
	@Override
	public boolean compareValues(String value1, String value2, String column) throws Exception
	{
		// Check if it's need to compare values as numbers or not.
		// value1 is usually expected one and could contain formula from ComparisonUtils.
		// So need to process it like not-numeric value in default way
		if (StringUtils.isNotBlank(value1) && StringUtils.isNotBlank(value2)
				&& !value1.startsWith(MatrixFunctions.FORMULA_START) && numericColumns.containsKey(column))
		{
			try
			{
				BigDecimal bdValue1 = new BigDecimal(bdValueTransformer != null ? bdValueTransformer.transform(value1) : value1),
						bdValue2 = new BigDecimal(bdValueTransformer != null ? bdValueTransformer.transform(value2) : value2),
						precision = numericColumns.get(column);
				return bdValue1.subtract(bdValue2).abs().compareTo(precision) <= 0;
			}
			catch (Exception e)
			{
				logger.trace("Couldn't present values '{}' and '{}' for numeric column '{}' as BigDecimal." +
						" They will be compared by default as strings", value1, value2, column, e);
			}
		}
		return super.compareValues(value1, value2, column);
	}
}
