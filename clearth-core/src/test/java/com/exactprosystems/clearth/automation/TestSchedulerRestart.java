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

import static com.exactprosystems.clearth.ApplicationManager.ADMIN;
import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;

import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.persistence.StateConfig;
import com.exactprosystems.clearth.data.DataHandlingException;
import com.exactprosystems.clearth.helpers.JsonAssert;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;

public class TestSchedulerRestart
{
	private static final Path TEST_CONFIG_DIR = USER_DIR.resolve("src/test/resources/Scheduler/TestRestart"),
			SIMPLE_RESTART_DATA = TEST_CONFIG_DIR.resolve("SimpleRestart");
	private static final String SCHEDULER = "RestartingScheduler",
			CONFIG_DIR = "configs",
			CONFIG_FILE = "config.cfg",
			MATRICES_DIR = "matrices",
			STEP1 = "Step1";
	private static final List<String> STEPS = Arrays.asList(STEP1, "Step2");
	private static final long BASE_SLEEP_LENGTH = 10;

	private ApplicationManager clearThManager;
	
	@BeforeClass
	public void startTestApp() throws ClearThException
	{
		clearThManager = new ApplicationManager();
	}

	@AfterMethod
	public void clearSchedulerData()
	{
		SchedulersManager manager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> userSchedulers = manager.getUserSchedulers(ADMIN);
		userSchedulers.clear();
	}

	@AfterClass
	public void disposeTestApp() throws IOException
	{
		if (clearThManager != null) 
			clearThManager.dispose();
	}
	
	@DataProvider(name = "dataForAutoSave")
	public Object[][] dataForAutoSaveProvider()
	{
		return new Object[][] {
			{ TEST_CONFIG_DIR.resolve("RestartWithAutoSave"), 13 },
			{ TEST_CONFIG_DIR.resolve("RestartAfterPause"), 5 },
			{ TEST_CONFIG_DIR.resolve("RestartAfterPauseStepContexts"), 5 }
		};
	}
	
	
	@Test
	public void testRestart() throws ClearThException, AutomationException, IOException, IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, DataHandlingException
	{
		Scheduler scheduler = clearThManager.getScheduler(SCHEDULER, ADMIN);
		scheduler.clearSteps();
		clearThManager.loadSteps(scheduler, SIMPLE_RESTART_DATA.resolve(CONFIG_DIR).resolve(CONFIG_FILE).toFile());
		clearThManager.loadMatrices(scheduler, SIMPLE_RESTART_DATA.resolve(MATRICES_DIR).toFile());
		
		scheduler.start(ADMIN);
		ApplicationManager.waitForSchedulerToSuspend(scheduler, BASE_SLEEP_LENGTH, 1000);
		
		Assert.assertEquals(scheduler.getCurrentStep().getName(), STEP1);
		Assert.assertTrue(scheduler.isSuspended());
		
		boolean stateSaved = scheduler.saveState();
		Assert.assertTrue(stateSaved);
		
		scheduler.stop();
		ApplicationManager.waitForSchedulerToStop(scheduler, BASE_SLEEP_LENGTH, 1000);
		
		Assert.assertTrue(scheduler.isStoppedByUser());
		Assert.assertTrue(scheduler.isInterrupted());
		
		boolean restored = scheduler.restoreState(ADMIN);
		Assert.assertTrue(restored);
		
		ApplicationManager.waitForSchedulerToStop(scheduler, BASE_SLEEP_LENGTH, 1000);
		
		List<Step> schedulerSteps = scheduler.getSteps();
		Assert.assertEquals(STEPS.size(), schedulerSteps.size());
		for (int i = 0; i < STEPS.size(); ++i)
		{
			Assert.assertEquals(STEPS.get(i), schedulerSteps.get(i).getName());
			Assert.assertTrue(stepIsCorrect(schedulerSteps.get(i)));
		}
	}
	
	@Test(dataProvider = "dataForAutoSave")
	public void testRestartWithAutoSave(Path dataDir, int expectedActions) throws ClearThException, AutomationException, IOException, IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, DataHandlingException
	{
		Scheduler scheduler = clearThManager.getScheduler(SCHEDULER, ADMIN);
		clearThManager.loadSteps(scheduler, dataDir.resolve(CONFIG_DIR).resolve(CONFIG_FILE).toFile());
		clearThManager.loadMatrices(scheduler, dataDir.resolve(MATRICES_DIR).toFile());
		try
		{
			scheduler.setStateConfig(new StateConfig(true));
			
			scheduler.start(ADMIN);
			ApplicationManager.waitForSchedulerToSuspend(scheduler, BASE_SLEEP_LENGTH, 3000);
			
			scheduler.stop();
			ApplicationManager.waitForSchedulerToStop(scheduler, BASE_SLEEP_LENGTH, 1000);
			
			boolean restored = scheduler.restoreState(ADMIN);
			Assert.assertTrue(restored, "State restored after execution interruption");
			
			ApplicationManager.waitForSchedulerToStop(scheduler, BASE_SLEEP_LENGTH, 3000);
			
			Assert.assertTrue(scheduler.isSuccessful());
			int actualActions = scheduler.getSteps().stream()
					.collect(Collectors.summingInt(s -> s.getExecutionProgress().getDone()));
			Assert.assertEquals(actualActions, expectedActions);
			
			restored = scheduler.restoreState(ADMIN);
			//After scheduler end the saved state should be removed and, thus, expected to be not restored
			Assert.assertFalse(restored, "State restored after execution end");
		}
		finally
		{
			scheduler.setStateConfig(new StateConfig(false));
		}
	}
	
	@Test
	public void testReportsUpdate() throws Exception
	{
		Scheduler scheduler = clearThManager.getScheduler(SCHEDULER, ADMIN);
		Path dataDir = TEST_CONFIG_DIR.resolve("ReportsUpdate");
		clearThManager.loadSteps(scheduler, dataDir.resolve(CONFIG_DIR).resolve(CONFIG_FILE).toFile());
		clearThManager.loadMatrices(scheduler, dataDir.resolve(MATRICES_DIR).toFile());
		try
		{
			scheduler.setStateConfig(new StateConfig(true));
			
			scheduler.start(ADMIN);
			ApplicationManager.waitForSchedulerToSuspend(scheduler, BASE_SLEEP_LENGTH, 3000);
			scheduler.stop();
			ApplicationManager.waitForSchedulerToStop(scheduler, BASE_SLEEP_LENGTH, 1000);
			
			boolean restored = scheduler.restoreState(ADMIN);
			Assert.assertTrue(restored, "State restored after 1st execution interruption");
			
			ApplicationManager.waitForSchedulerToSuspend(scheduler, BASE_SLEEP_LENGTH, 3000);
			scheduler.stop();
			ApplicationManager.waitForSchedulerToStop(scheduler, BASE_SLEEP_LENGTH, 1000);
			
			restored = scheduler.restoreState(ADMIN);
			Assert.assertTrue(restored, "State restored after 2nd execution interruption");
			
			ApplicationManager.waitForSchedulerToStop(scheduler, BASE_SLEEP_LENGTH, 3000);
			
			XmlSchedulerLaunchInfo launchInfo = scheduler.getSchedulerData().getLaunches().getLaunchesInfo().get(0);
			Path expectedReport = dataDir.resolve("expected_report.json"),
					actualReport = Path.of(ClearThCore.reportsPath(), launchInfo.getReportsPath(), "multi_pause.csv", "report.json");
			
			new JsonAssert().setIgnoredValueNames(SchedulerTest.IGNORED_EXPECTED_PARAMS)
					.assertEquals(expectedReport.toFile(), actualReport.toFile());
		}
		finally
		{
			scheduler.setStateConfig(new StateConfig(false));
		}
	}
	
	
	private boolean stepIsCorrect(Step step) {
		if (step.getFinished() == null) {
			return false;
		}
		if (step.isFailedDueToError()) {
			return false;
		}
		if (step.isAnyActionFailed()) {
			return false;
		}
		return true;
	}
}
