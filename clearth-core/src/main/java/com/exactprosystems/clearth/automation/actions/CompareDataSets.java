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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.utils.BigDecimalValueTransformer;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.tabledata.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonException;
import com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonProcessor;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.IndexedStringTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.StringTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.TableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories.StringTableDataReaderFactory;
import com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories.TableDataReaderFactory;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsCollectors.KeyColumnsRowsCollector;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsCollectors.StringKeyColumnsRowsCollector;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.NumericStringTableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.StringTableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.NumericStringTableRowMatcher;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.StringTableRowMatcher;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CompareDataSets extends Action
{
	public static final String KEY_COLUMNS = "KeyColumns", NUMERIC_COLUMNS = "NumericColumns", CHECK_DUPLICATES = "CheckDuplicates";
	
	protected Set<String> keyColumns = null;
	protected Map<String, BigDecimal> numericColumns = null;
	protected IValueTransformer bdValueTransformer = null;
	
	protected boolean checkDuplicates = false;
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		Map<String, String> actionParameters = getActionParameters();
		getLogger().debug("Initializing special action parameters");
		InputParamsHandler handler = new InputParamsHandler(actionParameters);
		initParameters(globalContext, handler);
		handler.check();
		
		BasicTableDataReader<String, String, ?> expectedReader = null, actualReader = null;
		try
		{
			getLogger().debug("Preparing data readers");
			TableDataReaderFactory<String, String> readerFactory = createTableDataReaderFactory();
			expectedReader = readerFactory.createTableDataReader(createTableDataReaderSettings(actionParameters, true), () -> getDbConnection(true));
			actualReader = readerFactory.createTableDataReader(createTableDataReaderSettings(actionParameters, false), () -> getDbConnection(false));
			
			return createComparisonProcessor().compareTables(createTableDataComparator(expectedReader, actualReader),
					checkDuplicates ? createKeyColumnsRowsCollector() : null);
		}
		catch (ParametersException | IOException e)
		{
			throw ResultException.failed("Error while preparing resources for making comparison.", e);
		}
		catch (ComparisonException e)
		{
			throw ResultException.failed(e.getMessage(), (Exception)e.getCause());
		}
		finally
		{
			Utils.closeResource(expectedReader);
			Utils.closeResource(actualReader);
		}
	}
	
	
	protected Map<String, String> getActionParameters()
	{
		return copyInputParams();
	}
	
	protected void initParameters(GlobalContext globalContext, InputParamsHandler handler)
	{
		keyColumns = handler.getSet(KEY_COLUMNS, ",");
		numericColumns = getNumericColumns(handler.getSet(NUMERIC_COLUMNS, ","));
		checkDuplicates = handler.getBoolean(CHECK_DUPLICATES, false);
	}
	
	protected Map<String, BigDecimal> getNumericColumns(Set<String> columnsWithScales)
	{
		if (CollectionUtils.isEmpty(columnsWithScales))
			return null;
		
		bdValueTransformer = createBigDecimalValueTransformer();
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
					throw ResultException.failed("Numeric column '" + column + "' with specified precision '"
							+ columnAndScale[1] + "' couldn't be obtained due to error.", e);
				}
			}
			numericColumns.put(column, precision);
		}
		return numericColumns;
	}
	
	protected IValueTransformer createBigDecimalValueTransformer()
	{
		return new BigDecimalValueTransformer();
	}
	
	
	protected TableDataReaderSettings createTableDataReaderSettings(Map<String, String> actionParameters,
			boolean forExpectedData) throws ParametersException
	{
		return new TableDataReaderSettings(actionParameters, forExpectedData);
	}
	
	protected TableDataReaderFactory<String, String> createTableDataReaderFactory()
	{
		return new StringTableDataReaderFactory();
	}
	
	protected Connection getDbConnection(boolean forExpectedData)
	{
		// FIXME: here we need to obtain DB connection by using Core capabilities, once this is implemented
		throw new UnsupportedOperationException("Could not initialize database connection. Please contact developers.");
	}
	
	
	protected StringTableRowMatcher createTableRowMatcher()
	{
		return MapUtils.isEmpty(numericColumns) ? new StringTableRowMatcher(keyColumns)
				:  new NumericStringTableRowMatcher(keyColumns, numericColumns, bdValueTransformer);
	}
	
	protected StringTableRowsComparator createTableRowsComparator()
	{
		ComparisonUtils comparisonUtils = ClearThCore.comparisonUtils();
		return MapUtils.isEmpty(numericColumns) ? new StringTableRowsComparator(comparisonUtils)
				: new NumericStringTableRowsComparator(comparisonUtils, numericColumns, bdValueTransformer);
	}
	
	
	protected TableDataComparator<String, String> createTableDataComparator(BasicTableDataReader<String, String, ?> expectedReader,
			BasicTableDataReader<String, String, ?> actualReader) throws IOException
	{
		StringTableRowsComparator rowsComparator = createTableRowsComparator();
		return keyColumns.isEmpty() ? new StringTableDataComparator(expectedReader, actualReader, rowsComparator)
				: new IndexedStringTableDataComparator(expectedReader, actualReader, createTableRowMatcher(), rowsComparator);
	}
	
	protected ComparisonProcessor<String, String, String> createComparisonProcessor() throws IOException
	{
		return new ComparisonProcessor<>();
	}
	
	protected KeyColumnsRowsCollector<String, String, String> createKeyColumnsRowsCollector() throws IOException
	{
		return new StringKeyColumnsRowsCollector(keyColumns);
	}
	
	
	@Override
	public void dispose()
	{
		super.dispose();
		
		keyColumns = null;
		numericColumns = null;
		bdValueTransformer = null;
	}
}
