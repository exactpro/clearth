/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.StringOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class SchedulerDataTest
{
	private static final Path TEST_OUTPUT = Paths.get("testOutput").resolve(SchedulerDataTest.class.getSimpleName()),
								CFG_FILE = Paths.get("configs","config.cfg");
	private static final String REPORTS_DIR_NAME = "schedulerReports";
	
	private ApplicationManager clearThManager;
	private Path resDir;

	@BeforeClass
	public void init() throws Exception
	{
		FileUtils.deleteDirectory(TEST_OUTPUT.toFile());
		Files.createDirectory(TEST_OUTPUT);

		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(SchedulerDataTest.class.getSimpleName()));
		clearThManager = new ApplicationManager();
	}

	@AfterClass
	public void dispose() throws IOException
	{
		if (clearThManager != null)
			clearThManager.dispose();
	}

	@Test
	public void testLoadStepsAndReadFromCsvFile() throws Exception
	{
		String stepCfg = resDir.resolve(CFG_FILE).toString();
		List<Step> steps = new ArrayList<>();

		SchedulerData.loadSteps(stepCfg, steps, new DefaultStepFactory(), new ArrayList<>());

		assertThat(steps).usingRecursiveComparison().isEqualTo(createSteps());
	}

	@Test
	public void testSaveStepsWithCsvWriter() throws IOException
	{
		Path configFile = TEST_OUTPUT.resolve(CFG_FILE);
		Files.createDirectory(configFile.getParent());
		Files.createFile(configFile);

		SchedulerData.saveSteps(configFile.toFile(), createHeader(), createSteps());

		String actualFile = FileUtils.readFileToString(configFile.toFile(), Utils.UTF8);
		String expectedFile = FileUtils.readFileToString(resDir.resolve(CFG_FILE).toFile(), Utils.UTF8);
		assertEquals(actualFile, expectedFile);
	}

	@Test
	public void testLoadMatrixData() throws Exception
	{
		SchedulerData schedulerData = new DefaultSchedulerData("data", "cfg",
				"schDir", "matrices", "last", new DefaultStepFactory());

		List<MatrixData> matricesContainer = schedulerData.loadMatrices(resDir.resolve("executed_matrices.csv").toString(),
				resDir.resolve("matrices") + File.separator);

		assertThat(matricesContainer).usingRecursiveComparison().isEqualTo(createMatrixDataList());
	}

	@Test
	public void testSaveMatrixData() throws Exception
	{
		Path matrixFile = TEST_OUTPUT.resolve("matrices").resolve("matrix1.csv");
		Files.createDirectory(matrixFile.getParent());
		Files.createFile(matrixFile);

		SchedulerData schedulerData = new DefaultSchedulerData("data", "cfg",
				"schDir", "matrices", "last", new DefaultStepFactory());

		schedulerData.saveMatrices(matrixFile.toString(), createMatrixDataList());
		String actualData = FileUtils.readFileToString(matrixFile.toFile(), Utils.UTF8);
		String expectedData = StringOperationUtils.multilineString(System.lineSeparator(),
				"Name,Matrix,Uploaded,Execute,TrimSpaces,Link,Type,AutoReload",
				"matrix1,matrix1.csv,,true,false,,,false");
		assertEquals(actualData, expectedData);
	}

	@Test
	public void testLoadReportsConfig() throws IOException
	{
		Path reportsConfigFile = resDir.resolve(REPORTS_DIR_NAME).resolve("reports.json");
		ReportsConfig reportsConfig = SchedulerData.loadReportsConfig(reportsConfigFile);
		
		assertFalse(reportsConfig.isCompleteHtmlReport());
		assertFalse(reportsConfig.isCompleteJsonReport());
		assertFalse(reportsConfig.isFailedHtmlReport());
	}
	
	@Test
	public void testSaveReportsConfig() throws IOException
	{
		Path reportsConfigFile = TEST_OUTPUT.resolve(REPORTS_DIR_NAME).resolve("reports.json");
		
		ReportsConfig reportsConfig = new ReportsConfig(true, false, true);
		SchedulerData.saveReportsConfig(reportsConfig, reportsConfigFile);
		
		String actData = FileUtils.readFileToString(reportsConfigFile.toFile(), Utils.UTF8);
		String expData = FileUtils.readFileToString(resDir.resolve(REPORTS_DIR_NAME).resolve("expected_report.json").toFile(), Utils.UTF8);
		
		assertEquals(actData, expData);
	}
	
	private List<MatrixData> createMatrixDataList()
	{
		MatrixData matrixData = new MatrixData();
		matrixData.setTrim(false);
		matrixData.setExecute(true);
		matrixData.setName("matrix1");
		matrixData.setFile(resDir.resolve("matrices").resolve("matrix1.csv").toFile());
		matrixData.setLink("");
		matrixData.setType("");
		matrixData.setAutoReload(false);
		matrixData.setUploadDate(null);

		List<MatrixData> list = new ArrayList<>();
		list.add(matrixData);
		return list;
	}

	private String[] createHeader()
	{
		return new String[]{"Global step", "Step kind", "Start at", "Start at type", "Wait next day", "Parameter",
				"Ask for continue", "Ask if failed", "Execute", "Comment"};
	}

	private List<Step> createSteps()
	{
		List<Step> steps = new ArrayList<>();

		steps.add(createDefaultStep("Step1"));
		steps.add(createDefaultStep("Step2"));
		steps.add(createDefaultStep("Step3"));

		return steps;
	}

	private Step createDefaultStep(String stepName)
	{
		return new DefaultStep(stepName,"Default","", StartAtType.END_STEP,
				false, "", false, false, true,"");
	}
}