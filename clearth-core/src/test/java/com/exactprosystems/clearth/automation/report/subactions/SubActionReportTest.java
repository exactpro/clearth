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

package com.exactprosystems.clearth.automation.report.subactions;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulerTest;
import com.exactprosystems.clearth.automation.TestActionUtils;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.helpers.JsonAssert;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;

public class SubActionReportTest
{
	private static final Path ALL_REPORTS_DIR = USER_DIR.getParent().resolve(ApplicationManager.TEST_REPORT_DIR)
			.resolve("automation").resolve("reports");
	
	private Path dataDir;
	private ApplicationManager clearThManager;
	private Set<String> customActions;
	
	@BeforeClass
	public void init() throws Exception
	{
		dataDir = Path.of(FileOperationUtils.resourceToAbsoluteFilePath(SubActionReportTest.class.getSimpleName()));
		clearThManager = new ApplicationManager();
		customActions = TestActionUtils.addCustomActions(dataDir.resolve("actionsmapping.cfg")).keySet();
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (customActions != null)
			TestActionUtils.removeCustomActions(customActions);
		if (clearThManager != null)
			clearThManager.dispose();
	}
	
	@Test
	public void subActionCommentTest() throws ClearThException, IOException, AutomationException
	{
		Path commentDir = dataDir.resolve("comment");
		Scheduler scheduler = TestActionUtils.runScheduler(clearThManager, ApplicationManager.ADMIN, ApplicationManager.ADMIN, 
				commentDir.resolve("config.cfg"), 
				commentDir.resolve("matrices"),
				3000);
		
		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		Path actualReport = ALL_REPORTS_DIR.resolve(launchesInfo.get(0).getReportsPath()).resolve("subActionComment.csv").resolve("report.json"),
				expectedReport = commentDir.resolve("report.json");
		
		new JsonAssert().setIgnoredValueNames(SchedulerTest.IGNORED_EXPECTED_PARAMS)
				.assertEquals(expectedReport.toFile(), actualReport.toFile());
	}
}
