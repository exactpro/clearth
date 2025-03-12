/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

import static com.exactprosystems.clearth.ApplicationManager.TEST_RES_DIR;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.ActionReportWriter;
import com.exactprosystems.clearth.helpers.JsonAssert;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;

public class FailedActionResultReferenceTest
{
	private static final Path RES_DIR = TEST_RES_DIR.resolve("FailedActionResultReferenceTest"),
			CONFIG_DIR = RES_DIR.resolve("configs");
	
	private ApplicationManager clearThManager;
	
	@BeforeClass
	public void init() throws ClearThException
	{
		clearThManager = ApplicationManager.builder().actionsMappingFile(CONFIG_DIR.resolve("actionsmapping.cfg").toString()).build();
	}
	
	@AfterClass
	public void dispose() throws Exception
	{
		if (clearThManager != null)
			clearThManager.dispose();
	}
	
	@Test
	public void failedResultReferenceTest() throws Exception
	{
		Scheduler scheduler = clearThManager.getScheduler("References", "test");
		clearThManager.loadSteps(scheduler, CONFIG_DIR.resolve("config.cfg").toFile());
		clearThManager.loadMatrices(scheduler, RES_DIR.resolve("matrices").toFile());
		scheduler.start("test");
		ApplicationManager.waitForSchedulerToStop(scheduler, 100, 2000);
		
		assertReports(scheduler.getSchedulerData().getLaunches().getLaunchesInfo(),
				"matrix1.csv",
				RES_DIR.resolve("reports").resolve("matrix1.json"));
	}
	
	
	private void assertReports(List<XmlSchedulerLaunchInfo> launchesInfo, String matrixName, Path expectedReport) throws IOException
	{
		Assert.assertTrue(launchesInfo.size() > 0, "Launches info is not empty");
		
		Path actualReport = Path.of(ClearThCore.reportsPath(), launchesInfo.get(0).getReportsPath(), matrixName, ActionReportWriter.JSON_REPORT_NAME);
		
		new JsonAssert().setIgnoredValueNames(SchedulerTest.IGNORED_EXPECTED_PARAMS)
				.assertEquals(expectedReport.toFile(), actualReport.toFile());
	}
}
