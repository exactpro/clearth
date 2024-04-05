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

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.actions.Compare2Values;
import com.exactprosystems.clearth.automation.report.ActionReportWriter;
import com.exactprosystems.clearth.automation.report.AssertReports;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.automation.report.html.template.ReportTemplatesProcessor;
import com.exactprosystems.clearth.data.DefaultTestExecutionHandler;
import com.exactprosystems.clearth.generators.IncrementingValueGenerator;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.CollectionUtils;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;

import freemarker.template.TemplateModelException;

public class ActionExecutorTest
{
	private final Path TEST_OUTPUT = USER_DIR.resolve("testOutput").resolve(ActionExecutorTest.class.getSimpleName());
	private ApplicationManager manager;
	private Path resourcesPath;
	
	@BeforeClass
	public void init() throws ClearThException, IOException
	{
		manager = new ApplicationManager();  //Required during action parameters calculation triggered from actionExecutor.executeAction()
		resourcesPath = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(ActionExecutorTest.class.getSimpleName()));
		
		FileUtils.deleteDirectory(TEST_OUTPUT.toFile());
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
	public void reportsWritingTest(ReportsConfig reportsConfig, Path outputDir) throws TemplateModelException, IOException
	{
		GlobalContext gc = new GlobalContext(new Date(), false, Collections.emptyMap(), null, "user", new DefaultTestExecutionHandler());
		MatrixFunctions mf = new MatrixFunctions(gc.getHolidays(), gc.getCurrentDate(), null, false, new IncrementingValueGenerator(0));
		ActionParamsCalculator calc = new ActionParamsCalculator(mf);
		ReportTemplatesProcessor templatesProcessor = new ReportTemplatesProcessor(resourcesPath.resolve("templates"));
		ActionReportWriter reportWriter = new ActionReportWriter(reportsConfig, templatesProcessor);
		
		String matrixName = "matrix1.csv",
				stepName = "Step1";
		Matrix matrix = createMatrix(matrixName);
		Step step = createStep(stepName);
		Action passed = createAction(matrix, step, "id1", "1", "1"),
				failed = createAction(matrix, step, "id2", "1", "2"),
				notExecuted = createAction(matrix, step, "id3", "1", "1");
		//This action is not executable due to expression in #Execute parameter and must be written to complete HTML report and failed HTML report
		notExecuted.formulaExecutable = "@{false}";
		List<Action> actions = Arrays.asList(passed, failed, notExecuted);
		
		ActionExecutor exec = new ActionExecutor(gc, calc, reportWriter, new FailoverStatus(), true, Collections.emptySet());
		try
		{
			executeActions(exec, outputDir, step, actions);
		}
		finally
		{
			Utils.closeResource(exec);
		}
		
		
		Path actualFilesDir = outputDir.resolve(matrixName),
				expectedFilesDir = resourcesPath.resolve("files");
		String stepFailed = stepName+"_failed",
				stepJson = stepName+".json";
		
		AssertReports.assertCompleteHtmlReports(actualFilesDir.resolve(stepName),
				expectedFilesDir.resolve(stepName),
				reportsConfig.isCompleteHtmlReport());
		
		AssertReports.assertFailedHtmlReports(actualFilesDir.resolve(stepFailed),
				expectedFilesDir.resolve(stepFailed),
				reportsConfig.isFailedHtmlReport());
		
		AssertReports.assertCompleteJsonReports(actualFilesDir.resolve(stepJson),
				expectedFilesDir.resolve(stepJson),
				reportsConfig.isCompleteJsonReport());
	}
	
	private Matrix createMatrix(String matrixName)
	{
		Matrix matrix = new Matrix(new MvelVariablesFactory(null, null));
		matrix.setName(matrixName);
		matrix.setFileName(matrixName);
		matrix.getMvelVars().setCleaningTable(new HashSetValuedHashMap<>());
		return matrix;
	}
	
	private Step createStep(String stepName)
	{
		Step step = new DefaultStep();
		step.setSafeName(stepName);
		step.setName(stepName);
		step.setKind(CoreStepKind.Default.getLabel());
		step.setExecute(true);
		return step;
	}
	
	private Action createAction(Matrix matrix, Step step, String id, String expectedValue, String actualValue)
	{
		ActionSettings settings = new ActionSettings();
		settings.setMatrix(matrix);
		settings.setStep(step);
		settings.setActionId(id);
		settings.setParams(CollectionUtils.map("Expected", expectedValue, "Actual", actualValue));
		settings.setMatrixInputParams(settings.getParams().keySet());
		
		Action result = new Compare2Values();
		result.preInit(null, "Compare2Values", Collections.emptyMap());
		result.init(settings);
		return result;
	}
	
	private void executeActions(ActionExecutor exec, Path outputDir, Step step, List<Action> actions)
	{
		exec.reset(outputDir.toString()+"/", new ActionsExecutionProgress());
		
		StepContext sc = new StepContext(step.getName(), new Date());
		AtomicBoolean canReplay = new AtomicBoolean(false);
		for (Action a : actions)
		{
			exec.prepareToAction(a);
			exec.executeAction(a, sc, canReplay);
		}
		exec.afterActionsExecution(step);
	}
}