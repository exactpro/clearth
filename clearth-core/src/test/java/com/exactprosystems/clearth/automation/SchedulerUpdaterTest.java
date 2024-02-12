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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ActionUpdateException;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.exceptions.SchedulerUpdateException;
import com.exactprosystems.clearth.helpers.JsonAssert;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;

public class SchedulerUpdaterTest
{
	private Path resourcesPath;
	private ApplicationManager appManager;
	
	@BeforeClass
	public void init() throws FileNotFoundException, ClearThException
	{
		resourcesPath = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath("SchedulerUpdaterTest"));
		appManager = new ApplicationManager();
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (appManager != null)
			appManager.dispose();
	}
	
	@Test
	public void updatedActionsInReport() throws ClearThException, AutomationException, SchedulerUpdateException, ActionUpdateException, IOException
	{
		String userName = "user";
		Scheduler scheduler = appManager.getScheduler("updatedScheduler", userName);
		appManager.loadSteps(scheduler, resourcesPath.resolve("steps.cfg").toFile());
		appManager.loadMatrices(scheduler, resourcesPath.resolve("matrices").toFile());
		
		scheduler.start(userName);
		ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 2000);
		
		MatrixData updatedMatrixData = scheduler.getMatrixDataFactory()
				.createMatrixData("matrix1.csv", resourcesPath.resolve("matrix1_updated.csv").toFile(), new Date(), true, false, null, null, false);
		SchedulerUpdater updater = new SchedulerUpdater(scheduler);
		updater.updateMatrices(List.of(updatedMatrixData));
		
		scheduler.continueExecution();
		ApplicationManager.waitForSchedulerToStop(scheduler, 100, 2000);
		
		List<XmlSchedulerLaunchInfo> launchesInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo();
		Path actualReport = Path.of(ClearThCore.reportsPath(), launchesInfo.get(0).getReportsPath(), "matrix1.csv", "report.json"),
				expectedReport = resourcesPath.resolve("matrix1_report.json");
		
		new JsonAssert().setIgnoredValueNames(SchedulerTest.IGNORED_EXPECTED_PARAMS)
				.assertEquals(expectedReport.toFile(), actualReport.toFile());
	}
}