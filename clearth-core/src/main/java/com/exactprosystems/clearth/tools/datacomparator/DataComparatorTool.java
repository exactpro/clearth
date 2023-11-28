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

package com.exactprosystems.clearth.tools.datacomparator;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.report.comparisonwriters.CsvComparisonWriter;
import com.exactprosystems.clearth.utils.BigDecimalValueTransformer;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.comparison.ComparisonException;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.IndexedStringTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.StringTableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.dataComparators.TableDataComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.mappings.DataMapping;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonData;
import com.exactprosystems.clearth.utils.tabledata.comparison.result.RowComparisonResultType;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.MappedTableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.rowsComparators.TableRowsComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.MappedStringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.StringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.primarykeys.CollectionPrimaryKey;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.NumericStringTableRowMatcher;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.StringTableRowMatcher;
import com.exactprosystems.clearth.utils.tabledata.rowMatchers.TableRowMatcher;

public class DataComparatorTool
{
	private final AtomicBoolean interrupted = new AtomicBoolean(false);
	private final String eol = System.lineSeparator();
	
	public ComparisonResult compare(BasicTableDataReader<String, String, ?> expectedReader, BasicTableDataReader<String, String, ?> actualReader, 
			ComparisonSettings settings) throws IOException, ParametersException, ComparisonException
	{
		interrupted.set(false);
		
		IValueTransformer valueTransformer = createValueTransformer(settings);
		ValuesComparator<String, String> valuesComp = createValuesComparator(settings, valueTransformer);
		TableRowsComparator<String, String> rowsComp = createRowsComparator(settings, valuesComp);
		TableDataComparator<String, String> tableComp = createTableDataComparator(expectedReader, actualReader, settings, rowsComp, valueTransformer);
		
		if (!tableComp.hasMoreRows())
			return nothingToCompare();
		
		ComparisonResult result = new ComparisonResult();
		Path errorsFile = null;
		Writer errorsWriter = null;
		Map<RowComparisonResultType, CsvComparisonWriter> detailWriters = new HashMap<>();
		try
		{
			Files.createDirectories(settings.getOutputDir());
			result.setStarted(LocalDateTime.now());
			do
			{
				result.incTotal();
				
				RowComparisonData<String, String> compData = tableComp.compareRows();
				
				List<String> compErrors = compData.getErrors();
				if (!compErrors.isEmpty())
				{
					if (errorsWriter == null)
					{
						errorsFile = getErrorsFile(settings);
						errorsWriter = createErrorsWriter(settings, errorsFile);
					}
					writeErrors(compErrors, errorsWriter, result.getTotal());
				}
				
				processRowComparisonData(compData, result, detailWriters, settings);
				
				if (!canContinue())
					break;
			}
			while (tableComp.hasMoreRows());
			
			finishComparison(result, settings, detailWriters, errorsFile);
			return result;
		}
		finally
		{
			closeWriters(detailWriters);
			Utils.closeResource(errorsWriter);
		}
	}
	
	public void interrupt()
	{
		interrupted.set(true);
	}
	
	
	protected IValueTransformer createValueTransformer(ComparisonSettings settings)
	{
		return new BigDecimalValueTransformer();
	}
	
	protected ValuesComparator<String, String> createValuesComparator(ComparisonSettings settings, IValueTransformer valueTransformer)
	{
		if (settings.getMapping() == null)
			return new StringValuesComparator(settings.getComparisonUtils());
		return new MappedStringValuesComparator(settings.getComparisonUtils(), settings.getMapping(), valueTransformer);
	}
	
	protected TableRowsComparator<String, String> createRowsComparator(ComparisonSettings settings, ValuesComparator<String, String> valuesComp)
	{
		if (settings.getMapping() == null)
			return new TableRowsComparator<>(valuesComp);
		return new MappedTableRowsComparator<>(valuesComp, settings.getMapping());
	}
	
	protected TableRowMatcher<String, String, CollectionPrimaryKey<String>> createRowMatcher(ComparisonSettings settings, IValueTransformer valueTransformer)
	{
		DataMapping<String> mapping = settings.getMapping();
		Set<String> keys = mapping.getKeyColumns();
		Map<String, BigDecimal> numerics = mapping.getNumericColumns();
		if (numerics.isEmpty())
			return new StringTableRowMatcher(keys);
		return new NumericStringTableRowMatcher(keys, numerics, valueTransformer);
	}
	
	protected TableDataComparator<String, String> createTableDataComparator(BasicTableDataReader<String, String, ?> expectedReader, BasicTableDataReader<String, String, ?> actualReader, 
			ComparisonSettings settings, TableRowsComparator<String, String> rowsComp, IValueTransformer valueTransformer) throws IOException, ParametersException
	{
		if (settings.getMapping() == null || settings.getMapping().getKeyColumns().isEmpty())
			return new StringTableDataComparator(expectedReader, actualReader, rowsComp);
		
		TableRowMatcher<String, String, CollectionPrimaryKey<String>> rowMatcher = createRowMatcher(settings, valueTransformer);
		return new IndexedStringTableDataComparator<>(expectedReader, actualReader, rowMatcher, rowsComp);
	}
	
	
	protected Path getErrorsFile(ComparisonSettings settings)
	{
		return settings.getOutputDir().resolve("errors.txt");
	}
	
	protected Writer createErrorsWriter(ComparisonSettings settings, Path file) throws IOException
	{
		return FileOperationUtils.newBlockingBufferedWriter(file);
	}
	
	protected void writeErrors(Collection<String> errors, Writer writer, int rowIndex) throws IOException
	{
		writer.write("Row #"+rowIndex+":");
		writer.write(eol);
		for (String e : errors)
		{
			writer.write("  "+e);
			writer.write(eol);
		}
	}
	
	
	protected CsvComparisonWriter createWriter(ComparisonSettings settings)
	{
		return new CsvComparisonWriter(0, false, settings.getOutputDir().toFile());
	}
	
	protected String createHeader(RowComparisonData<String, String> row, int rowIndex)
	{
		return "Row #"+rowIndex;
	}
	
	
	private ComparisonResult nothingToCompare()
	{
		ComparisonResult result = new ComparisonResult();
		result.setDescription("Both datasets are empty");
		return result;
	}
	
	private void closeWriters(Map<RowComparisonResultType, CsvComparisonWriter> writers)
	{
		for (CsvComparisonWriter w : writers.values())
			Utils.closeResource(w);
	}
	
	private void processRowComparisonData(RowComparisonData<String, String> row, ComparisonResult result, 
			Map<RowComparisonResultType, CsvComparisonWriter> writers, ComparisonSettings settings)
			throws ComparisonException, IOException
	{
		RowComparisonResultType type = row.getResultType();
		switch (type)
		{
		case PASSED : result.incPassed(); break;
		case FAILED : result.incFailed(); break;
		case NOT_FOUND : result.incNotFound(); break;
		case EXTRA : result.incExtra(); break;
		default : throw new ComparisonException("Unsupported result type - "+type);
		}
		
		CsvComparisonWriter writer = writers.computeIfAbsent(type, t -> createWriter(settings));
		writer.addDetail(row.toDetailedResult(), createHeader(row, result.getTotal()));
	}
	
	private boolean canContinue()
	{
		return !interrupted.get();
	}
	
	private void finishComparison(ComparisonResult result, ComparisonSettings settings, Map<RowComparisonResultType, CsvComparisonWriter> writers, Path errorsFile) throws IOException
	{
		Path outputDir = settings.getOutputDir();
		
		Path file;
		if ((file = saveReport(writers.get(RowComparisonResultType.PASSED), outputDir, "passed")) != null)
			result.setPassedDetails(file);
		
		if ((file = saveReport(writers.get(RowComparisonResultType.FAILED), outputDir, "failed")) != null)
			result.setFailedDetails(file);
		
		if ((file = saveReport(writers.get(RowComparisonResultType.NOT_FOUND), outputDir, "not_found")) != null)
			result.setNotFoundDetails(file);
		
		if ((file = saveReport(writers.get(RowComparisonResultType.EXTRA), outputDir, "extra")) != null)
			result.setExtraDetails(file);
		
		result.setErrors(errorsFile);
		result.setFinished(LocalDateTime.now());
	}
	
	private Path saveReport(CsvComparisonWriter writer, Path directory, String name) throws IOException
	{
		if (writer == null)
			return null;
		
		return writer.finishReport(directory, name, "", true);
	}
}
