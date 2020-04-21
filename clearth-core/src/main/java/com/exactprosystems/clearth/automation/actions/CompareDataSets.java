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

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.CloseableContainerResult;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.CsvContainerResult;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.BigDecimalValueTransformer;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.tabledata.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.KeyColumnsRowsCollector;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.IndexedStringTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.StringTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.TableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories.StringTableDataReaderFactory;
import com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories.TableDataReaderFactory;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonData;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonResultType;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.DefaultStringTableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.NumericStringTableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.DefaultStringTableRowMatcher;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.NumericStringTableRowMatcher;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class CompareDataSets extends Action
{
	public static final String KEY_COLUMNS = "KeyColumns", NUMERIC_COLUMNS = "NumericColumns", CHECK_DUPLICATES = "CheckDuplicates";
	
	// Names for results' containers
	public static final String CONTAINER_PASSED = "Passed rows",
			CONTAINER_FAILED = "Failed rows",
			CONTAINER_NOT_FOUND = "Not found rows",
			CONTAINER_EXTRA = "Extra rows";
	
	protected Set<String> keyColumns = null;
	protected Map<String, BigDecimal> numericColumns = null;
	protected IValueTransformer bdValueTransformer = null;
	
	protected boolean checkDuplicates = false;
	protected KeyColumnsRowsCollector keyColumnsRowsCollector = null;
	
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		getLogger().debug("Initializing special action parameters");
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		initParameters(globalContext, handler);
		handler.check();
		
		BasicTableDataReader<String, String, ?> expectedReader = null, actualReader = null;
		try
		{
			getLogger().debug("Preparing data readers");
			TableDataReaderFactory<String, String> readerFactory = createTableDataReaderFactory();
			expectedReader = readerFactory.createTableDataReader(createTableDataReaderSettings(true), () -> getDbConnection(true));
			actualReader = readerFactory.createTableDataReader(createTableDataReaderSettings(false), () -> getDbConnection(false));
			
			getLogger().debug("Data readers are ready. Starting comparison");
			return compareTables(expectedReader, actualReader);
		}
		catch (Exception e)
		{
			if (e instanceof ResultException)
				throw (ResultException)e;
			throw ResultException.failed("Error while comparing data.", e);
		}
		finally
		{
			getLogger().debug("Closing used resources");
			// Readers may be not closed by comparator (e.g. due to some exception)
			Utils.closeResource(expectedReader);
			Utils.closeResource(actualReader);
		}
	}
	
	protected void initParameters(GlobalContext globalContext, InputParamsHandler handler)
	{
		keyColumns = handler.getSet(KEY_COLUMNS, ",");
		numericColumns = getNumericColumns(handler.getSet(NUMERIC_COLUMNS, ","));
		checkDuplicates = handler.getBoolean(CHECK_DUPLICATES, false);
	}
	
	
	protected Result compareTables(BasicTableDataReader<String, String, ?> expectedReader,
			BasicTableDataReader<String, String, ?> actualReader) throws Exception
	{
		ContainerResult result = null;
		DefaultStringTableRowMatcher rowMatcher = null;
		if (!keyColumns.isEmpty())
		{
			rowMatcher = createTableRowMatcher(keyColumns);
			if (checkDuplicates)
				keyColumnsRowsCollector = createKeyColumnsRowsCollector(keyColumns);
		}
		
		try (TableDataComparator<String, String> comparator = createTableDataComparator(expectedReader, actualReader,
				rowMatcher, createTableRowsComparator()))
		{
			if (!comparator.hasMoreRows())
				return DefaultResult.passed("Both datasets are empty. Nothing to compare.");
			
			long comparisonStartTime = System.currentTimeMillis();
			result = createComparisonContainerResult();
			int rowsCount = 0, passedRowsCount = 0;
			do
			{
				rowsCount++;
				RowComparisonData<String, String> compData = comparator.compareRows();
				if (compData.isSuccess())
					passedRowsCount++;
				
				// Pass comparison errors to the logs if exist
				List<String> compErrors = compData.getErrors();
				if (getLogger().isWarnEnabled() && !compErrors.isEmpty())
				{
					LineBuilder errMsgBuilder = new LineBuilder();
					errMsgBuilder.add("Comparison error(s) at line #").add(rowsCount).append(":");
					compErrors.forEach(currentError -> errMsgBuilder.add("* ").append(currentError));
					getLogger().warn(errMsgBuilder.toString());
				}
				
				processCurrentRowResult(compData, getRowContainerName(compData, rowsCount), result, keyColumnsRowsCollector,
						comparator.getCurrentRow(), rowMatcher);
				afterRow(rowsCount, passedRowsCount);
			}
			while (comparator.hasMoreRows());
			
			getLogger().debug("Comparison finished in {} sec. Processed {} rows: {} passed / {} failed",
					TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - comparisonStartTime), rowsCount,
					passedRowsCount, rowsCount - passedRowsCount);
			
			return result;
		}
		finally
		{
			if (result instanceof AutoCloseable)
				Utils.closeResource((AutoCloseable)result);
			Utils.closeResource(keyColumnsRowsCollector);
		}
	}
	
	protected ContainerResult createComparisonContainerResult()
	{
		ContainerResult result = CloseableContainerResult.createPlainResult(null);
		// Creating containers firstly to have them in expected order
		result.addWrappedContainer(CONTAINER_PASSED, createComparisonNestedResult(CONTAINER_PASSED), true);
		result.addWrappedContainer(CONTAINER_FAILED, createComparisonNestedResult(CONTAINER_FAILED), true);
		result.addWrappedContainer(CONTAINER_NOT_FOUND, createComparisonNestedResult(CONTAINER_NOT_FOUND), true);
		result.addWrappedContainer(CONTAINER_EXTRA, createComparisonNestedResult(CONTAINER_EXTRA), true);
		return result;
	}
	
	protected ContainerResult createComparisonNestedResult(String header)
	{
		return CsvContainerResult.createPlainResult(header.toLowerCase().replace(" ", "_"));
	}
	
	protected TableDataComparator<String, String> createTableDataComparator(BasicTableDataReader<String, String, ?> expectedReader,
			BasicTableDataReader<String, String, ?> actualReader, DefaultStringTableRowMatcher rowMatcher,
			DefaultStringTableRowsComparator rowsComparator) throws IOException
	{
		return rowMatcher == null ? new StringTableDataComparator(expectedReader, actualReader, rowsComparator)
				: new IndexedStringTableDataComparator(expectedReader, actualReader, rowMatcher, rowsComparator);
	}
	
	protected void processCurrentRowResult(RowComparisonData<String, String> compData, String rowName, ContainerResult result,
			KeyColumnsRowsCollector keyColumnsRowsCollector, TableRow<String, String> currentRow,
			DefaultStringTableRowMatcher rowMatcher) throws IOException
	{
		if (keyColumnsRowsCollector != null && rowMatcher != null)
		{
			String primaryKey = rowMatcher.createPrimaryKey(currentRow),
					originalRowName = keyColumnsRowsCollector.checkForDuplicatedRow(primaryKey, currentRow, rowMatcher::matchBySecondaryKey);
			if (StringUtils.isNotBlank(originalRowName))
			{
				addDuplicatedRowResult(compData, rowName, originalRowName, result);
				return;
			}
			// Add current row to collector only if it's not a duplicated one
			keyColumnsRowsCollector.addRow(rowName, primaryKey, currentRow);
		}
		addRowComparisonResult(createBlockResult(compData, rowName), result, compData.getResultType());
	}
	
	protected void addDuplicatedRowResult(RowComparisonData<String, String> compData, String rowName, String originalRowName,
			ContainerResult result)
	{
		Result rowResult = createBlockResult(compData, rowName + " (duplicate of row named '" + originalRowName + "')");
		rowResult.setSuccess(false);
		
		RowComparisonResultType compResultType = compData.getResultType();
		if (compResultType == RowComparisonResultType.PASSED)
			compResultType = RowComparisonResultType.FAILED;
		addRowComparisonResult(rowResult, result, compResultType);
	}
	
	protected void addRowComparisonResult(Result rowResult, ContainerResult result, RowComparisonResultType compResultType)
	{
		String nestedName = getResultTypeName(compResultType);
		ContainerResult nestedResult = result.getContainer(nestedName);
		if (nestedResult == null)
		{
			nestedResult = createComparisonNestedResult(nestedName);
			result.addWrappedContainer(nestedName, nestedResult, true);
		}
		nestedResult.addDetail(rowResult);
	}
	
	protected Result createBlockResult(RowComparisonData<String, String> compData, String headerMessage)
	{
		ContainerResult blockResult = ContainerResult.createBlockResult(headerMessage);
		blockResult.addDetail(compData.toDetailedResult());
		blockResult.setUseFailReasonColor(true);
		return blockResult;
	}
	
	protected String getRowContainerName(RowComparisonData<String, String> compData, int rowsCount)
	{
		return "Row #" + rowsCount;
	}
	
	protected void afterRow(int rowNumber, int passedRowsCount)
	{
		if (rowNumber > 0 && (rowNumber <= 10000 && rowNumber % 1000 == 0 || rowNumber <= 100000 && rowNumber % 10000 == 0
				|| rowNumber <= 1000000 && rowNumber % 100000 == 0 || rowNumber % 1000000 == 0))
			getLogger().debug("Compared {} rows, {} passed", rowNumber, passedRowsCount);
		
		// Check if action has been interrupted (i.e. scheduler stopped) and should be finished
		if (rowNumber % 1000 == 0 && Thread.currentThread().isInterrupted())
			throw ResultException.failed("Action execution has been interrupted.");
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
	
	protected TableDataReaderSettings createTableDataReaderSettings(boolean forExpectedData) throws ParametersException
	{
		return new TableDataReaderSettings(inputParams, forExpectedData);
	}
	
	protected TableDataReaderFactory<String, String> createTableDataReaderFactory()
	{
		return new StringTableDataReaderFactory();
	}
	
	protected DefaultStringTableRowMatcher createTableRowMatcher(Set<String> keyColumns)
	{
		return MapUtils.isEmpty(numericColumns) ? new DefaultStringTableRowMatcher(keyColumns)
				:  new NumericStringTableRowMatcher(keyColumns, numericColumns, bdValueTransformer);
	}
	
	protected DefaultStringTableRowsComparator createTableRowsComparator()
	{
		return MapUtils.isEmpty(numericColumns) ? new DefaultStringTableRowsComparator()
				: new NumericStringTableRowsComparator(numericColumns, bdValueTransformer);
	}
	
	protected IValueTransformer createBigDecimalValueTransformer()
	{
		return new BigDecimalValueTransformer();
	}
	
	protected Connection getDbConnection(boolean forExpectedData)
	{
		// FIXME: here we need to obtain DB connection by using Core capabilities, once this is implemented
		throw ResultException.failed("Could not initialize database connection. Please contact developers.");
	}
	
	protected KeyColumnsRowsCollector createKeyColumnsRowsCollector(Set<String> keyColumns) throws IOException
	{
		return new KeyColumnsRowsCollector(keyColumns);
	}
	
	
	protected String getResultTypeName(RowComparisonResultType result)
	{
		switch (result)
		{
			case PASSED:
				return CONTAINER_PASSED;
			case FAILED:
				return CONTAINER_FAILED;
			case NOT_FOUND:
				return CONTAINER_NOT_FOUND;
			case EXTRA:
				return CONTAINER_EXTRA;
			default:
				return null;
		}
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
		
		keyColumns = null;
		numericColumns = null;
		bdValueTransformer = null;
		keyColumnsRowsCollector = null;
	}
}
