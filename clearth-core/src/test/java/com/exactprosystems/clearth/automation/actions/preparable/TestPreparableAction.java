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

package com.exactprosystems.clearth.automation.actions.preparable;

import static com.exactprosystems.clearth.ApplicationManager.ADMIN;
import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;
import static com.exactprosystems.clearth.ApplicationManager.waitForSchedulerToStop;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.utils.SettingsException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.LoggerStub;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.ClearThException;

public class TestPreparableAction
{
	private static final Path TEST_DATA = USER_DIR.resolve("src/test/resources/Action/Preparable");
	private static final Path ACTIONS_MAPPING_PATH = TEST_DATA.resolve("actionsmapping.cfg");
	private static final Path MATRICES_DIR = TEST_DATA.resolve("matrices");
	private static final Path CONFIGS_DIR = TEST_DATA.resolve("configs");
	private static final Path CONFIG = CONFIGS_DIR.resolve("config.cfg");
	private static ApplicationManager clearThManager;
	private static final Logger logger = new LoggerStub();
	private static Map<String, ActionMetaData> extraActionsMapping;
	private static Map<String, ActionMetaData> originActionsMapping;

	@Test
	public void testPrepare() throws IOException, ClearThException, AutomationException
	{
		Scheduler scheduler = clearThManager.getScheduler(ADMIN, ADMIN);
		scheduler.clearSteps();
		clearThManager.loadSteps(scheduler, CONFIG.toFile());
		clearThManager.loadMatrices(scheduler, MATRICES_DIR.toFile());

		scheduler.start(ADMIN);
		waitForSchedulerToStop(scheduler, 100, 2000);

		Assert.assertTrue(PreparableAction1.isPrepared());
		Assert.assertFalse(PreparableAction2.isPrepared());
	}

	private static void addExtraActionsMapping() throws SettingsException {
		originActionsMapping = ClearThCore.getInstance().getActionFactory().getActionsMapping();
		extraActionsMapping = new ActionsMapping(ACTIONS_MAPPING_PATH,true).getDescriptions();
		originActionsMapping.putAll(extraActionsMapping);
	}

	private static void removeExtraActionsMapping()
	{
		for (String actionName : extraActionsMapping.keySet())
		{
			originActionsMapping.remove(actionName);
		}
	}

	@BeforeClass
	public static void startTestApp() throws ClearThException, SettingsException {
		clearThManager = new ApplicationManager();
		addExtraActionsMapping();
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
		removeExtraActionsMapping();
		if (clearThManager != null) clearThManager.dispose();
	}
}
