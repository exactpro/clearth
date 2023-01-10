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

package com.exactprosystems.clearth.automation.report.results;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.utils.ClearThException;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.exactprosystems.clearth.automation.report.Result.DETAILS_DIR;
import static org.testng.Assert.*;

public class CsvContainerResultTest
{
	private final static String SAME_VALUE = "same_value",
			EXPECTED = "EXPECTED", ACTUAL = "ACTUAL",
			EXPECTED_VALUE = "expected_value", ACTUAL_VALUE = "actual_value",
			FAILED = "FAILED", PASSED = "PASSED",
			PARAM_NAME = "param";
	private final static String MAX_RECORDS_LIMIT_1 = "maxDisplayedRowsCount_1",
			SHOULD_NOT_EXIST = "shouldNotExist",
			WRITE_REPORT_ANYWAY = "writeReportAnyway";
	protected static String EXTENSION = ".csv";
	protected static Path testRootPath = Paths.get("testOutput").resolve(CsvContainerResultTest.class.getSimpleName());
	private static ApplicationManager applicationManager;
	
	private static ContainerResult passedResultDetail, failedResultDetail;
	
	@BeforeClass
	public static void beforeClass() throws ClearThException
	{
		applicationManager = new ApplicationManager();
		FileUtils.deleteQuietly(testRootPath.toFile());
		
		passedResultDetail = createContainerResult(PARAM_NAME, SAME_VALUE, SAME_VALUE, true);
		failedResultDetail = createContainerResult(PARAM_NAME, EXPECTED_VALUE, ACTUAL_VALUE, false);
	}
	
	protected static ContainerResult createContainerResult(String paramName, String expectedValue, String actualValue,
	                                                         boolean identical)
	{
		ContainerResult result = new ContainerResult(paramName, false);
		result.addDetail(createDetailedResult(paramName, expectedValue, actualValue, identical));
		return result;
	}
	
	protected static DetailedResult createDetailedResult(String paramName, String expectedValue, String actualValue, 
	                                                     boolean identical)
	{
		DetailedResult detailedResult = new DetailedResult();
		detailedResult.addResultDetail(new ResultDetail(paramName, expectedValue, actualValue, identical));
		return detailedResult;
	}
	
	@AfterClass
	public static void afterClass() throws IOException
	{
		if (applicationManager != null)
			applicationManager.dispose();
	}

	@Test
	public void testOnlyFailedInHtml() throws IOException
	{
		String testName = "testOnlyFailedInHtml";
		Path testPath = testRootPath.resolve(testName);
		Action action = new StubAction(testName);
		boolean[] comparisonRowResults = new boolean[]{ false, true };
		
		// Should create because there are passed results
		CsvContainerResult containerResult = CsvContainerResult.createPlainResult();
		containerResult.setOnlyFailedInHtml(true);
		assertCreates(containerResult, testPath.resolve("base"), action, comparisonRowResults);

		containerResult = CsvContainerResult.createPlainResult();
		containerResult.setOnlyFailedInHtml(true);
		assertNotCreates(containerResult, testPath.resolve(SHOULD_NOT_EXIST), action, false, false);

		containerResult = CsvContainerResult.createPlainResult();
		containerResult.setOnlyFailedInHtml(true);
		containerResult.setMaxDisplayedRowsCount(1);
		assertCreates(containerResult, testPath.resolve(MAX_RECORDS_LIMIT_1), action, false, false);
	}
	
	@Test
	public void testOnlyFailedInCsv()
	{
		String testName = "testOnlyFailedInCsv";
		Path testPath = testRootPath.resolve(testName);
		Action action = new StubAction(testName);
		boolean[] sourceResults = new boolean[]{ false, true, false, true, false };

		CsvContainerResult containerResult = CsvContainerResult.createPlainResult();
		containerResult.setOnlyFailedInCsv(true);
		containerResult.setMaxDisplayedRowsCount(3);
		//Should not exist because there should be added only 3 results and max displayed count is 3 too
		assertNotCreates(containerResult, testPath.resolve(SHOULD_NOT_EXIST), action, sourceResults);

		containerResult = CsvContainerResult.createPlainResult();
		containerResult.setOnlyFailedInCsv(true);
		containerResult.setWriteCsvReportAnyway(true);
		assertCreates(containerResult, testPath.resolve(WRITE_REPORT_ANYWAY), action, sourceResults);
	}
	
	@Test
	public void testEmptyResult()
	{
		String testName = "testEmptyResult";
		Path testPath = testRootPath.resolve(testName);
		Action action = new StubAction(testName);
		CsvContainerResult containerResult = CsvContainerResult.createPlainResult();
		containerResult.setWriteCsvReportAnyway(true);
		assertNotCreates(containerResult, testPath.resolve(SHOULD_NOT_EXIST), action);
	}

	@Test
	public void testDefault()
	{
		String testName = "testCreatesReport";
		Path testPath = testRootPath.resolve(testName);
		Action action = new StubAction(testName);
		boolean[] defaultRowResults = new boolean[60];
		for (int i = 0; i < 30; i++)
		{
			defaultRowResults[i * 2] = true;
			defaultRowResults[i * 2 + 1] = false;
		}

		CsvContainerResult containerResult = CsvContainerResult.createPlainResult();
		assertNotCreates(containerResult, testPath.resolve(SHOULD_NOT_EXIST), action, false, true);
		
		containerResult = CsvContainerResult.createPlainResult();
		assertCreates(containerResult, testPath.resolve("default"), action, defaultRowResults);
	}

	private void assertNotCreates(CsvContainerResult containerResult, Path reportPath, Action action,
	                              boolean... results)
	{
		for (boolean identical : results)
			containerResult.addDetail(identical ? passedResultDetail : failedResultDetail);

		containerResult.processDetails(reportPath.toFile(), action);
		
		assertFalse(reportPath.toFile().exists());
	}
	
	private void assertCreates(CsvContainerResult containerResult, Path reportPath, Action action,
	                                    boolean... results)
	{
		for (boolean identical : results)
			containerResult.addDetail(identical ? passedResultDetail : failedResultDetail);

		containerResult.processDetails(reportPath.toFile(), action);

		File[] reportFiles = reportPath.resolve(DETAILS_DIR).toFile().listFiles();
		assertNotNull(reportFiles, "Report file was not created");
		assertEquals(reportFiles.length, 1, "expected only one reportFile, found: " + Arrays.toString(reportFiles));
	}
	
	private static class StubAction extends Action
	{
		private final String prefix;

		private StubAction(String prefix)
		{
			this.prefix = prefix;
		}

		@Override
		public String getStepName()
		{
			return prefix + "StepName";
		}

		@Override
		public String getIdInMatrix()
		{
			return prefix + "IdInMatrix";
		}

		@Override
		public String getName()
		{
			return prefix + "ActionName";
		}

		@Override
		protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
				throws ResultException, FailoverException
		{
			return null;
		}
	}
}