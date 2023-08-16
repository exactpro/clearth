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

package com.exactprosystems.clearth.automation.actions.csv;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.exactprosystems.clearth.ApplicationManager.waitForSchedulerToStop;

public class AddRecordToCsvFileTest
{

	private static final String ADMIN = "admin";
	private static final Path TEST_OUTPUT = Paths.get("testOutput").resolve(AddRecordToCsvFileTest.class.getSimpleName());
	private Path resDir, config, matrices, outputFile;
	private ApplicationManager appManager;

	@BeforeClass
	public void init() throws IOException, ClearThException, SettingsException
	{
		FileUtils.deleteDirectory(TEST_OUTPUT.toFile());
		Files.createDirectory(TEST_OUTPUT);

		outputFile = TEST_OUTPUT.resolve("file.csv");
		Files.createFile(outputFile);

		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath("Action")).resolve(AddRecordToCsvFileTest.class.getSimpleName());
		config = resDir.resolve("configs").resolve("config.cfg");
		matrices = resDir.resolve("matrices");

		appManager = new ApplicationManager();
	}

	@AfterClass
	public void dispose() throws IOException
	{
		SchedulersManager sm = ClearThCore.getInstance().getSchedulersManager();
		Scheduler scheduler = sm.getSchedulerByName(ADMIN, ADMIN);
		scheduler.clearSteps();

		if (appManager != null)
			appManager.dispose();
	}

	@Test
	public void testRun() throws ClearThException, AutomationException, IOException
	{
		Scheduler scheduler = appManager.getScheduler(ADMIN, ADMIN);
		appManager.loadSteps(scheduler, config.toFile());
		appManager.loadMatrices(scheduler, matrices.toFile());

		scheduler.start(ADMIN);
		waitForSchedulerToStop(scheduler, 1000, 5000);

		Assert.assertTrue(scheduler.isSuccessful());

		String actualData = FileUtils.readFileToString(outputFile.toFile(), Utils.UTF8);
		String expectedData = "Paparam1,Paparam2,Paparam3" + System.lineSeparator() + "Va,l u,e s" + System.lineSeparator() +
				"Va lu es,lu es,e es" + System.lineSeparator();
		Assert.assertEquals(actualData, expectedData);
	}
}