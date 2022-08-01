/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;

public class TestSchedulerRestart
{
	private static final Path TEST_CONFIG_DIR = USER_DIR.resolve("src/test/resources/Scheduler/TestRestart");
	private static final Path TEST_DATA = TEST_CONFIG_DIR.resolve("SimpleRestart");
	private static final Path MATRICES_DIR = TEST_DATA.resolve("matrices");
	private static final Path CONFIGS_DIR = TEST_DATA.resolve("configs");
	private static final Path CONFIG = CONFIGS_DIR.resolve("config.cfg");
	private static final String STEP1 = "Step1";
	private static final List<String> STEPS = Arrays.asList(STEP1, "Step2");
	private static final long BASE_SLEEP_LENGTH = 10;

	private static ApplicationManager clearThManager;


	@Test
	public void testRestart() throws ClearThException, AutomationException, IOException
	{
		Scheduler scheduler = clearThManager.getScheduler(ADMIN, ADMIN);
		scheduler.clearSteps();
		clearThManager.loadSteps(scheduler, CONFIG.toFile());
		clearThManager.loadMatrices(scheduler, MATRICES_DIR.toFile());

		scheduler.start(ADMIN);
		if (!awaitSchedulerSuspend(scheduler, 1000)) {
			fail("Scheduler should suspend after first step");
		}

		Assert.assertEquals(STEP1, scheduler.getCurrentStep().getName());
		Assert.assertTrue(scheduler.isSuspended());
		Assert.assertTrue(scheduler.saveState());

		try {
			scheduler.stop();
			if (!awaitSchedulerStop(scheduler, 1000)) {
				fail("Scheduler did not stop in expected time");
			}
			Assert.assertTrue(scheduler.isStoppedByUser());
			Assert.assertTrue(scheduler.isInterrupted());
			Assert.assertTrue(scheduler.restoreState(ADMIN));
		} catch (Exception e) {
			fail("Restoring state should not throw exception: " + e.toString());
		}

		scheduler.continueExecution();
		if (!awaitSchedulerStop(scheduler, 1000)) {
			fail("Scheduler did not finish second and later steps in expected time");
		}

		List<Step> schedulerSteps = scheduler.getSteps();
		Assert.assertEquals(schedulerSteps.size(), STEPS.size());
		for (int i = 0; i < STEPS.size(); ++i) {
			Assert.assertEquals(schedulerSteps.get(i).getName(), STEPS.get(i));
			Assert.assertTrue(stepIsCorrect(schedulerSteps.get(i)));
		}
	}

	private boolean awaitSchedulerSuspend(Scheduler scheduler, long maxTimeout) {
		long waitTime = 0;
		while (!scheduler.isSuspended()) {
			try {
				Thread.sleep(BASE_SLEEP_LENGTH);
			} catch (InterruptedException e) {
				return false;
			}
			waitTime += BASE_SLEEP_LENGTH;
			if (waitTime > maxTimeout) {
				return false;
			}
		}
		return true;
	}

	private boolean awaitSchedulerStop(Scheduler scheduler, long maxTimeout) {
		long waitTime = 0;
		while (scheduler.isRunning()) {
			try {
				Thread.sleep(BASE_SLEEP_LENGTH);
			} catch (InterruptedException e) {
				return false;
			}
			waitTime += BASE_SLEEP_LENGTH;
			if (waitTime > maxTimeout) {
				return false;
			}
		}
		return true;
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

	@BeforeClass
	public static void startTestApp() throws ClearThException
	{
		clearThManager = new ApplicationManager();
	}

	@After
	public void clearSchedulerData()
	{
		SchedulersManager manager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> userSchedulers = manager.getUserSchedulers(ADMIN);
		userSchedulers.clear();
	}

	@AfterClass
	public static void disposeTestApp() throws IOException
	{
		if (clearThManager != null) 
			clearThManager.dispose();
	}
}
