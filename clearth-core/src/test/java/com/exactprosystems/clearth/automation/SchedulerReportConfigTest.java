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
	
	private Path resDir;
	private ApplicationManager clearThManager;
	private Scheduler scheduler;
	@BeforeClass
	public void init() throws Exception
	{
		clearThManager = new ApplicationManager();
		resDir = Path.of(FileOperationUtils.resourceToAbsoluteFilePath(SchedulerReportConfigTest.class.getSimpleName()));
		scheduler = createScheduler();
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
	
	private void assertReportsConfigs(XmlReportsConfig xmlReportsConfig, ReportsConfig reportsConfig)
	{
		assertEquals(xmlReportsConfig.isCompleteHtmlReport(), reportsConfig.isCompleteHtmlReport());
		assertEquals(xmlReportsConfig.isFailedHtmlReport(), reportsConfig.isFailedHtmlReport());
		assertEquals(xmlReportsConfig.isCompleteJsonReport(), reportsConfig.isCompleteJsonReport());
	}
	
	private Scheduler createScheduler() throws ClearThException
	{
		Scheduler scheduler = clearThManager.getScheduler(USER, USER);
		File stepCfg = clearThManager.getSchedulerConfig(resDir.resolve("configs").resolve("config.cfg"));
		clearThManager.loadSteps(scheduler, stepCfg);
		clearThManager.loadMatrices(scheduler, resDir.resolve("matrices").toFile());
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
				assertEquals(Files.exists(path.resolve("report.json")), json);
				assertEquals(Files.exists(path.resolve("report.html")), html);
				assertEquals(Files.exists(path.resolve("report_failed.html")), failed_html);
			}
		}
	}
}
