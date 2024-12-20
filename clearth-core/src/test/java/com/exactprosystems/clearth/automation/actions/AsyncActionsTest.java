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

package com.exactprosystems.clearth.automation.actions;

import static com.exactprosystems.clearth.ApplicationManager.ADMIN;
import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

import java.io.IOException;
import java.nio.file.Path;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.utils.ClearThException;

public class AsyncActionsTest
{
	private static final Path TEST_DATA = USER_DIR.resolve("src/test/resources/Async");
	private static final String SCHEDULER = "async";
	
	private ApplicationManager appManager;
	
	@BeforeClass
	public void init() throws ClearThException
	{
		appManager = new ApplicationManager();
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (appManager != null)
			appManager.dispose();
	}
	
	@Test
	public void asyncInStepStatus() throws Exception
	{
		Scheduler scheduler = appManager.getScheduler(SCHEDULER, ADMIN);
		scheduler.clearSteps();
		appManager.loadSteps(scheduler, TEST_DATA.resolve("config.cfg").toFile());
		appManager.loadMatrices(scheduler, TEST_DATA.resolve("matrices").toFile());
		
		scheduler.start(ADMIN);
		
		//2nd step has long asynchronous action that is awaited at the end of 3rd step
		try
		{
			ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 1000);
			Assert.assertFalse(scheduler.getSteps().get(1).isAsync(), "Async status of 2nd step before its start");
			
			scheduler.continueExecution();
			ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 1000);
			Assert.assertTrue(scheduler.getSteps().get(1).isAsync(), "Async status of 2nd step after its start");
			
			scheduler.continueExecution();
			ApplicationManager.waitForSchedulerToSuspend(scheduler, 100, 5000);
			Assert.assertFalse(scheduler.getSteps().get(1).isAsync(), "Async status of 2nd step after finish of 3rd step");
			
			scheduler.continueExecution();
			ApplicationManager.waitForSchedulerToStop(scheduler, 100, 1000);
		}
		catch (Exception e)
		{
			scheduler.stop();
			throw e;
		}
	}
}
