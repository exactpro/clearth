/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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
package com.exactprosystems.clearth;

import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class SchedulerTest
{
	private static final Path TEST_CONFIG_DIR = USER_DIR.resolve("src/test/resources/SchedulerTestData");

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

		try
		{
			while (scheduler.isRunning())
			{
				TimeUnit.SECONDS.sleep(1);
			}
		}
		catch (InterruptedException e)
		{
			throw new ClearThException(e);
		}

		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		if (launchesInfo == null || launchesInfo.isEmpty())
			throw new ClearThException("Launches data is not found");

		allSuccessVerify(launchesInfo.get(0));
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

	private void allSuccessVerify(XmlSchedulerLaunchInfo launch)
	{
		assertNotNull("Scheduler launch info is not available", launch);
		assertTrue("Scheduler launch info has not-success status", launch.isSuccess());
		assertTrue("Reports by last launch are not generated", checkLastReportDir(launch.getReportsPath()));
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
}
