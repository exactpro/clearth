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

package com.exactprosystems.clearth.woodpecker.daemons.action;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.DaemonActionDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.StartScriptReader;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTableFactory;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class ActionTaskDescriptionsTableFactory implements TaskDescriptionsTableFactory
{
	private final Map<String, DaemonActionFactory> actionFactoriesByType;
	
	
	public ActionTaskDescriptionsTableFactory(Map<String, DaemonActionFactory> actionFactoriesByType)
	{
		this.actionFactoriesByType = actionFactoriesByType;
	}
	

	@Override
	public TaskDescriptionsTable create(StartScriptReader startScriptReader, DaemonSettings daemonSettings,
	                                    WoodpeckerNotifications notifications, LoadingStatistics loadingStatistics,
	                                    DaemonContext daemonContext) throws WoodpeckerException
	{
		ActionDaemonStartScriptReader actionStartScriptReader = (ActionDaemonStartScriptReader) startScriptReader;

		LinkedHashMap<String, ActionTaskDescription> tasks = new LinkedHashMap<>();
		for (DaemonActionDesc actionDesc : actionStartScriptReader.getActionsDescs())
		{
			ActionTaskDescription taskDesc = createDescription(actionDesc, daemonSettings, 
					(ActionLoadingStatistics) loadingStatistics, daemonContext, 
					actionStartScriptReader.getReplicaBaseDir(), tasks.keySet());
			
			tasks.put(actionDesc.getActionName(), taskDesc);
		}		
		return new ActionTaskDescriptionsTable(tasks);
	}
	
	private ActionTaskDescription createDescription(DaemonActionDesc actionDesc, DaemonSettings settings,
	                                                ActionLoadingStatistics statistics, DaemonContext context,
	                                                Path configsDirPath, Set<String> previousActionNames) 
			throws WoodpeckerException
	{
		String actionName = actionDesc.getActionName();
		if (previousActionNames.contains(actionName))
			throw new WoodpeckerConfigException("Duplicated action name '%s' is found in Start script.", actionName);

		String type = actionDesc.getActionType();
		DaemonActionFactory factory = actionFactoriesByType.get(type);
		if (factory == null)
			throw new UnsupportedOperationException(format("Action type '%s' is unknown.", type));

		DaemonAction action = factory.create(actionDesc, settings, statistics, context, configsDirPath);
		
		return new ActionTaskDescription(actionName, actionDesc.getPart(), actionDesc.getThreadsCount(), action);
	}
}
