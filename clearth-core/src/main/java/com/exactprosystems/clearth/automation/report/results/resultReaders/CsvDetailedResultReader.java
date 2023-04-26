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

package com.exactprosystems.clearth.automation.report.results.resultReaders;

import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.ComparisonResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.ValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.ValueParser;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.exactprosystems.clearth.automation.report.results.CsvDetailedResult.*;

public class CsvDetailedResultReader implements AutoCloseable
{
	private static final Logger logger = LoggerFactory.getLogger(CsvDetailedResultReader.class);
	private ZipFile zipFile;
	protected final CsvDataReader reader;
	@SuppressWarnings("rawtypes")
	protected final ValuesComparator valuesComparator;
	@SuppressWarnings("rawtypes")
	protected final ValueParser valueParser;

	public<A, B> CsvDetailedResultReader(File zipFile,
	ValuesComparator<A, B> valuesComparator, ValueParser<A ,B> valueParser)
			throws IOException
	{
		this.valuesComparator = valuesComparator;
		this.valueParser = valueParser;
		
		reader = createCsvDataReader(zipFile);
		logger.trace("Created reader for {}", zipFile.getCanonicalPath());
		reader.start();
	}

	protected CsvDataReader createCsvDataReader(File zipFile1)
			throws IOException
	{
		zipFile = new ZipFile(zipFile1);
		ZipEntry entry = zipFile.entries().nextElement();
		return new CsvDataReader(new InputStreamReader(zipFile.getInputStream(entry)));
	}

	public DetailedResult readNext() throws Exception
	{
		if (!reader.hasMoreData())
			return null;
		
		TableRow<String, String> expectedRow = reader.readRow();
		if (!reader.hasMoreData())
			throw new IllegalStateException("Premature end of file: found expected data only");
		TableRow<String, String> actualRow = reader.readRow();
		 
		return toDetailedResult(expectedRow, actualRow);
	}
	
	protected DetailedResult toDetailedResult(TableRow<String, String> expectedRow, TableRow<String, String> actualRow)
			throws Exception
	{
		DetailedResult result = new DetailedResult();
		result.setComment(expectedRow.getValue(COLUMN_COMPARISON_NAME));
		for (String header : expectedRow.getHeader())
		{
			if (COLUMN_COMPARISON_NAME.equals(header) || COLUMN_ROW_KIND.equals(header) || COLUMN_COMPARISON_RESULT.equals(header))
				continue;

			result.addResultDetail(createResultDetail(header,  expectedRow.getValue(header), actualRow.getValue(header)));
		}

		String comparisonResultValue = expectedRow.getValue(COLUMN_COMPARISON_RESULT);
		if (FAILED.equals(comparisonResultValue) && result.isSuccess() ||
				PASSED.equals(comparisonResultValue) && !result.isSuccess())
		{
			logger.trace("Error while getting row with comparison name = '{}': " +
							"result of detail from CSV file is {}, but result contained in row is {}. result parsed: {}",
					expectedRow.getValue(COLUMN_COMPARISON_NAME),
					result.isSuccess() ? PASSED : FAILED,
					comparisonResultValue,
					result);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	protected ResultDetail createResultDetail(String header, String expectedValue, String actualValue) throws Exception
	{
		ComparisonResult compResult = valuesComparator.compareValues(valueParser.parseValue(expectedValue),
				valueParser.parseValue(actualValue),
				valueParser.parseHeader(header));
		
		return new ResultDetail(header, expectedValue, actualValue, compResult);
	}

	@Override
	public void close()
	{
		Utils.closeResource(reader);
		Utils.closeResource(zipFile);
	}
}