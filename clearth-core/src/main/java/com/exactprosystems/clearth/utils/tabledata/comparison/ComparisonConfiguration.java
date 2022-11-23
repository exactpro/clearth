/******************************************************************************
 * Copyright 2009-2021 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils.tabledata.comparison;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.inputparams.ParametersHandler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonProcessor.DEFAULT_MAX_STORED_ROWS_COUNT;

public class ComparisonConfiguration
{
	private static final String MAX_ROWS_TO_STORE_TEMPLATE = "Max%sRowsInReport";
	public static final String KEY_COLUMNS = "KeyColumns",
			NUMERIC_COLUMNS = "NumericColumns",
			CHECK_DUPLICATES = "CheckDuplicates",
			MAX_PASSED_ROWS_TO_STORE = String.format(MAX_ROWS_TO_STORE_TEMPLATE, "Passed"),
			MAX_FAILED_ROWS_TO_STORE = String.format(MAX_ROWS_TO_STORE_TEMPLATE, "Failed"),
			MAX_NOT_FOUND_ROWS_TO_STORE = String.format(MAX_ROWS_TO_STORE_TEMPLATE, "NotFound"),
			MAX_EXTRA_ROWS_TO_STORE = String.format(MAX_ROWS_TO_STORE_TEMPLATE, "Extra");
	
	protected final IValueTransformer bdValueTransformer;
	
	protected Set<String> keyColumns;
	protected Map<String, BigDecimal> numericColumns;
	protected boolean checkDuplicates;
	
	protected int maxPassedRowsToStore;
	protected int maxFailedRowsToStore;
	protected int maxNotFoundRowsToStore;
	protected int maxExtraRowsToStore;
	
	public ComparisonConfiguration(Map<String, String> parameters, IValueTransformer bdValueTransformer) throws ParametersException
	{
		this.bdValueTransformer = bdValueTransformer;
		
		ParametersHandler handler = new ParametersHandler(parameters);
		loadConfiguration(handler);
		handler.check();
	}
	
	protected void loadConfiguration(ParametersHandler handler) throws ParametersException
	{
		keyColumns = handler.getSet(KEY_COLUMNS, ",");
		numericColumns = getNumericColumns(handler.getSet(NUMERIC_COLUMNS, ","));
		checkDuplicates = handler.getBoolean(CHECK_DUPLICATES, false);
		maxPassedRowsToStore = handler.getInteger(MAX_PASSED_ROWS_TO_STORE, DEFAULT_MAX_STORED_ROWS_COUNT);
		maxFailedRowsToStore = handler.getInteger(MAX_FAILED_ROWS_TO_STORE, DEFAULT_MAX_STORED_ROWS_COUNT);
		maxNotFoundRowsToStore = handler.getInteger(MAX_NOT_FOUND_ROWS_TO_STORE, DEFAULT_MAX_STORED_ROWS_COUNT);
		maxExtraRowsToStore = handler.getInteger(MAX_EXTRA_ROWS_TO_STORE, DEFAULT_MAX_STORED_ROWS_COUNT);
	}
	
	protected Map<String, BigDecimal> getNumericColumns(Set<String> columnsWithScales) throws ParametersException
	{
		Map<String, BigDecimal> numericColumns = new HashMap<>();
		for (String columnWithScale : columnsWithScales)
		{
			String[] columnAndScale = columnWithScale.split(":", 2);
			String column = columnAndScale[0];
			BigDecimal precision = BigDecimal.ZERO;
			if (columnAndScale.length == 2)
			{
				try
				{
					precision = new BigDecimal(bdValueTransformer != null ?
							bdValueTransformer.transform(columnAndScale[1]) : columnAndScale[1]);
				}
				catch (Exception e)
				{
					throw new ParametersException("Numeric column '" + column + "' with specified precision '"
							+ columnAndScale[1] + "' couldn't be obtained due to error.", e);
				}
			}
			numericColumns.put(column, precision);
		}
		return numericColumns;
	}
	
	
	public Set<String> getKeyColumns()
	{
		return keyColumns;
	}
	
	public Map<String, BigDecimal> getNumericColumns()
	{
		return numericColumns;
	}
	
	public boolean isCheckDuplicates()
	{
		return checkDuplicates;
	}

	public int getMaxPassedRowsToStore()
	{
		return maxPassedRowsToStore;
	}

	public int getMaxFailedRowsToStore()
	{
		return maxFailedRowsToStore;
	}

	public int getMaxNotFoundRowsToStore()
	{
		return maxNotFoundRowsToStore;
	}

	public int getMaxExtraRowsToStore()
	{
		return maxExtraRowsToStore;
	}
}
