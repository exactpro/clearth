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

package com.exactprosystems.clearth.automation.report.resultReaders;

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.CsvDetailedResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.automation.report.results.resultReaders.CsvDetailedResultReader;
import com.exactprosystems.clearth.utils.BigDecimalValueTransformer;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.NumericStringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.StringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.StringValueParser;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doReturn;
import static org.testng.Assert.*;

public class CsvDetailedResultReaderTest extends BasicTestNgTest
{
	private NumericStringValuesComparator numericValuesComparator;
	private StringValuesComparator valuesComparator;
	private StringValueParser valueParser;
	
	private static final Path zipFilesPath =
			Paths.get("src", "test", "resources").resolve(CsvDetailedResultReaderTest.class.getSimpleName());
	private static final Path testDir = Paths.get("testOutput").resolve(CsvDetailedResultReaderTest.class.getSimpleName());
	
	@Override
	protected void mockOtherApplicationFields(ClearThCore application)
	{
		Path tempDir = testDir.resolve("temp").toAbsolutePath();
		FileUtils.deleteQuietly(testDir.toFile());
		tempDir.toFile().mkdirs();
		doReturn(tempDir.toString()).when(application).getTempDirPath();
	}
	
	@Override
	public void mockOtherBeforeClass()
	{
		ComparisonUtils comparisonUtils = new ComparisonUtils();
		Map<String, BigDecimal> numericColumns = new HashMap<String, BigDecimal>() {{
			put("Column_1", BigDecimal.ONE);
		}};
		IValueTransformer valueTransformer = new BigDecimalValueTransformer();
		
		valuesComparator = new StringValuesComparator(comparisonUtils);
		valueParser = new StringValueParser();
		numericValuesComparator = new NumericStringValuesComparator(comparisonUtils, numericColumns, valueTransformer);
	}
	
	private DetailedResult generateDetailedResult(String comment, ResultDetail... resultDetails)
	{
		DetailedResult result = new DetailedResult();
		result.setComment(comment);
		for (ResultDetail resultDetail : resultDetails)
			result.addResultDetail(resultDetail);
		
		return result;
	}
	
	@Test
	private void passedReportTest() throws Exception
	{
		File file = new File(zipFilesPath.resolve("passedReport.zip").toUri());
		DetailedResult expectedResult1 = generateDetailedResult("Row #12",
				new ResultDetail("Column_1", "1", "1", true),
				new ResultDetail("Column_2", "2", "2", true),
				new ResultDetail("Column_3", "3", "3", true),
				new ResultDetail("Column_4","2023-03-02", "2023-03-02", true));
		
		DetailedResult expectedResult2 = generateDetailedResult("Row #13",
				new ResultDetail("Column_1", "4", "4", true),
				new ResultDetail("Column_2", "5.0", "5.0", true),
				new ResultDetail("Column_3", "6", "6", true),
				new ResultDetail("Column_4","2023-03-01", "2023-03-01", true));
		try (CsvDetailedResultReader reader = new CsvDetailedResultReader(file, numericValuesComparator, valueParser))
		{
			assertEquals(reader.readNext(), expectedResult1);
			assertEquals(reader.readNext(), expectedResult2);
			assertNull(reader.readNext());
		}
	}

	@Test
	private void failedReportTest() throws Exception
	{
		File file = new File(zipFilesPath.resolve("failedReport.zip").toUri());
		DetailedResult expectedResult1 = generateDetailedResult("Row #12",
				new ResultDetail("Column_1", "1", "1", true),
				new ResultDetail("Column_2", "2", "90", false),
				new ResultDetail("Column_3", "3", "3", true),
				new ResultDetail("Column_4","2023-03-02", "02032023", false));
		
		
		try (CsvDetailedResultReader reader = new CsvDetailedResultReader(file, numericValuesComparator, valueParser))
		{
			assertEquals(reader.readNext(), expectedResult1);
			assertNull(reader.readNext());
		}
	}

	@Test
	private void numericReportTest() throws Exception
	{
		File file = new File(zipFilesPath.resolve("numericsPassedReport.zip").toUri());
		DetailedResult expectedResult = generateDetailedResult("Row #12",
				new ResultDetail("Column_1", "1", "1.1", true));
		try (CsvDetailedResultReader reader = new CsvDetailedResultReader(file, numericValuesComparator, valueParser))
		{
			assertEquals(reader.readNext(), expectedResult);
			assertNull(reader.readNext());
		}

		try (CsvDetailedResultReader reader = new CsvDetailedResultReader(file, valuesComparator, valueParser))
		{
			assertNotEquals(reader.readNext(), expectedResult);
			assertNull(reader.readNext());
		}
	}
	
	@Test
	private void testGetReader() throws Exception
	{
		try (CsvDetailedResult result = new CsvDetailedResult())
		{
			result.setValueHandlers(valuesComparator, valueParser);
			result.setWriteCsvReportAnyway(true);
	
			DetailedResult expectedResult = generateDetailedResult("", new ResultDetail("field", "1", "1", true));
	
			result.addDetail(expectedResult);
			
			result.processDetails(testDir.toFile(), null);
			
			assertEquals(result.getReader().readNext(), expectedResult);
		}
	}
}