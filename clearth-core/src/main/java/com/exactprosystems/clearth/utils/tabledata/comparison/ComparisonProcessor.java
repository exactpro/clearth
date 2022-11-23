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

package com.exactprosystems.clearth.utils.tabledata.comparison;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.CloseableContainerResult;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.CsvContainerResult;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.IndexedTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.TableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonData;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonResultType;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsCollectors.KeyColumnsRowsCollector;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for making comparison of different sources with table data.
 * Uses {@link TableDataComparator} for its work.
 * @param <A> class of header members.
 * @param <B> class of values in table rows.
 * @param <C> class of primary key for indexed table data if used.
 */
public class ComparisonProcessor<A, B, C>
{
	private final static Logger logger = LoggerFactory.getLogger(ComparisonProcessor.class);
	
	// Names of results containers
	public static final String CONTAINER_PASSED = "Passed rows", CONTAINER_FAILED = "Failed rows",
			CONTAINER_NOT_FOUND = "Not found rows", CONTAINER_EXTRA = "Extra rows";
	public static final int DEFAULT_MAX_STORED_ROWS_COUNT = -1;

	private long lastAwaitedTimeout = 0;
	
	private int maxPassedRowsToStore = DEFAULT_MAX_STORED_ROWS_COUNT;
	private int maxFailedRowsToStore = DEFAULT_MAX_STORED_ROWS_COUNT;
	private int maxNotFoundRowsToStore = DEFAULT_MAX_STORED_ROWS_COUNT;
	private int maxExtraRowsToStore = DEFAULT_MAX_STORED_ROWS_COUNT;
	
	public ComparisonProcessor() {}
	
	public ComparisonProcessor(ComparisonConfiguration compConfig)
	{
		setMaxPassedRowsToStore(compConfig.getMaxPassedRowsToStore());
		setMaxFailedRowsToStore(compConfig.getMaxFailedRowsToStore());
		setMaxNotFoundRowsToStore(compConfig.getMaxNotFoundRowsToStore());
		setMaxExtraRowsToStore(compConfig.getMaxExtraRowsToStore());
	}
	/**
	 * Starts comparison process using {@link TableDataComparator} and timeout for execution.
	 * Could collect rows by key columns and find duplicates of them.
	 * @param comparator to obtain and compare next pairs of rows.
	 * @param keyColumnsRowsCollector is used only if comparator is indexed one. Helps with detecting duplicated rows by key columns.
	 * @param timeout the time after which comparison process will be suspended. Value less or equal 0 means that timeout won't be used.
	 * @return {@link Result} with all details of compared data formed in a special way.
	 * @throws ComparisonException if any kind of error occurred while making comparison.
	 */
	public Result compareTables(TableDataComparator<A, B> comparator, KeyColumnsRowsCollector<A, B, C> keyColumnsRowsCollector,
			long timeout) throws ComparisonException
	{
		TableRowMatcher<A, B, C> rowMatcher = comparator instanceof IndexedTableDataComparator ?
				((IndexedTableDataComparator<A, B, C>)comparator).getRowMatcher() : null;
		ContainerResult result = null;
		Stopwatch stopwatch = timeout > 0 ? Stopwatch.createAndStart(timeout) : null;
		
		logger.debug("Starting comparison");
		try
		{
			if (!comparator.hasMoreRows())
				return whenNothingToCompare();
			
			result = createComparisonContainerResult();
			long comparisonStartTime = System.currentTimeMillis();
			int rowsCount = 0, passedRowsCount = 0;
			do
			{
				rowsCount++;
				RowComparisonData<A, B> compData = comparator.compareRows();
				if (compData.isSuccess())
					passedRowsCount++;
				
				// Pass comparison errors to the logs if exist
				List<String> compErrors = compData.getErrors();
				if (logger.isWarnEnabled() && !compErrors.isEmpty())
				{
					LineBuilder errMsgBuilder = new LineBuilder();
					errMsgBuilder.add("Comparison error(s) at line #").add(rowsCount).append(":");
					compErrors.forEach(currentError -> errMsgBuilder.add("* ").append(currentError));
					logger.warn(errMsgBuilder.toString());
				}
				
				processCurrentRowResult(result, compData, comparator.getCurrentRow(), getRowContainerName(compData, rowsCount),
						keyColumnsRowsCollector, rowMatcher);
				afterRow(rowsCount, passedRowsCount, stopwatch);
			}
			while (comparator.hasMoreRows());
			
			logger.debug("Comparison finished in {} sec. Processed {} rows: {} passed / {} failed",
					TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - comparisonStartTime), rowsCount,
					passedRowsCount, rowsCount - passedRowsCount);
			
			return result;
		}
		catch (Exception e)
		{
			throw new ComparisonException("Couldn't process comparison of table data.", e);
		}
		finally
		{
			lastAwaitedTimeout = stopwatch != null ? stopwatch.stop() : 0;
			
			logger.debug("Closing used resources");
			if (result instanceof AutoCloseable)
				Utils.closeResource((AutoCloseable)result);
			Utils.closeResource(keyColumnsRowsCollector);
			Utils.closeResource(comparator);
		}
	}
	
	/**
	 * Starts comparison process using {@link TableDataComparator} without the time limit.
	 * Could collect rows by key columns and find duplicates of them.
	 * @param comparator to obtain and compare next pairs of rows.
	 * @param keyColumnsRowsCollector is used only if comparator is indexed one. Helps with detecting duplicated rows by key columns.
	 * @return {@link Result} with all details of compared data formed in a special way.
	 * @throws ComparisonException if any kind of error occurred while making comparison.
	 */
	public Result compareTables(TableDataComparator<A, B> comparator, KeyColumnsRowsCollector<A, B, C> keyColumnsRowsCollector)
			throws ComparisonException
	{
		return compareTables(comparator, keyColumnsRowsCollector, 0);
	}
	
	/**
	 * Starts comparison process using {@link TableDataComparator} with specified timeout.
	 * @param comparator to obtain and compare next pairs of rows.
	 * @param timeout the time after which comparison process will be suspended. Value less or equal 0 means that timeout won't be used.   
	 * @return {@link Result} with all details of compared data formed in a special way.
	 * @throws ComparisonException if any kind of error occurred while making comparison.
	 */
	public Result compareTables(TableDataComparator<A, B> comparator, long timeout) throws ComparisonException
	{
		return compareTables(comparator, null, timeout);
	}
	
	/**
	 * Starts comparison process using {@link TableDataComparator} without the time limit.
	 * @param comparator to obtain and compare next pairs of rows.
	 * @return {@link Result} with all details of compared data formed in a special way.
	 * @throws ComparisonException if any kind of error occurred while making comparison.
	 */
	public Result compareTables(TableDataComparator<A, B> comparator) throws ComparisonException
	{
		return compareTables(comparator, 0);
	}
	
	/**
	 * Returns the amount of time that processor actually waited till the end of last comparison or 0 if timeout wasn't used.
	 */
	public long getLastAwaitedTimeout()
	{
		return lastAwaitedTimeout;
	}
	
	
	protected Result whenNothingToCompare()
	{
		return DefaultResult.passed("Both datasets are empty. Nothing to compare.");
	}
	
	protected ContainerResult createComparisonContainerResult()
	{
		ContainerResult result = CloseableContainerResult.createPlainResult(null);
		// Creating containers firstly to have them in expected order
		result.addWrappedContainer(CONTAINER_PASSED, createComparisonNestedResult(CONTAINER_PASSED, getMaxPassedRowsToStore()), true);
		result.addWrappedContainer(CONTAINER_FAILED, createComparisonNestedResult(CONTAINER_FAILED, getMaxFailedRowsToStore()), true);
		result.addWrappedContainer(CONTAINER_NOT_FOUND, createComparisonNestedResult(CONTAINER_NOT_FOUND, getMaxNotFoundRowsToStore()), true);
		result.addWrappedContainer(CONTAINER_EXTRA, createComparisonNestedResult(CONTAINER_EXTRA, getMaxExtraRowsToStore()), true);
		return result;
	}
	
	protected ContainerResult createComparisonNestedResult(String header, int maxStoredRowsCount)
	{
		CsvContainerResult result = CsvContainerResult.createPlainResult(header.toLowerCase().replace(" ", "_"));
		result.setMaxStoredRowsCount(maxStoredRowsCount);
		return result;
	}
	
	
	protected void processCurrentRowResult(ContainerResult result, RowComparisonData<A, B> compData, TableRow<A, B> currentRow,
			String rowName, KeyColumnsRowsCollector<A, B, C> keyColumnsRowsCollector, TableRowMatcher<A, B, C> rowMatcher) throws IOException
	{
		if (keyColumnsRowsCollector != null && rowMatcher != null)
		{
			// We could check and store duplicated rows only if them have key columns (i.e. IndexedTableDataComparator is used)
			C primaryKey = rowMatcher.createPrimaryKey(currentRow);
			String originalRowName = keyColumnsRowsCollector.checkForDuplicatedRow(currentRow, primaryKey, rowMatcher::matchBySecondaryKey);
			if (StringUtils.isNotBlank(originalRowName))
			{
				addDuplicatedRowResult(result, compData, rowName, originalRowName);
				return;
			}
			// Add current row to collector only if it's not a duplicated one
			keyColumnsRowsCollector.addRow(currentRow, primaryKey, rowName);
		}
		addRowComparisonResult(result, createBlockResult(compData, rowName), compData.getResultType());
	}
	
	protected void addDuplicatedRowResult(ContainerResult result, RowComparisonData<A, B> compData, String rowName, String originalRowName)
	{
		Result rowResult = createBlockResult(compData, rowName + " (duplicate of row named '" + originalRowName + "')");
		rowResult.setSuccess(false);
		
		RowComparisonResultType compResultType = compData.getResultType();
		if (compResultType == RowComparisonResultType.PASSED)
			compResultType = RowComparisonResultType.FAILED;
		addRowComparisonResult(result, rowResult, compResultType);
	}
	
	protected void addRowComparisonResult(ContainerResult result, Result rowResult, RowComparisonResultType compResultType)
	{
		String nestedName = getResultTypeName(compResultType);
		ContainerResult nestedResult = result.getContainer(nestedName);
		if (nestedResult == null)
		{
			nestedResult = createComparisonNestedResult(nestedName, DEFAULT_MAX_STORED_ROWS_COUNT);
			result.addWrappedContainer(nestedName, nestedResult, true);
		}
		nestedResult.addDetail(rowResult);
	}
	
	/**
	 * Calls after each processed row while comparing. Logs progress and checks for timeout and interruption.
	 * @param rowNumber number of already processed rows.
	 * @param passedRowsCount total amount of passed rows for current moment.
	 * @param stopwatch to detect if time set for comparison is expired.
	 * @throws InterruptedException if timeout specified for comparison is expired or current thread has been interrupted.
	 */
	protected void afterRow(int rowNumber, int passedRowsCount, Stopwatch stopwatch) throws InterruptedException
	{
		if (rowNumber > 0 && (rowNumber <= 10000 && rowNumber % 1000 == 0 || rowNumber <= 100000 && rowNumber % 10000 == 0
				|| rowNumber <= 1000000 && rowNumber % 100000 == 0 || rowNumber % 1000000 == 0))
			logger.debug("Compared {} rows, {} passed", rowNumber, passedRowsCount);
		
		// Check if timeout set for comparison is expired
		if (stopwatch != null && stopwatch.isExpired())
			throw new InterruptedException("Timeout specified for comparison has expired.");
		
		// Check if comparison has been interrupted (i.e. scheduler stopped) and should be finished
		if (rowNumber % 1000 == 0 && Thread.interrupted())
			throw new InterruptedException("Comparison process has been interrupted.");
	}
	
	
	protected String getRowContainerName(RowComparisonData<A, B> compData, int rowsCount)
	{
		return "Row #" + rowsCount;
	}
	
	protected Result createBlockResult(RowComparisonData<A, B> compData, String headerMessage)
	{
		ContainerResult blockResult = ContainerResult.createBlockResult(headerMessage);
		blockResult.addDetail(compData.toDetailedResult());
		blockResult.setUseFailReasonColor(true);
		return blockResult;
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

	public int getMaxPassedRowsToStore()
	{
		return maxPassedRowsToStore;
	}

	public void setMaxPassedRowsToStore(int maxPassedRowsToStore)
	{
		this.maxPassedRowsToStore = maxPassedRowsToStore;
	}

	public int getMaxFailedRowsToStore()
	{
		return maxFailedRowsToStore;
	}

	public void setMaxFailedRowsToStore(int maxFailedRowsToStore)
	{
		this.maxFailedRowsToStore = maxFailedRowsToStore;
	}

	public int getMaxNotFoundRowsToStore()
	{
		return maxNotFoundRowsToStore;
	}

	public void setMaxNotFoundRowsToStore(int maxNotFoundRowsToStore)
	{
		this.maxNotFoundRowsToStore = maxNotFoundRowsToStore;
	}

	public int getMaxExtraRowsToStore()
	{
		return maxExtraRowsToStore;
	}

	public void setMaxExtraRowsToStore(int maxExtraRowsToStore)
	{
		this.maxExtraRowsToStore = maxExtraRowsToStore;
	}
}
