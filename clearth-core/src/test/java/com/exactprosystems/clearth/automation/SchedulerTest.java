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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;

import com.exactprosystems.clearth.helpers.JsonAssert;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;
import static com.exactprosystems.clearth.ApplicationManager.waitForSchedulerToStop;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class SchedulerTest
{
	private static final Path TEST_CONFIG_DIR = USER_DIR.resolve("src/test/resources/SchedulerTestData");
	private static final String REPORTS_DIR = USER_DIR.getParent().resolve(ApplicationManager.TEST_REPORT_DIR)
			.resolve("automation").resolve("reports").toString();

	public static final Set<String> IGNORED_EXPECTED_PARAMS =
			new HashSet<String>(Arrays.asList("version", "userName", "matrixName", "host", "executionStart", "executionEnd", 
					"executionTime", "started", "finished"));

	private final String userName = "test", schedulerName = "Test";

	private static ApplicationManager clearThManager;

	@Parameterized.Parameter
	public Path testConfigPath;

	@Parameterized.Parameters(name = "config: {0}")
	public static Iterable<?> data() throws IOException
	{
		try (Stream<Path> list = Files.list(TEST_CONFIG_DIR))
		{
			return list
					.filter(f -> f.toFile().isDirectory())
					.sorted()
					.collect(Collectors.toList());
		}
	}

	@Test
	public void executeTest() throws ClearThException, AutomationException
	{
		Scheduler scheduler = clearThManager.getScheduler(schedulerName, userName);
		List<String> warnings = loadStepsForExecuteTest(scheduler);
		if (!warnings.isEmpty())
			throw new AutomationException("Steps loading errors:" + warnings);
		loadMatricesForExecuteTest(scheduler);

		scheduler.start(userName);
		waitForSchedulerToStop(scheduler, 100, 10000);

		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		if (launchesInfo == null || launchesInfo.isEmpty())
			throw new ClearThException("Launches data is not found");

		XmlSchedulerLaunchInfo currentLaunch = launchesInfo.get(0);
		allSuccessVerify(currentLaunch, scheduler.getExecutedMatricesPath());
		verifyStepsStatuses(scheduler.getSteps());
		checkReports(currentLaunch.getReportsPath());
	}

	private void verifyStepsStatuses(List<Step> steps)
	{
		for (Step step : steps)
		{
			Result result = step.getResult();
			String msg = format("Step '%s' with kind '%s'", step.getName(), step.getKind());
			if (result != null)
			{
				assertEquals(format("%s has unexpected inner result '%s' status", msg, result.isSuccess() ? "Success" : "Failed"),
						result.isSuccess(), !step.isFailedDueToError());
			}
			ActionsExecutionProgress stepExecProgress = step.getExecutionProgress();
			assertEquals(format("%s has wrong actions execution progress", msg),
					step.getFinished() != null && stepExecProgress.getSuccessful() == stepExecProgress.getDone(), 
					!step.isAnyActionFailed() && !step.isFailedDueToError());
		}
	}

	@BeforeClass
	public static void startTestApplication() throws ClearThException
	{
		clearThManager = new ApplicationManager();
	}

	@After
	public void clearSchedulerData()
	{
		SchedulersManager manager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> userSchedulers = manager.getUserSchedulers(userName);
		userSchedulers.clear();
	}

	@AfterClass
	public static void disposeTestApplication() throws IOException
	{
		if (clearThManager != null) clearThManager.dispose();
	}

	protected List<String> loadStepsForExecuteTest(Scheduler scheduler) throws ClearThException
	{
		File stepCfg = clearThManager.getSchedulerConfig(testConfigPath.resolve("configs/config.cfg"));
		return clearThManager.loadSteps(scheduler, stepCfg);
	}

	private void loadMatricesForExecuteTest(Scheduler scheduler) throws ClearThException
	{
		clearThManager.loadMatrices(scheduler, testConfigPath.resolve("matrices").toFile());
	}

	private void allSuccessVerify(XmlSchedulerLaunchInfo launch, Path executedMatricesPath)
	{
		assertNotNull("Scheduler launch info is not available", launch);
		assertTrue("Scheduler launch info has not-success status", launch.isSuccess());
		assertTrue("Reports by last launch are not generated", checkLastReportDir(launch.getReportsPath()));
		assertTrue("Last executed matrices are not generated", checkLastExecutedMatrices(executedMatricesPath));
	}

	private boolean checkLastReportDir(String lastReportPath)
	{
		if (lastReportPath.isEmpty())
			return false;
		Path reportPath = Paths.get(ClearThCore.getInstance().getReportsPath()).resolve(lastReportPath);

		try (Stream<Path> list = Files.list(reportPath))
		{
			return Files.exists(reportPath) && list.count() > 0;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	private boolean checkLastExecutedMatrices(Path executedMatricesPath)
	{
		File executedMatricesFile = executedMatricesPath.getParent().resolve(ExecutedMatricesData.EXECUTED_MATRICES_FILENAME).toFile();
		if (!executedMatricesFile.exists() || !Files.exists(executedMatricesPath))
			return false;

		try (Stream<Path> list = Files.list(executedMatricesPath))
		{
			return list.count() > 0;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	private void checkReports(String reportPath) throws ClearThException
	{
		File[] reportPaths = Paths.get(REPORTS_DIR, reportPath).toFile().listFiles(File::isDirectory);
		assertNotNull("Report paths is empty", reportPaths);

		for (File path : reportPaths)
		{
			String fileName = FilenameUtils.removeExtension(path.getName());

			File actualReport = path.toPath().resolve("report.json").toFile();
			File expectedReport = getExpectedReport(fileName);
			try
			{
				new JsonAssert().setIgnoredValueNames(IGNORED_EXPECTED_PARAMS).assertEquals(expectedReport, actualReport);
			}
			catch (IOException e)
			{
				throw new ClearThException(e);
			}
		}
	}

	private File getExpectedReport(String fileName)
	{
		Path reports = testConfigPath.resolve("reports");
		File[] files = reports.toFile().listFiles((dir, name) -> name.equals(fileName + "_report.json"));
		assertNotNull("Actual report file not found", files);

		return files[0];
	}
}
