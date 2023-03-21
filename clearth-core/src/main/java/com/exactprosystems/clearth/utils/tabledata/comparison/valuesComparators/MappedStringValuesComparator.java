/*******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.report.results.ComparisonResult;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.DataMapping;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

public class MappedStringValuesComparator extends StringValuesComparator
{
	private static final Logger logger = LoggerFactory.getLogger(MappedStringValuesComparator.class);
	NumericStringValuesComparator numericStringValuesComparator;
	DataMapping<String> dataMapping;
	
	public MappedStringValuesComparator(ComparisonUtils comparisonUtils, DataMapping<String> dataMapping,
	                                    IValueTransformer valueTransformer)
	{
		super(comparisonUtils);
		this.dataMapping = dataMapping;
		
		Map<String, BigDecimal> numericColumns = dataMapping.getNumericColumns();
		if (!MapUtils.isEmpty(numericColumns))
			numericStringValuesComparator = new NumericStringValuesComparator(
					comparisonUtils, numericColumns, valueTransformer);
	}

	@Override
	public ComparisonResult compareValues(String expectedValue, String actualValue, String column) throws Exception
	{
		try
		{
			ComparisonResult result = dataMapping.isNumeric(column) ?
					numericStringValuesComparator.compareValues(expectedValue, actualValue, column) :
					super.compareValues(expectedValue, actualValue, column);
			return dataMapping.isInfo(column) ? infoOnFailed(result) : result;
		}
		catch (Exception e)
		{
			if (!dataMapping.isInfo(column))
				throw e;
			
			logger.warn("Error occurred during comparing values '{}' and '{}' for column '{}' - skipping it as " +
					"column is set as info", expectedValue, actualValue, column, e);
			return ComparisonResult.INFO;
		}
	}

	private ComparisonResult infoOnFailed(ComparisonResult result)
	{
		if (ComparisonResult.FAILED.equals(result))
			return ComparisonResult.INFO;
		
		return result;
	}
}