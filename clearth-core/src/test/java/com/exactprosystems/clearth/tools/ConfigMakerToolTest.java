/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.tools;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.utils.ClearThException;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Arrays;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

public class ConfigMakerToolTest
{
	public static final Path CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR = Paths.get("testOutput/ConfigMakerTool");
	private static final String CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR = "ConfigMakerToolTest";

	private static final String CONFIG_FILE_FROM_CSV = "csv_config.cfg";
	private static final String CONFIG_FILE_FROM_XLS = "xls_config.cfg";
	private static final String CONFIG_FILE_FROM_XLSX = "xlsx_config.cfg";

	public static final String CSV_MATRIX_FILE = "csv_matrix.csv";
	public static final String CSV_MATRIX_FILE_2 = "csv_matrix2.csv";
	public static final String XLS_MATRIX_FILE = "xls_matrix.xls";
	public static final String XLSX_MATRIX_FILE = "xlsx_matrix.xlsx";

	public static final String ABC_MATRIX_FILE = "abc_matrix.abc";
	public static final String NON_EXISTING_FILE = "123456";

	private static final String EXCEPTION_MESSAGE_REGEX =
			"No matrix file selected|Unsupported matrix file format|Matrix file does not exist!";

	private ConfigMakerTool configMakerTool;
	private ApplicationManager clearThManager;

	@BeforeClass
	public void startTestApplication() throws ClearThException, IOException
	{
		clearThManager = new ApplicationManager();
		Files.createDirectories(CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR);
		configMakerTool = ClearThCore.getInstance().getToolsFactory().createConfigMakerTool();
	}

	@DataProvider(name = "config-maker")
	Object[][] createDataForConfigMaker()
	{
		return new Object[][]{
				{CSV_MATRIX_FILE, CONFIG_FILE_FROM_CSV},
				{XLS_MATRIX_FILE, CONFIG_FILE_FROM_XLS},
				{XLSX_MATRIX_FILE, CONFIG_FILE_FROM_XLSX}};
	}

	@Test(dataProvider = "config-maker")
	public void checkMakeConfig(String matrixFileName, String configFileName) throws ClearThException, IOException
	{
		File matrix =
				Paths.get(resourceToAbsoluteFilePath(CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR))
						.resolve(matrixFileName)
						.toFile();
		File destDir = CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR.toFile();
		File expectedResult =
				Paths.get(resourceToAbsoluteFilePath(CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR))
						.resolve(configFileName)
						.toFile();
		File actualResult = configMakerTool.makeConfig(matrix, destDir, "");

		String actualContent = FileUtils.readFileToString(actualResult, StandardCharsets.UTF_8);
		String expectedContent = FileUtils.readFileToString(expectedResult, StandardCharsets.UTF_8);
		assertEquals(actualContent, expectedContent);
	}

	@DataProvider(name = "config-applier")
	Object[][] createDataForConfigApplySteps()
	{
		return new Object[][]{
				{CSV_MATRIX_FILE, Arrays.array("Step1")},
				{XLS_MATRIX_FILE, Arrays.array("Step1", "Step2")},
				{XLSX_MATRIX_FILE, Arrays.array("Step1", "Step2", "Step3")}
		};
	}

	@Test(dataProvider = "config-applier")
	public void checkMakeConfigAndApplySteps(String matrixFileName, String[] stepNames) throws Exception
	{
		File matrix =
				Paths.get(resourceToAbsoluteFilePath(CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR))
						.resolve(matrixFileName)
						.toFile();
		File destDir = CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR.toFile();
		Scheduler scheduler = clearThManager.getScheduler("admin", "admin");
		StepFactory stepFactory = scheduler.getStepFactory();

		List<Step> expectedSteps = new ArrayList<>();
		for (String step : stepNames)
		{
			expectedSteps.add(stepFactory.createStep(step, CoreStepKind.Default.getLabel(), "", StartAtType.DEFAULT,
					false,
					"", false, false, true, ""));
		}

		configMakerTool.makeConfigAndApply(scheduler, matrix, destDir, false);
		List<Step> actualSteps = scheduler.getSteps();
		assertEquals(actualSteps, expectedSteps);
	}

	@Test(dataProvider = "config-applier")
	public void checkMakeConfigAndApplyWarnings(String matrixFileName, String[] stepNames) throws Exception
	{
		File matrix =
				Paths.get(resourceToAbsoluteFilePath(CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR))
						.resolve(matrixFileName)
						.toFile();
		File destDir = CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR.toFile();
		Scheduler scheduler = clearThManager.getScheduler("admin", "admin");
		List<String> actualWarnings = configMakerTool.makeConfigAndApply(scheduler, matrix, destDir, false);
		assertTrue(actualWarnings.isEmpty());
	}

	@DataProvider(name = "unsupported-files")
	Object[][] createDataForUnsupportedMatrixFiles()
	{
		return new Object[][]{{ABC_MATRIX_FILE}, {NON_EXISTING_FILE}};
	}

	@Test(dataProvider = "unsupported-files", expectedExceptions = ClearThException.class,
			expectedExceptionsMessageRegExp = EXCEPTION_MESSAGE_REGEX)
	public void checkMakeConfigWithUnsupportedMatrixFiles(String matrixFileName) throws IOException, ClearThException
	{
		File matrix =
				Paths.get(resourceToAbsoluteFilePath(CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR))
						.resolve(matrixFileName)
						.toFile();
		File destDir = CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR.toFile();
		configMakerTool.makeConfig(matrix, destDir, "");
	}

	@Test(dataProvider = "unsupported-files", expectedExceptions = ClearThException.class,
			expectedExceptionsMessageRegExp = EXCEPTION_MESSAGE_REGEX)
	public void checkMakeAndApplyWithUnsupportedMatrixFiles(String matrixFileName) throws IOException, ClearThException
	{
		File matrix =
				Paths.get(resourceToAbsoluteFilePath(CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR))
						.resolve(matrixFileName)
						.toFile();
		File destDir = CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR.toFile();
		Scheduler scheduler = clearThManager.getScheduler("admin", "admin");
		configMakerTool.makeConfigAndApply(scheduler, matrix, destDir, false);
	}

	@DataProvider(name = "config-add-test")
	Object[][] createDataForConfigMakerAdd()
	{
		return new Object[][]{
				{CSV_MATRIX_FILE, CSV_MATRIX_FILE, Arrays.array("Step1"), 1},
				{CSV_MATRIX_FILE, CSV_MATRIX_FILE_2, Arrays.array("Step1", "Step2"), 0},
				{CSV_MATRIX_FILE_2, CSV_MATRIX_FILE, Arrays.array("Step2", "Step1"), 0},
				{null, CSV_MATRIX_FILE, Arrays.array("Step1"), 0}};
	}

	@Test(dataProvider = "config-add-test")
	public void checkMakeAndAdd(String matrixFileName, String secondMatrixFileName, String[] stepNames, int expectedWarningsCount)
		throws IOException, ClearThException
	{
		File destDir = CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR.toFile();
		Scheduler scheduler = clearThManager.getScheduler("admin", "admin");
		StepFactory stepFactory = scheduler.getStepFactory();

		List<Step> expectedSteps = new ArrayList<>();
		for (String step : stepNames)
		{
			expectedSteps.add(stepFactory.createStep(step, CoreStepKind.Default.getLabel(), "", StartAtType.DEFAULT,
					false,
					"", false, false, true, ""));
		}

		int warningCount = 0;

		if (matrixFileName != null) 
		{
			File matrix =
				Paths.get(resourceToAbsoluteFilePath(CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR))
						.resolve(matrixFileName)
						.toFile();
			warningCount += configMakerTool.makeConfigAndApply(scheduler, matrix, destDir, false).size();
		}
		else
			scheduler.clearSteps();

		if (secondMatrixFileName != null) 
		{
			File matrix =
				Paths.get(resourceToAbsoluteFilePath(CONFIG_MAKER_TOOL_TEST_RESOURCE_DIR))
						.resolve(secondMatrixFileName)
						.toFile();
			warningCount += configMakerTool.makeConfigAndApply(scheduler, matrix, destDir, true).size();
		}

		List<Step> actualSteps = scheduler.getSteps();
		assertEquals(warningCount, expectedWarningsCount);
		assertEquals(actualSteps, expectedSteps);
	}

	@AfterMethod
	public void tearDown() throws IOException
	{
		FileUtils.cleanDirectory(CONFIG_MAKER_TOOL_TEST_OUTPUT_DIR.toFile());
	}
}