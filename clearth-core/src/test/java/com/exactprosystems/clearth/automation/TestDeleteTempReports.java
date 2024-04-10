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
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.Stopwatch;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.exactprosystems.clearth.ApplicationManager.*;
import static org.testng.Assert.*;

public class TestDeleteTempReports
{
	private static final Path RES_DIR = USER_DIR.resolve("src/test/resources").resolve(TestDeleteTempReports.class.getSimpleName());
	private static final Path MATRICES_DIR = RES_DIR.resolve("matrices");
	private static final Path CONFIG_DIR = RES_DIR.resolve("configs");
	private static final String USER = "user1";
	private ApplicationManager clearThManager;
	
	
	@BeforeClass
	public void init() throws ClearThException
	{
		clearThManager = new ApplicationManager();
		
		List<Scheduler> userSchedulers = ClearThCore.getInstance().getSchedulersManager().getUserSchedulers(USER);
		if (userSchedulers != null)
			userSchedulers.clear();
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (clearThManager != null)
			clearThManager.dispose();
	}
	
	@Test
	public void testClearActionsAndCompletedReports() throws ClearThException, IOException, AutomationException, InterruptedException
	{
		Scheduler scheduler = createScheduler("config_actions_delete.cfg");

		scheduler.start(USER);
		waitForSchedulerToSuspend(scheduler, 100, 2000);
		
		File actionRepDir = new File(scheduler.getActionReportsDir()),
				completedDir = new File(scheduler.getCompletedReportsDir());
		assertTrue(actionRepDir.exists());
		assertFalse(completedDir.exists());
		
		scheduler.continueExecution();
		waitForSchedulerToStop(scheduler, 100, 2000);
		
		waitForPathToDelete(3000, actionRepDir);
		waitForPathExists(3000, completedDir);
	}
	
	@Test
	public void testClearCurrentReports() throws ClearThException, IOException, AutomationException, InterruptedException
	{
		Scheduler scheduler = createScheduler("config_make_current_reports.cfg");
		
		scheduler.start(USER);
		waitForSchedulerToSuspend(scheduler, 100, 1000);
		
		File repDir1 = new File(scheduler.makeCurrentReports(scheduler.getReportsDir() + "current_1", false, true).getPath());
		assertTrue(repDir1.exists());
		
		scheduler.continueExecution();
		waitForSchedulerToSuspend(scheduler, 100, 1000);
		
		File repDir2 = new File(scheduler.makeCurrentReports(scheduler.getReportsDir() + "current_2", false, true).getPath());
		assertTrue(repDir1.exists());
		assertTrue(repDir2.exists());
		
		scheduler.continueExecution();
		waitForSchedulerToStop(scheduler, 100, 2000);
		
		waitForPathToDelete(3000, repDir1);
		waitForPathToDelete(3000, repDir2);
	}
	
	@Test
	public void testCurrentStateReports() throws ClearThException, IOException, AutomationException, InterruptedException
	{
		Scheduler scheduler = createScheduler("config_save_state.cfg");
		
		scheduler.start(USER);
		waitForSchedulerToSuspend(scheduler, 100, 3000);
		
		scheduler.saveState();
		File currentState = new File(scheduler.getStateInfo().getReportsInfo().getPath());
		assertTrue(currentState.exists());
		
		scheduler.continueExecution();
		waitForSchedulerToStop(scheduler, 100, 2000);
		
		waitForPathExists(3000, currentState);
	}
	
	@Test
	public void testCurrentReportsAndSavedStateReports() throws ClearThException, IOException, AutomationException, InterruptedException
	{
		Scheduler scheduler = createScheduler("config_save_state.cfg");
		
		scheduler.start(USER);
		waitForSchedulerToSuspend(scheduler, 100, 3000);
		
		File currentRep = new File(scheduler.makeCurrentReports(scheduler.getReportsDir() + "current_rep", false, true).getPath());
		assertTrue(currentRep.exists());
		
		scheduler.saveState();
		File currentState = Path.of(scheduler.getStateInfo().getReportsInfo().getPath()).getParent().resolve("current_state").toFile();
		assertTrue(currentState.exists());
		
		scheduler.continueExecution();
		waitForSchedulerToStop(scheduler, 100, 4000);
		
		waitForPathToDelete(3000, currentRep);
		waitForPathExists(3000, currentState);
	}
	
	private void waitForPathToDelete(long timeout, File checkFile) throws InterruptedException
	{
		Stopwatch s = Stopwatch.createAndStart(timeout);
		while (!s.isExpired())
		{
			if (!checkFile.exists())
				return;
			Thread.sleep(100);
		}
		fail("Too long waiting for check if reports path '" + checkFile.getName() + "' is deleted");
	}
	
	private void waitForPathExists(long timeout, File checkFile) throws InterruptedException
	{
		Stopwatch s = Stopwatch.createAndStart(timeout);
		while (!s.isExpired())
		{
			if (!checkFile.exists())
				fail("Reports path '" + checkFile.getName() + "' does not exist");
			Thread.sleep(100);
		}
	}
	
	private Scheduler createScheduler(String cfgFile) throws ClearThException, IOException
	{
		Scheduler scheduler = clearThManager.getScheduler(USER, USER);
		scheduler.clearSteps();
		clearThManager.loadSteps(scheduler, CONFIG_DIR.resolve(cfgFile).toFile());
		clearThManager.loadMatrices(scheduler, MATRICES_DIR.toFile());
		
		return scheduler;
	}
}
