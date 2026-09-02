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
package com.exactprosystems.clearth.woodpecker.daemons;

import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonDesc;
import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.StartScript;
import com.exactprosystems.clearth.woodpecker.configuration.start.StartScriptReader;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTableFactory;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingGenerator;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.reports.DaemonExecutionReportWriter;
import com.exactprosystems.clearth.woodpecker.reports.DaemonExecutionResultExporter;
import com.exactprosystems.clearth.woodpecker.reports.DefaultDaemonExecutionResultExporter;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;
import com.exactprosystems.clearth.woodpecker.utils.charts.LoadingChartBuilder;

import java.nio.file.Path;
import java.util.Collection;

public abstract class WoodpeckerDaemonFactory
{
	
	public WoodpeckerDaemon createDaemon(Path replicaBaseDirPath, DaemonSettings settings, DaemonDesc description)
	{
		return new CommonDaemon(this, replicaBaseDirPath, settings, description);
	}
	
	
	public DaemonSettings createSettings()
	{
		return new DaemonSettings();
	}
	
	public Class<? extends DaemonSettings> getSettingsClass()
	{
		return DaemonSettings.class;
	}
	
	
	public abstract Class<? extends StartScript> getStartScriptClass();

	public Class[] getExtendedStartScriptClasses()
	{
		return new Class[]{};
	}
	
	public abstract StartScriptReader<? extends StartScript> createStartScriptReader(Path replicaBaseDir);


	public DaemonContext createDaemonContext(DaemonSettings settings) throws WoodpeckerException
	{
		return new DaemonContext(settings);
	}
	
	
	public abstract TaskDescriptionsTableFactory createTaskDescriptionsTableFactory();
	
	
	public abstract LoadingStatistics createLoadingStatistics(WoodpeckerDaemon daemon, Collection<String> operationNames);
	
	
	public abstract LoadingGenerator createLoadingGenerator(DaemonSettings settings, WorkQueue workQueue,
	                                                        TaskDescriptionsTable table);
	
	
	public abstract LoadingChartBuilder createLoadingChartBuilder();
	
	
	public abstract DaemonExecutionReportWriter createDaemonExecutionReportWriter();

	public DaemonExecutionResultExporter createDaemonExecutionResultExporter()
	{
		return new DefaultDaemonExecutionResultExporter();
	}
}
