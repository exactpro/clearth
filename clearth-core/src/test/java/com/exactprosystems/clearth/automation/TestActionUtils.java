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

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.LoggerStub;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.data.DefaultTestExecutionHandler;
import com.exactprosystems.clearth.generators.IncrementingValueGenerator;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class TestActionUtils
{
	private static final Logger logger = new LoggerStub();
	
	public static Map<String, ActionMetaData> addCustomActions(Path customActionsMapping) throws SettingsException {
		Map<String, ActionMetaData> customActions = new ActionsMapping(customActionsMapping,true).getDescriptions();
		ClearThCore.getInstance().getActionFactory().getActionsMapping().putAll(customActions);
		return customActions;
	}
	
	public static void removeCustomActions(Collection<String> customActions)
	{
		ClearThCore.getInstance().getActionFactory().getActionsMapping()
				.keySet().removeAll(customActions);
	}
	
	
	public static Scheduler runScheduler(ApplicationManager manager, String userName, String schedulerName, Path configFile, Path matricesDir, int timeout) 
			throws ClearThException, IOException, AutomationException
	{
		Scheduler scheduler = manager.getScheduler(schedulerName, userName);
		scheduler.clearSteps();
		
		manager.loadSteps(scheduler, configFile.toFile());
		manager.loadMatrices(scheduler, matricesDir.toFile());
		
		scheduler.start(userName);
		ApplicationManager.waitForSchedulerToStop(scheduler, 100, timeout);
		return scheduler;
	}
	
	public static void resetUserSchedulers(String userName)
	{
		SchedulersManager manager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> userSchedulers = manager.getUserSchedulers(userName);
		if (userSchedulers != null)
			userSchedulers.clear();
	}

	public static GlobalContext createGlobalContext(String user)
	{
		return new GlobalContext(new Date(),
				false,new HashMap<>(),
				new MatrixFunctions(new HashMap<>(), new Date(),new Date(), false, new IncrementingValueGenerator(1)),
				user, new DefaultTestExecutionHandler());
	}
}