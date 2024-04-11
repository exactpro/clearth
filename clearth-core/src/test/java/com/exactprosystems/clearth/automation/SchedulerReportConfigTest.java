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
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.data.DataHandlingException;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.xmldata.XmlReportsConfig;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;
import static com.exactprosystems.clearth.ApplicationManager.waitForSchedulerToStop;
import static org.testng.Assert.*;

public class SchedulerReportConfigTest
{
	private static final String REPORTS_DIR = USER_DIR.getParent().resolve(ApplicationManager.TEST_REPORT_DIR)
			.resolve("automation").resolve("reports").toString();
	private static final String USER = "user";
	
	private ApplicationManager clearThManager;
	private Path resDir,
			configsDir;
	
	@BeforeClass
	public void init() throws Exception
	{
		clearThManager = new ApplicationManager();
		resDir = Path.of(FileOperationUtils.resourceToAbsoluteFilePath(SchedulerReportConfigTest.class.getSimpleName()));
		configsDir = resDir.resolve("configs");
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (clearThManager != null)
			clearThManager.dispose();
	}
	
	@DataProvider(name = "reportsConfig")
	public Object[][] reportsConfig()
	{
		return new Object[][]
		{
			{ new ReportsConfig(false, false, false) },
			{ new ReportsConfig(true, false, false) },
			{ new ReportsConfig(true, true, false) },
			{ new ReportsConfig(true, true, true) },
			{ new ReportsConfig(true, false, true) },
			{ new ReportsConfig(false, true, true) },
			{ new ReportsConfig(false, false, true) },
			{ new ReportsConfig(false, true, false) }
		};
	}
	
	@Test(dataProvider = "reportsConfig")
	public void testSchedulerReportsConfiguration(ReportsConfig reportsConfig) throws Exception
	{
		Scheduler scheduler = createScheduler(resDir.resolve("matrices"), configsDir.resolve("config.cfg"));
		scheduler.setReportsConfig(reportsConfig);
		scheduler.start(USER);
		waitForSchedulerToStop(scheduler, 100, 5000);
		
		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		assertNotNull(launchesInfo, "Launches data");
		assertFalse(launchesInfo.isEmpty());
		
		XmlSchedulerLaunchInfo currentLaunch = launchesInfo.get(0);
		Path repOutput = Paths.get(REPORTS_DIR, currentLaunch.getReportsPath());
		
		assertReportsConfigs(currentLaunch.getReportsConfig(), reportsConfig);
		checkReports(repOutput, reportsConfig);
	}
	
	@Test
	public void testReportsConfigState() throws ClearThException, IOException, AutomationException, 
			IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, DataHandlingException
	{
		ReportsConfig initialConfig = new ReportsConfig(false, true, true);
		
		Scheduler scheduler = createScheduler(resDir.resolve("matrices_for_state"), configsDir.resolve("config_for_state.cfg"));
		scheduler.setReportsConfig(initialConfig);
		scheduler.start(USER);
		
		ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 3000);
		scheduler.saveState();
		scheduler.stop();
		ApplicationManager.waitForSchedulerToStop(scheduler, 100, 2000);
		
		scheduler.setReportsConfig(new ReportsConfig(true, true, true));  //This new configuration should not affect reports written after restoring execution state
		scheduler.restoreState(USER);
		ApplicationManager.waitForSchedulerToStop(scheduler, 100, 5000);
		
		XmlSchedulerLaunchInfo currentLaunch = scheduler.getSchedulerData().getLaunches().getLaunchesInfo().get(0);
		Path repOutput = Paths.get(REPORTS_DIR, currentLaunch.getReportsPath());
		checkReports(repOutput, initialConfig);
	}
	
	
	private void assertReportsConfigs(XmlReportsConfig xmlReportsConfig, ReportsConfig reportsConfig)
	{
		assertEquals(xmlReportsConfig.isCompleteHtmlReport(), reportsConfig.isCompleteHtmlReport());
		assertEquals(xmlReportsConfig.isFailedHtmlReport(), reportsConfig.isFailedHtmlReport());
		assertEquals(xmlReportsConfig.isCompleteJsonReport(), reportsConfig.isCompleteJsonReport());
	}
	
	private Scheduler createScheduler(Path matricesDir, Path stepsFile) throws ClearThException
	{
		Scheduler scheduler = clearThManager.getScheduler(USER, USER);
		clearThManager.loadSteps(scheduler, stepsFile.toFile());
		clearThManager.loadMatrices(scheduler, matricesDir.toFile());
		return scheduler;
	}
	
	private void checkReports(Path reportPath, ReportsConfig reportsConfig) throws IOException
	{
		boolean json = reportsConfig.isCompleteJsonReport(),
				html = reportsConfig.isCompleteHtmlReport(),
				failed_html = reportsConfig.isFailedHtmlReport();
		if (!Files.exists(reportPath))
		{
			assertTrue(!json && !html && !failed_html);
			return;
		}
		
		try (Stream<Path> stream = Files.list(reportPath))
		{
			Iterator<Path> reportPaths = stream.iterator();
			while (reportPaths.hasNext())
			{
				Path path = reportPaths.next();
				assertEquals(Files.exists(path.resolve("report.json")), json, "Complete JSON report exists");
				assertEquals(Files.exists(path.resolve("report.html")), html, "Complete HTML report exists");
				assertEquals(Files.exists(path.resolve("report_failed.html")), failed_html, "Failed HTML report exists");
			}
		}
	}
}
