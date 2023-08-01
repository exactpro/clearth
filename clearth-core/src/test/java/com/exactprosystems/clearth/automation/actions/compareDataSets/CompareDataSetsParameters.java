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

package com.exactprosystems.clearth.automation.actions.compareDataSets;

import static com.exactprosystems.clearth.ApplicationManager.ADMIN;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.ActionSettings;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.TestActionUtils;
import com.exactprosystems.clearth.automation.actions.CompareDataSets;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.CsvDetailedResult;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.SettingsException;

public class CompareDataSetsParameters
{
	private static final Path OUTPUT_DIR = Paths.get("testOutput"),
			ACTION_RESULT_DIR = OUTPUT_DIR.resolve("CompareDataSetsParameters"),
			ACTION_RESOURCE_DIR = ApplicationManager.USER_DIR.resolve("src").resolve("test").resolve("resources").resolve("Action").resolve("CompareDataSetsParameters"),
			TEST_DATA_DIR = ACTION_RESOURCE_DIR.resolve("testData");
	private static final File ACTION_RESULT_DIR_FILE = ACTION_RESULT_DIR.toFile();
	private static final String PARAM_MIN_PASSED = "MinPassedRowsInReport",
			PARAM_MIN_FAILED = "MinFailedRowsInReport",
			PARAM_LIST_FAILED = "ListFailedColumnsInReport";
	
	private ApplicationManager manager;
	
	@BeforeClass
	public void init() throws IOException, ClearThException, SettingsException, SQLException
	{
		FileUtils.deleteDirectory(ACTION_RESULT_DIR_FILE);
		Files.createDirectories(ACTION_RESULT_DIR);
		
		manager = new ApplicationManager(ACTION_RESOURCE_DIR.resolve("clearth.cfg").toString());
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (manager != null)
			manager.dispose();
	}
	
	
	@DataProvider(name = "dataForMinRows")
	public Object[][] dataForMinRows()
	{
		Path file55Rows = TEST_DATA_DIR.resolve("55Rows.csv"),
				file2Rows = TEST_DATA_DIR.resolve("2Rows.csv"),
				file3Rows = TEST_DATA_DIR.resolve("3Rows.csv");
		
		return new Object[][] {
			{file55Rows, -1, TEST_DATA_DIR.resolve("55RowsExpectedResult.csv")},
			{file2Rows, -1, null},
			{file3Rows, 3, TEST_DATA_DIR.resolve("3RowsExpectedResult.csv")},
			{file2Rows, 3, null}
		};
	}
	
	@DataProvider(name = "dataForListFailed")
	public Object[][] dataForListFailed()
	{
		return new Object[][] {
			{false, TEST_DATA_DIR.resolve("3ColumnsFailedResult_Default.csv")},
			{true, TEST_DATA_DIR.resolve("3ColumnsFailedResult_FailedList.csv")}
		};
	}
	
	
	@Test(dataProvider = "dataForMinRows")
	public void minRowsInReport(Path dataFile, int minRows, Path expectedFile) throws FailoverException, IOException
	{
		Map<String, String> params = createInputParams(dataFile, dataFile,
				minRows >= 0 ? new Pair<>(PARAM_MIN_PASSED, Integer.toString(minRows)) : null);
		ContainerResult containerResult = runCompareDataSets(params);
		CsvDetailedResult csvResult = (CsvDetailedResult) containerResult.getDetails().get(0);
		
		File resultFile = csvResult.getReportFile();
		if (expectedFile == null)
		{
			Assert.assertNull(resultFile, "Action result file");
			return;
		}
		
		File actualFile = FileOperationUtils.unzipFile(resultFile, ACTION_RESULT_DIR_FILE).get(0);
		assertFiles(actualFile, expectedFile.toFile(), "File content");
	}
	
	@Test(dataProvider = "dataForListFailed")
	public void failedColumns(boolean listFailed, Path failedResultFile) throws FailoverException, IOException
	{
		Path expectedFile = TEST_DATA_DIR.resolve("3ColumnsExpected.csv"),
				actualFile = TEST_DATA_DIR.resolve("3ColumnsActual.csv"),
				passedResultFile = TEST_DATA_DIR.resolve("3ColumnsPassedResult.csv");
		
		Map<String, String> params = createInputParams(expectedFile, actualFile, 
				new Pair<>(PARAM_MIN_PASSED, "1"),
				new Pair<>(PARAM_MIN_FAILED, "1"),
				listFailed ? new Pair<>(PARAM_LIST_FAILED, "true") : null);
		ContainerResult containerResult = runCompareDataSets(params);
		
		List<Result> subResults = containerResult.getDetails();
		CsvDetailedResult actualPassedResult = (CsvDetailedResult) subResults.get(0),
				actualFailedResult = (CsvDetailedResult) subResults.get(1);
		
		File actualPassedResultFile = FileOperationUtils.unzipFile(actualPassedResult.getReportFile(), ACTION_RESULT_DIR_FILE).get(0);
		assertFiles(actualPassedResultFile, passedResultFile.toFile(), "Passed file content");
		
		File actualFailedResultFile = FileOperationUtils.unzipFile(actualFailedResult.getReportFile(), ACTION_RESULT_DIR_FILE).get(0);
		assertFiles(actualFailedResultFile, failedResultFile.toFile(), "Failed file content");
	}
	
	
	@SafeVarargs
	private Map<String, String> createInputParams(Path expectedFile, Path actualFile, Pair<String, String>... params)
	{
		Map<String, String> inputParams = new HashMap<>();
		inputParams.put("ID", "id1");
		inputParams.put("Globalstep", "Step1");
		inputParams.put("Action", "CompareDataSets");
		inputParams.put("ExpectedFormat", "CsvFile");
		inputParams.put("ExpectedSource", expectedFile.toString());
		inputParams.put("ActualFormat", "CsvFile");
		inputParams.put("ActualSource", actualFile.toString());
		
		if (params != null)
		{
			for (Pair<String, String> p : params)
			{
				if (p != null)
					inputParams.put(p.getFirst(), p.getSecond());
			}
		}
		return inputParams;
	}
	
	private ContainerResult runCompareDataSets(Map<String, String> params) throws FailoverException
	{
		ActionSettings settings = new ActionSettings();
		settings.setParams(params);
		CompareDataSets action = new CompareDataSets();
		action.init(settings);
		
		Result result = action.execute(new StepContext("Step1", new Date()), new MatrixContext(), TestActionUtils.createGlobalContext(ADMIN));
		Assert.assertTrue(result instanceof ContainerResult, "Result class is ContainerResult");
		
		result.processDetails(ACTION_RESULT_DIR_FILE, action);
		return (ContainerResult) result;
	}
	
	private void assertFiles(File actual, File expected, String description) throws IOException
	{
		Assert.assertEquals(FileUtils.readFileToString(actual, StandardCharsets.UTF_8), 
				FileUtils.readFileToString(expected, StandardCharsets.UTF_8),
				description);
	}
}
