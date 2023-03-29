/******************************************************************************
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
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.BigDecimalValueTransformer;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.TableDataException;
import com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonConfiguration;
import com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonException;
import com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonProcessor;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.IndexedStringTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.StringTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.TableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.DataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories.StringTableDataReaderFactory;
import com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories.TableDataReaderFactory;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsCollectors.KeyColumnsRowsCollector;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsCollectors.StringKeyColumnsRowsCollector;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.MappedTableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.TableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.MappedStringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.NumericStringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.StringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.MappedTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.NumericStringTableRowMatcher;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.StringTableRowMatcher;

import java.io.IOException;
import java.util.Map;

public class CompareDataSets extends Action
{
	protected IValueTransformer bdValueTransformer = null;
	protected ComparisonConfiguration compConfig;
	protected TableDataReaderFactory<String, String> readerFactory;

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		BasicTableDataReader<String, String, ?> expectedReader = null, actualReader = null;
		try
		{
			getLogger().debug("Initializing comparison configuration");
			Map<String, String> actionParameters = getActionParameters();
			bdValueTransformer = createBigDecimalValueTransformer();
			compConfig = createComparisonConfiguration(actionParameters);

			getLogger().debug("Preparing data readers");
			readerFactory = createTableDataReaderFactory();
			expectedReader = createTableDataReader(actionParameters, true, globalContext);
			actualReader = createTableDataReader(actionParameters, false, globalContext);

			return makeComparison(expectedReader, actualReader);
		}
		catch (ParametersException | IOException e)
		{
			return DefaultResult.failed("Error while preparing resources for making comparison.", e);
		}
		catch (TableDataException | ComparisonException e)
		{
			return DefaultResult.failed(e.getMessage(), (Exception)e.getCause());
		}
		finally
		{
			Utils.closeResource(expectedReader);
			Utils.closeResource(actualReader);
		}
	}

	private BasicTableDataReader<String, String,?> createTableDataReader(Map<String, String> actionParameters,
					boolean forExpected, GlobalContext globalContext) throws ParametersException, TableDataException
	{
		BasicTableDataReader<String, String, ?> tableDataReader = readerFactory.createTableDataReader(
				createTableDataReaderSettings(actionParameters, forExpected, globalContext));

		DataMapping<String> dataMapping = compConfig.getDataMapping();
		if (dataMapping != null)
			tableDataReader = new MappedTableDataReader<>(tableDataReader, dataMapping.getHeaderMapper(forExpected));

		return tableDataReader;
	}

	protected Map<String, String> getActionParameters()
	{
		return copyInputParams();
	}
	
	protected IValueTransformer createBigDecimalValueTransformer()
	{
		return new BigDecimalValueTransformer();
	}
	
	protected ComparisonConfiguration createComparisonConfiguration(Map<String, String> actionParameters)
			throws ParametersException
	{
		return new ComparisonConfiguration(actionParameters, bdValueTransformer);
	}

	protected TableDataReaderSettings createTableDataReaderSettings(Map<String, String> actionParameters,
			boolean forExpectedData, GlobalContext globalContext) throws ParametersException
	{
		return new TableDataReaderSettings(actionParameters, forExpectedData, globalContext::getDbConnection);
	}
	
	protected TableDataReaderFactory<String, String> createTableDataReaderFactory()
	{
		return new StringTableDataReaderFactory();
	}
	
	
	protected StringTableRowMatcher createTableRowMatcher()
	{
		return compConfig.getNumericColumns().isEmpty() ? new StringTableRowMatcher(compConfig.getKeyColumns())
				:  new NumericStringTableRowMatcher(compConfig.getKeyColumns(), compConfig.getNumericColumns(), bdValueTransformer);
	}
	
	protected TableRowsComparator<String, String> createTableRowsComparator()
	{
		if (compConfig.getDataMapping() != null)
			return new MappedTableRowsComparator<>(createValuesComparator(), compConfig.getDataMapping());

		return new TableRowsComparator<>(createValuesComparator());
	}

	protected ValuesComparator<String, String> createValuesComparator()
	{
		ComparisonUtils comparisonUtils = ClearThCore.comparisonUtils();

		if (compConfig.getDataMapping() != null)
			return new MappedStringValuesComparator(comparisonUtils, compConfig.getDataMapping(), bdValueTransformer);

		if (!compConfig.getNumericColumns().isEmpty())
			return new NumericStringValuesComparator(comparisonUtils, compConfig.getNumericColumns(), bdValueTransformer);

		 return new StringValuesComparator(comparisonUtils);
	}
	
	
	protected TableDataComparator<String, String> createTableDataComparator(BasicTableDataReader<String, String, ?> expectedReader,
			BasicTableDataReader<String, String, ?> actualReader) throws IOException, ParametersException
	{
		TableRowsComparator<String, String> rowsComparator = createTableRowsComparator();
		return compConfig.getKeyColumns().isEmpty() ? new StringTableDataComparator(expectedReader, actualReader, rowsComparator)
				: new IndexedStringTableDataComparator(expectedReader, actualReader, createTableRowMatcher(), rowsComparator);
	}
	
	protected ComparisonProcessor<String, String, String> createComparisonProcessor()
	{
		return new ComparisonProcessor<>(compConfig);
	}
	
	protected KeyColumnsRowsCollector<String, String, String> createKeyColumnsRowsCollector() throws IOException
	{
		return new StringKeyColumnsRowsCollector(compConfig.getKeyColumns());
	}
	
	protected Result makeComparison(BasicTableDataReader<String, String, ?> expectedReader,
			BasicTableDataReader<String, String, ?> actualReader) throws ComparisonException, IOException, ParametersException
	{
		return createComparisonProcessor().compareTables(createTableDataComparator(expectedReader, actualReader),
				compConfig.isCheckDuplicates() ? createKeyColumnsRowsCollector() : null);
	}
	
	
	@Override
	public void dispose()
	{
		super.dispose();
		
		bdValueTransformer = null;
		compConfig = null;
	}
}
