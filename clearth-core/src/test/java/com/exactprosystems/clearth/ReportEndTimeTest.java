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

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.Stopwatch;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static org.junit.Assert.fail;

public class ReportEndTimeTest
{
	private static final int WAITING_TIMEOUT = 1000;

	private final String userName = "reportChecker", schedulerName = "ReportChecker";

	private static final String RESOURCE_PATH = "ReportEndTimeTest";
	private static final String MATRICES_PATH = "matrices";
	private static final String CONFIG_PATH = "configs/config.cfg";
	private static final Path TEST_OUTPUT_PATH =
			USER_DIR.getParent().resolve("testOutput").resolve("ReportEndTimeTest");

	private static ApplicationManager clearThManager;
	private Scheduler scheduler;

	@BeforeClass
	public static void startTestApp() throws ClearThException, IOException
	{
		clearThManager = new ApplicationManager();
		Files.createDirectories(TEST_OUTPUT_PATH);
	}

	@Test
	public void runTest() throws ClearThException, AutomationException, IOException
	{
		scheduler = clearThManager.getScheduler(userName, schedulerName);
		loadStepsForExecuteTest();
		loadMatricesForExecuteTest();
		scheduler.start(userName);
		waitForFirstStepEnd(100, WAITING_TIMEOUT);
		checkExecutionTime();
		scheduler.stop();
	}

	private void checkExecutionTime()
	{
		long executionTime =
				scheduler.getExecutor().getReportEndTime().getTime() - scheduler.getExecutor().getStarted().getTime();
		long stepsTimeSum = calculateStepsTimeSum();
		if (executionTime < stepsTimeSum)
			fail(String.format("Execution time in millis is: %d. That is less than the sum of all steps: %d.",
					executionTime, stepsTimeSum));
	}

	private long calculateStepsTimeSum()
	{
		long stepsTimeSum = 0;
		for (Step step : scheduler.getSteps())
		{
			if (step.getStarted() != null)
			{
				if (step.getFinished() != null)
				{
					long stepTime = step.getFinished().getTime() - step.getStarted().getTime();
					stepsTimeSum += stepTime;
				}
				else
				{
					for (Action action : step.getActions())
					{
						if (action.getStarted() != null && action.getFinished() != null)
						{
							long actionTime = action.getFinished().getTime() - action.getStarted().getTime();
							stepsTimeSum += actionTime;
						}
					}
				}
			}
		}
		return stepsTimeSum;
	}

	private void loadStepsForExecuteTest() throws ClearThException, FileNotFoundException
	{
		File config = Paths.get(resourceToAbsoluteFilePath(RESOURCE_PATH)).resolve(CONFIG_PATH).toFile();
		clearThManager.loadSteps(scheduler, config);
	}

	private void loadMatricesForExecuteTest() throws ClearThException, FileNotFoundException
	{
		File matricesDir = Paths.get(resourceToAbsoluteFilePath(RESOURCE_PATH)).resolve(MATRICES_PATH).toFile();
		clearThManager.loadMatrices(scheduler, matricesDir);
	}

	private void waitForFirstStepEnd(long delay, long timeout)
	{
		try
		{
			Stopwatch s = Stopwatch.createAndStart(timeout);
			while (scheduler.isRunning())
			{
				if (scheduler.getSteps().get(0).getExecutionProgress().getDone() > 1)
					return;
				if (s.isExpired())
					fail("Timeout expired before ending of the first step.");
				TimeUnit.MILLISECONDS.sleep(delay);
			}
			fail("Scheduler execution finished before timeout expired.");
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			fail("Waiting before first step is done is interrupted.");
		}
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
		FileUtils.cleanDirectory(TEST_OUTPUT_PATH.toFile());
	}

}