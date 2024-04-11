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

package com.exactprosystems.clearth.automation.report;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplatesProcessor;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import freemarker.template.TemplateModelException;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

public class ReportsWriterTest
{
	private static final String USER = "user", FAILED_MATRIX_NAME = "test1_failed.csv", PASSED_MATRIX_NAME = "test2_passed.csv", STEP = "Step1";
	private static final Path TEST_OUTPUT = USER_DIR.resolve("testOutput").resolve(ReportsWriterTest.class.getSimpleName());
	private ApplicationManager manager;
	private Path resDir;
	private Scheduler scheduler;
	
	@BeforeClass
	public void init() throws ClearThException, IOException, SettingsException
	{
		manager = new ApplicationManager();
		resDir = Path.of(FileOperationUtils.resourceToAbsoluteFilePath(ReportsWriterTest.class.getSimpleName()));
		scheduler = manager.getScheduler(USER, USER);
		
		FileUtils.deleteDirectory(TEST_OUTPUT.toFile());
		Files.createDirectories(TEST_OUTPUT);
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (manager != null)
			manager.dispose();
	}
	
	@DataProvider(name = "reportsConfig")
	public Object[][] reportsConfig()
	{
		return new Object[][]
		{
			{ new ReportsConfig(false, false, false), TEST_OUTPUT.resolve("1")},
			{ new ReportsConfig(true, false, false), TEST_OUTPUT.resolve("2")},
			{ new ReportsConfig(true, true, false), TEST_OUTPUT.resolve("3")},
			{ new ReportsConfig(true, true, true), TEST_OUTPUT.resolve("4")},
			{ new ReportsConfig(true, false, true), TEST_OUTPUT.resolve("5")},
			{ new ReportsConfig(false, true, true), TEST_OUTPUT.resolve("6")},
			{ new ReportsConfig(false, false, true), TEST_OUTPUT.resolve("7")},
			{ new ReportsConfig(false, true, false), TEST_OUTPUT.resolve("8")}
		};
	}
	
	@Test(dataProvider = "reportsConfig")
	public void testBuildAndWriteReports(ReportsConfig repCfg, Path repDirName)
			throws IOException, ReportException, TemplateModelException
	{
		List<Step> steps = Collections.singletonList(createStep(STEP));
		
		SimpleExecutor executor = new DefaultSimpleExecutor(scheduler, steps, Collections.emptyList(),
				new GlobalContext(new Date(), false, Collections.emptyMap(), null, USER, null), null, null, repCfg);
		
		Matrix matrixFailed = createMatrix(FAILED_MATRIX_NAME, false);
		Matrix matrixPassed = createMatrix(PASSED_MATRIX_NAME, true);
		List<String> stepFiles = List.of(STEP, STEP + "_failed", STEP + ".json");
		
		ReportsWriter writer = new ReportsWriter(executor, repDirName.toString(), resDir.resolve("actions").toString(), repCfg, createTemplatesProcessor());
		writer.buildAndWriteReports(matrixFailed, stepFiles, USER, "");
		writer.buildAndWriteReports(matrixPassed, stepFiles, USER, "");
		
		boolean html = repCfg.isCompleteHtmlReport(),
				failed = repCfg.isFailedHtmlReport(),
				json = repCfg.isCompleteJsonReport();
		
		AssertReports.assertAllReports(repDirName.resolve(FAILED_MATRIX_NAME), resDir.resolve("expected_failed"), html, failed, json);
		AssertReports.assertAllReports(repDirName.resolve(PASSED_MATRIX_NAME), resDir.resolve("expected_passed"), html, failed, json);
	}
	
	private Matrix createMatrix(String matrixName, boolean successful)
	{
		Matrix matrix = new Matrix(new MvelVariablesFactory(null, null));
		matrix.setName(matrixName);
		matrix.setFileName(matrixName);
		matrix.setSuccessful(successful);
		matrix.setStepSuccessful(STEP, successful);
		return matrix;
	}
	
	private Step createStep(String stepName)
	{
		Step step = new DefaultStep();
		step.setSafeName(stepName);
		step.setName(stepName);
		step.setKind(CoreStepKind.Default.getLabel());
		step.setExecute(true);
		step.setFinished(new Date());
		step.setExecutionProgress(new ActionsExecutionProgress(1, 1));
		return step;
	}
	
	private ReportTemplatesProcessor createTemplatesProcessor() throws TemplateModelException, IOException
	{
		return new ReportTemplatesProcessor(resDir.resolve("templates"));
	}
}