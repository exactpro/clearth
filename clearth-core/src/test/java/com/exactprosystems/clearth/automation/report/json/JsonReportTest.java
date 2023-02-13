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

package com.exactprosystems.clearth.automation.report.json;

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ClearThVersion;
import com.exactprosystems.clearth.automation.CoreStepKind;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.automation.report.ActionReportWriter.JSON_REPORT_NAME;
import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class JsonReportTest extends BasicTestNgTest
{
	private static final Path JSON_REPORTS_OUTPUT = Paths.get("testOutput/json_reports");
	private static final String MATRIX_FILE_NAME = "CheckJsonReports1.csv";
	private static final String ACTIONS_REPORTS_DIR = "JsonReports";
	private static final String USER_NAME = "admin";
	private static final Date START_TIME = new Date(1580126821783L);
	private static final Date END_TIME = new Date(1580126822273L);
	private static final String HOST_VAR = "%HOST%";


	@Override
	protected void mockOtherApplicationFields(ClearThCore application)
	{
		ClearThVersion version = mock(ClearThVersion.class);
		when(version.getBuildNumber()).thenReturn("local_build");
		when(application.getVersion()).thenReturn(version);
	}

	@BeforeMethod
	public void setUp() throws IOException
	{
		Files.createDirectories(JSON_REPORTS_OUTPUT);
	}

	@DataProvider(name = "json")
	Object[][] createParamsForJsonReports() throws FileNotFoundException
	{
		Step step1 = mockStep("Step1", new Date(1580126822098L), new Date(1580126822208L));
		Step step2 = mockStep("Step2", new Date(1580126822215L), new Date(1580126822265L));
		
		Matrix matrix1 = mock(Matrix.class);
		when(matrix1.getName()).thenReturn(MATRIX_FILE_NAME);
		when(matrix1.getShortFileName()).thenReturn(MATRIX_FILE_NAME);
		when(matrix1.isSuccessful()).thenReturn(true);
		when(matrix1.getDescription()).thenReturn("This is a test matrix for JSON reports");
		when(matrix1.getConstants()).thenReturn(map("Currency", "EUR", "ISIN", "E00000187"));
		when(matrix1.isStepSuccessful(anyString())).thenReturn(true);
		
		return new Object[][]
				{
						{
								matrix1, asList(step1, step2), new HashSet<>(asList("Step1", "Step2")),
								Paths.get(resourceToAbsoluteFilePath("JsonReports/CheckJsonReports1.csv")),
								Paths.get(resourceToAbsoluteFilePath("JsonReports/CheckJsonReports1.csv/ExpectedJsonReport.json"))
						}
				};
	}

	@Test(dataProvider = "json")
	public void testBuildAndWriteJsonReports(Matrix matrix, List<Step> allSteps, Set<String> matrixSteps,
	                                         Path actionsReportsPath, Path expectedReportPath) throws IOException
	{
		Files.createDirectories(JSON_REPORTS_OUTPUT.resolve(MATRIX_FILE_NAME));

		Path actualReportPath = JSON_REPORTS_OUTPUT.resolve(MATRIX_FILE_NAME).resolve(JSON_REPORT_NAME);
		JsonReport report = new JsonReport(matrix, allSteps, matrixSteps, USER_NAME, START_TIME, END_TIME);
		report.writeReport(actualReportPath, actionsReportsPath);

		String expectedJson = loadExpectedJson(expectedReportPath);
		
		String actualJson = readFileToString(actualReportPath.toFile(), UTF_8);
		actualJson = actualJson.replace("\r", "");
		
		assertEquals(actualJson, expectedJson);
	}

	@AfterMethod
	public void tearDown() throws IOException
	{
		FileUtils.cleanDirectory(JSON_REPORTS_OUTPUT.toFile());
	}
	
	private Step mockStep(String name, Date startTime, Date endTime)
	{
		Step step = mock(Step.class);
		when(step.getName()).thenReturn(name);
		when(step.getSafeName()).thenReturn(name);
		when(step.getKind()).thenReturn(CoreStepKind.Default.getLabel());
		when(step.isFailedDueToError()).thenReturn(false);
		when(step.getStarted()).thenReturn(startTime);
		when(step.getFinished()).thenReturn(endTime);
		return step;
	}
	
	private String loadExpectedJson(Path path) throws IOException
	{
		String json = readFileToString(path.toFile(), UTF_8);
		json = json.replace(HOST_VAR, Utils.host());
		json = json.replace("\r", "");
		return json;
	}
}