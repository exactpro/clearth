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
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.automation.report.results.resultReaders.CsvContainerResultReader;
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
	private static final String TEST_HEADER = "TestRow";

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
		CsvContainerResultReader reader = null;
		try
		{
			writer = new CsvComparisonWriter(0);
			ContainerResult containerResult = createContainerResult();
			writer.addDetail(containerResult);
			writer.finishReport(TEST_DIR, "", "", false);

			File[] reportFiles = TEST_DIR.resolve("details").toFile().listFiles();
			assertNotNull(reportFiles, "Report was not created");
			assertEquals(reportFiles.length, 1,
					"Found more than one file by path " + reportFiles[0].getParent());
			
			reader = new CsvContainerResultReader(reportFiles[0],
					new StringValuesComparator(new ComparisonUtils()), new StringValueParser());
			
			for (Result result : containerResult.getDetails())
			{
				DetailedResult detailedResult = (DetailedResult) result;
				assertEquals(reader.readNext(), detailedResult);
			}
			assertNull(reader.readNext());
		}
		finally
		{
			Utils.closeResource(writer);
			Utils.closeResource(reader);
		}
	}
	
	private ContainerResult createContainerResult()
	{
		ContainerResult result = ContainerResult.createBlockResult(TEST_HEADER);
		DetailedResult detailedResult = new DetailedResult();
		detailedResult.setComment(TEST_HEADER); // result's header becomes comment in reader
		detailedResult.addResultDetail(new ResultDetail("Param1", "expected", "actual", false));
		detailedResult.addResultDetail(new ResultDetail("Param2", "same", "same", true));
		result.addDetail(detailedResult);
		
		return result;
	}
}