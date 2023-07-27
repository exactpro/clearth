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
import com.exactprosystems.clearth.utils.SettingsException;

public class CompareDataSetsParameters
{
	private static final Path OUTPUT_DIR = Paths.get("testOutput"),
			ACTION_RESULT_DIR = OUTPUT_DIR.resolve("CompareDataSetsParameters"),
			ACTION_RESOURCE_DIR = ApplicationManager.USER_DIR.resolve("src").resolve("test").resolve("resources").resolve("Action").resolve("CompareDataSetsParameters"),
			TEST_DATA_DIR = ACTION_RESOURCE_DIR.resolve("testData");
	
	private ApplicationManager manager;
	
	@BeforeClass
	public void init() throws IOException, ClearThException, SettingsException, SQLException
	{
		FileUtils.deleteDirectory(ACTION_RESULT_DIR.toFile());
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
	
	
	@Test(dataProvider = "dataForMinRows")
	public void minRowsInReport(Path dataFile, int minRows, Path expectedFile) throws FailoverException, IOException
	{
		ActionSettings settings = new ActionSettings();
		settings.setParams(createInputParams(dataFile, minRows));
		CompareDataSets action = new CompareDataSets();
		action.init(settings);
		
		Result result = action.execute(new StepContext("Step1", new Date()), new MatrixContext(), TestActionUtils.createGlobalContext(ADMIN));
		Assert.assertTrue(result instanceof ContainerResult, "Result class is ContainerResult");
		
		File actionResultStorage = ACTION_RESULT_DIR.toFile();
		result.processDetails(actionResultStorage, action);
		ContainerResult containerResult = (ContainerResult) result;
		CsvDetailedResult csvResult = (CsvDetailedResult) containerResult.getDetails().get(0);
		
		File resultFile = csvResult.getReportFile();
		if (expectedFile == null)
		{
			Assert.assertNull(resultFile, "Action result file");
			return;
		}
		
		File actualFile = FileOperationUtils.unzipFile(resultFile, actionResultStorage).get(0);
		
		Assert.assertEquals(FileUtils.readFileToString(actualFile, StandardCharsets.UTF_8), 
				FileUtils.readFileToString(expectedFile.toFile(), StandardCharsets.UTF_8),
				"File content");
	}
	
	
	private Map<String, String> createInputParams(Path file, int minRowsinReport)
	{
		String filePath = file.toString();
		Map<String, String> inputParams = new HashMap<>();
		inputParams.put("ID", "id1");
		inputParams.put("Globalstep", "Step1");
		inputParams.put("Action", "CompareDataSets");
		inputParams.put("ExpectedFormat", "CsvFile");
		inputParams.put("ExpectedSource", filePath);
		inputParams.put("ActualFormat", "CsvFile");
		inputParams.put("ActualSource", filePath);
		if (minRowsinReport >= 0)
			inputParams.put("MinPassedRowsInReport", Integer.toString(minRowsinReport));
		return inputParams;
	}
}
