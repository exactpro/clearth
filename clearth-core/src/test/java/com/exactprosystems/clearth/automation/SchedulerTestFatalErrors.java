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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.FatalAutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

public class SchedulerTestFatalErrors
{
	public static ApplicationManager clearThManager;

	public static Scheduler scheduler;
	public static final String generatorTestResources = "ActionGeneratorMatrixIDTest";
	public static final String userName = "test";
	public static Path stepCfg;

	@BeforeClass
	public static void init() throws ClearThException, FileNotFoundException
	{
		clearThManager = new ApplicationManager();
		stepCfg = Paths.get(resourceToAbsoluteFilePath(generatorTestResources));
	}

	@AfterClass
	public static void disposeTestApplication() throws IOException
	{
		if (clearThManager != null) clearThManager.dispose();
	}

	@Test(expected = FatalAutomationException.class)
	public void testStart() throws Exception
	{
		ClearThCore.getInstance().getConfig().getAutomation().getMatrixFatalErrors().setDuplicateActionId(true);

		scheduler = clearThManager.getScheduler("Test1", userName);
		File cfg = clearThManager.getSchedulerConfig(stepCfg.resolve("config").resolve("config.cfg"));
		clearThManager.loadSteps(scheduler, cfg);
		clearThManager.loadMatrices(scheduler, stepCfg.toFile());

		try
		{
			scheduler.start(userName);
		}
		finally
		{
			clearSchedulerTestApplication();
		}
	}

	public void clearSchedulerTestApplication()
	{
		SchedulersManager manager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> userSchedulers = manager.getUserSchedulers(userName);
		userSchedulers.clear();
	}
}