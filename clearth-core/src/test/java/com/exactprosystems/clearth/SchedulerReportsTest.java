/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class SchedulerReportsTest
{
	private final String userName = "reportChecker", schedulerName = "ReportChecker";

	private static final Path TEST_CONFIG_DIR = USER_DIR.resolve("src/test/resources/SchedulerReports/NotContainedStrings");
	private static final Path REPORTS_DIR = USER_DIR.getParent().resolve(ApplicationManager.TEST_REPORT_DIR).resolve("automation").resolve("reports");

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
	public void shouldNotContainReportData() throws ClearThException, AutomationException, IOException
	{
		Scheduler scheduler = clearThManager.getScheduler(schedulerName, userName);
		loadStepsForExecuteTest(scheduler);
		loadMatricesForExecuteTest(scheduler);
		scheduler.start(userName);
		waitForSchedulerToStop(scheduler, 100, 10000);
		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		if (launchesInfo == null || launchesInfo.isEmpty())
		{
			throw new ClearThException("Launches data is not found");
		}

		checkReports(launchesInfo.get(0));
	}

	@BeforeClass
	public static void startTestApp() throws ClearThException
	{
		clearThManager = new ApplicationManager();
	}

	@After
	public void clearSchedulerData()
	{
		SchedulersManager manager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> userSchedulers = manager.getUserSchedulers(schedulerName);
		userSchedulers.clear();
	}

	@AfterClass
	public static void disposeTestApp() throws IOException
	{
		if (clearThManager != null) clearThManager.dispose();
	}

	private void checkReports(XmlSchedulerLaunchInfo launchInfo) throws IOException
	{
		File[] reportDirs = getReportDirs(launchInfo);

		for (File reportDir : reportDirs)
		{
			File jsonReport = reportDir.toPath().resolve("report.json").toFile();
			verifyReport(jsonReport);
			File htmlReport = reportDir.toPath().resolve("report.html").toFile();
			verifyReport(htmlReport);
		}
	}

	private void verifyReport(File report) throws IOException
	{
		String extension = FilenameUtils.getExtension(report.getName());

		try (BufferedReader reader = new BufferedReader(new FileReader(report)))
		{
			List<String> stringsForSearch = getStringsForSearch();

			String line;
			boolean lineIsContains = false;
			while ((line = reader.readLine()) != null)
			{
				for (String forSearch : stringsForSearch)
				{
					lineIsContains = line.contains(getReportDataCommentBlock(forSearch, extension));
					if (lineIsContains)
						break;
				}
				if (lineIsContains)
					break;
			}
			assertFalse("Report shouldn't contain pre/post report data", lineIsContains);
		}
	}

	private String getReportDataCommentBlock(String commentBlock, String extension)
	{
		switch (extension)
		{
			case "html": return "<!-- " + commentBlock + " -->";
			case "json": return "/* " + commentBlock + " */";
			default: return "";
		}
	}

	private File[] getReportDirs(XmlSchedulerLaunchInfo launchInfo)
	{
		return REPORTS_DIR.resolve(launchInfo.getReportsPath()).toFile().listFiles(File::isDirectory);
	}

	private void loadStepsForExecuteTest(Scheduler scheduler) throws ClearThException
	{
		File stepCfg = clearThManager.getSchedulerConfig(testConfigPath.resolve("configs/config.cfg"));
		clearThManager.loadSteps(scheduler, stepCfg);
	}

	private void loadMatricesForExecuteTest(Scheduler scheduler) throws ClearThException
	{
		clearThManager.loadMatrices(scheduler, testConfigPath.resolve("matrices").toFile());
	}

	private List<String> getStringsForSearch() throws IOException
	{
		File stringsFile = testConfigPath.resolve("configs/strings.cfg").toFile();

		List<String> result = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(stringsFile)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				result.add(line);
			}

			return result;
		}
	}

	private static void waitForSchedulerToStop(Scheduler scheduler, long delay, long timeout)
	{
		try
		{
			Stopwatch s = Stopwatch.createAndStart(timeout);
			while (scheduler.isRunning())
			{
				if (s.isExpired())
					fail("Too long wait for Scheduler to finish.");

				TimeUnit.MILLISECONDS.sleep(delay);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			fail("Waiting for Scheduler stop is interrupted.");
		}
	}
}
