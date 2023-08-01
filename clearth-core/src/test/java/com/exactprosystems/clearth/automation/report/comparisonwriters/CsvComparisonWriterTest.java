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

package com.exactprosystems.clearth.automation.report.comparisonwriters;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.CsvDetailedResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.automation.report.results.resultReaders.CsvDetailedResultReader;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.comparison.valuesComparators.StringValuesComparator;
import com.exactprosystems.clearth.utils.tabledata.converters.StringValueParser;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.*;

public class CsvComparisonWriterTest
{
	private static final Path TEST_DIR = Paths.get("testOutput", CsvComparisonWriterTest.class.getSimpleName());
	private static final String TEST_HEADER = "TestRow", 
			NOT_TEST_HEADER = "Not" + TEST_HEADER;

	private ApplicationManager applicationManager;
	@BeforeClass
	private void beforeClass() throws ClearThException
	{
		applicationManager = new ApplicationManager();
		FileUtils.deleteQuietly(TEST_DIR.toFile());
	}
	
	@AfterClass
	private void afterClass() throws IOException
	{
		if (applicationManager != null)
			applicationManager.dispose();
	}
	
	@Test
	private void defaultTest() throws Exception
	{
		CsvComparisonWriter writer = null;
		CsvDetailedResultReader reader = null;
		try
		{
			writer = new CsvComparisonWriter(0, false, new File(ClearThCore.tempPath()));
			CsvDetailedResult csvResult = createCsvResult();
			writer.addDetail(csvResult);
			writer.finishReport(TEST_DIR, "", "", false);
			
			File[] reportFiles = TEST_DIR.resolve("details").toFile().listFiles();
			assertNotNull(reportFiles, "Report was not created");
			assertEquals(reportFiles.length, 1,
					"Found more than one file by path " + reportFiles[0].getParent());
			
			reader = new CsvDetailedResultReader(reportFiles[0],
					new StringValuesComparator(new ComparisonUtils()), new StringValueParser());
			
			for (DetailedResult result : csvResult.getDetails())
			{
				assertEquals(reader.readNext(), result);
			}
			assertNull(reader.readNext());
		}
		finally
		{
			Utils.closeResource(writer);
			Utils.closeResource(reader);
		}
	}
	
	private CsvDetailedResult createCsvResult()
	{
		CsvDetailedResult result = new CsvDetailedResult(NOT_TEST_HEADER);
		DetailedResult detailedResult = new DetailedResult();
		detailedResult.setComment(TEST_HEADER); // row result's comment is header in written file
		detailedResult.addResultDetail(new ResultDetail("Param1", "expected", "actual", false));
		detailedResult.addResultDetail(new ResultDetail("Param2", "same", "same", true));
		detailedResult.addResultDetail(new ResultDetail("Param3", "  space-prefix", "space-prefix", false));
		detailedResult.addResultDetail(new ResultDetail("Param4", "space-postfix", "space-postfix ", false));
		result.addDetail(detailedResult);
		
		return result;
	}
}