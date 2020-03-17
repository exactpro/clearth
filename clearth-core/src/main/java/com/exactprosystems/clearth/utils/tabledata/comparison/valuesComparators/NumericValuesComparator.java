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

package com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators;

import com.exactprosystems.clearth.automation.MatrixFunctions;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class NumericValuesComparator extends StringValuesComparator
{
	private final static Logger logger = LoggerFactory.getLogger(NumericValuesComparator.class);
	
	protected final Map<String, Integer> columnsWithScales;
	
	public NumericValuesComparator(Map<String, Integer> columnsWithScales)
	{
		this.columnsWithScales = columnsWithScales;
	}
	
	@Override
	public boolean compareValues(String value1, String value2, String column) throws Exception
	{
		// Check if it's need to compare values as numbers or not.
		// value1 is usually expected one and could contain formula from ComparisonUtils.
		// So need to process it like not-numeric value in default way
		if (StringUtils.isNotBlank(value1) && StringUtils.isNotBlank(value2)
				&& !value1.startsWith(MatrixFunctions.FORMULA_START) && columnsWithScales.containsKey(column))
		{
			try
			{
				BigDecimal bdValue1 = new BigDecimal(transformValue(value1)),
						bdValue2 = new BigDecimal(transformValue(value2));
				// Apply scales for values if exist
				Integer scale = columnsWithScales.get(column);
				if (scale != null)
				{
					bdValue1 = bdValue1.setScale(scale, RoundingMode.HALF_UP);
					bdValue2 = bdValue2.setScale(scale, RoundingMode.HALF_UP);
				}
				return bdValue1.compareTo(bdValue2) == 0;
			}
			catch (Exception e)
			{
				getLogger().trace("Couldn't present values '{}' and '{}' as BigDecimal." +
						" They will be compared by default as strings", value1, value2, e);
			}
		}
		return super.compareValues(value1, value2, column);
	}
	
	protected String transformValue(String originalValue)
	{
		return originalValue.replace(',', '.');
	}
	
	protected Logger getLogger()
	{
		return logger;
	}
}
