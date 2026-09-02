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
import com.exactprosystems.clearth.woodpecker.configuration.start.ActionDaemonStartScript;
import com.exactprosystems.clearth.woodpecker.configuration.start.StartScript;
import com.exactprosystems.clearth.woodpecker.configuration.start.StartScriptReader;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemonFactory;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTableFactory;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingGenerator;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.reports.DaemonExecutionReportWriter;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;
import com.exactprosystems.clearth.woodpecker.utils.charts.LoadingChartBuilder;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class ActionDaemonFactory extends WoodpeckerDaemonFactory
{
	public static final String FACTORY_NAME = "ActionDaemonFactory";
	

	@Override
	public Class<? extends ActionDaemonStartScript> getStartScriptClass()
	{
		return ActionDaemonStartScript.class;
	}
	
	@Override
	public StartScriptReader<? extends StartScript> createStartScriptReader(Path replicaBaseDir)
	{
		return new ActionDaemonStartScriptReader(replicaBaseDir, getStartScriptClass(),
				getExtendedStartScriptClasses());
	}
	

	@Override
	public TaskDescriptionsTableFactory createTaskDescriptionsTableFactory()
	{
		return new ActionTaskDescriptionsTableFactory(getAdditionalActionFactories());
	}
	
	public Map<String, DaemonActionFactory> getAdditionalActionFactories()
	{
		return emptyMap();
	}
	
	
	@Override
	public LoadingStatistics createLoadingStatistics(WoodpeckerDaemon daemon, Collection<String> operationNames)
	{
		return new ActionLoadingStatistics(operationNames);
	}
	

	@Override
	public LoadingGenerator createLoadingGenerator(DaemonSettings settings, WorkQueue workQueue,
	                                               TaskDescriptionsTable table)
	{
		return new ActionLoadingGenerator(workQueue, (ActionTaskDescriptionsTable) table);
	}

	
	@Override
	public LoadingChartBuilder createLoadingChartBuilder()
	{
		return new ActionDaemonChartBuilder();
	}


	@Override
	public DaemonExecutionReportWriter createDaemonExecutionReportWriter()
	{
		return new ActionDaemonExecutionReportWriter();
	}
}
