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

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.Stopwatch;

public class TestSchedulerSuccess
{
	private static final String ADMIN = "admin";
	private static final Path TEST_CONFIG_DIR = USER_DIR.resolve("src/test/resources/Scheduler/TestIsSuccessful");
	private static final Path TEST_DATA = TEST_CONFIG_DIR.resolve("TwoParallelMatricesWithSimilarStep");
	private static final Path MATRICES_DIR = TEST_DATA.resolve("matrices");
	private static final Path CONFIGS_DIR = TEST_DATA.resolve("configs");
	private static final Path CONFIG = CONFIGS_DIR.resolve("config.cfg");
	private static final String STEP1 = "Step1";

	private static ApplicationManager clearThManager;


	@Test
	public void testIsSuccessful() throws ClearThException, AutomationException, IOException
	{
		Scheduler scheduler = clearThManager.getScheduler(ADMIN, ADMIN);
		scheduler.clearSteps();
		clearThManager.loadSteps(scheduler, CONFIG.toFile());
		clearThManager.loadMatrices(scheduler, MATRICES_DIR.toFile());

		scheduler.start(ADMIN);
		waitForSchedulerToStop(scheduler, 100, 2000);

		Map<String, Step> stepsMap = toMap(scheduler.getSteps());

		Step step1 = stepsMap.get(STEP1);
		if (step1 == null)
			fail("There is gotta be + '" + STEP1 + "' in Scheduler configuration due to the test case.");

		Assert.assertTrue(step1.isAnyActionFailed());
		Assert.assertFalse(step1.isFailedDueToError());
		Assert.assertFalse(scheduler.isSuccessful());
	}

	private static Map<String, Step> toMap(Collection<Step> steps)
	{
		return steps.stream().collect(Collectors.toMap(Step::getName, Function.identity()));
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
		if (clearThManager != null) clearThManager.dispose();
	}

	private static void waitForSchedulerToStop(Scheduler scheduler, long delay, long timeout)
	{
		try
		{
			Stopwatch s = Stopwatch.createAndStart(timeout);
			while (scheduler.isRunning())
			{
				if (s.isExpired())
					fail("Too long to wait for Scheduler to finish.");

				TimeUnit.MILLISECONDS.sleep(delay);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			fail("Waiting for Scheduler to stop is interrupted.");
		}
	}
}
