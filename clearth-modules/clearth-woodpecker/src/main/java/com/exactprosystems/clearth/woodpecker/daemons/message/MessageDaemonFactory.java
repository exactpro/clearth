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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.SimpleMessageSenderDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageDaemonStartScript;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemonFactory;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTable;
import com.exactprosystems.clearth.woodpecker.execution.loading.generator.LoadingGenerator;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProviderFactory;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProviderTypes;
import com.exactprosystems.clearth.woodpecker.execution.providers.csvTemplate.CsvTemplateMsgProviderFactory;
import com.exactprosystems.clearth.woodpecker.execution.senders.SimpleMessageSenderFactory;
import com.exactprosystems.clearth.woodpecker.execution.senders.MessageSenderFactory;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.WorkQueue;
import com.exactprosystems.clearth.woodpecker.reports.DaemonExecutionReportWriter;
import com.exactprosystems.clearth.woodpecker.utils.charts.LoadingChartBuilder;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.arraycopy;
import static java.util.Collections.emptyMap;

public class MessageDaemonFactory extends WoodpeckerDaemonFactory
{
	public static final String FACTORY_NAME = "MessageDaemonFactory";
	
	@Override
	public DaemonSettings createSettings()
	{
		return new MessageDaemonSettings();
	}

	@Override
	public Class<? extends DaemonSettings> getSettingsClass()
	{
		return MessageDaemonSettings.class;
	}
	

	@Override
	public Class<MessageDaemonStartScript> getStartScriptClass()
	{
		return MessageDaemonStartScript.class;
	}
	
	@Override
	public MessageDaemonStartScriptReader createStartScriptReader(Path replicaBaseDir)
	{
		return new MessageDaemonStartScriptReader(replicaBaseDir, getStartScriptClass(),
				getOtherClassesForStartScript());
	}


	public Map<String, MessageProviderFactory> getAdditionalProviderFactories()
	{
		return emptyMap();
	}

	public Map<String, MessageSenderFactory> getAdditionalSenderFactories()
	{
		return emptyMap();
	}

	@Override
	public MessageTaskDescriptionsTableFactory createTaskDescriptionsTableFactory()
	{
		Map<String, MessageProviderFactory> providerFactories = new HashMap<>();
		providerFactories.put(MessageProviderTypes.CSV_TEMPLATE, new CsvTemplateMsgProviderFactory());
		providerFactories.putAll(getAdditionalProviderFactories());
		
		Map<String, MessageSenderFactory> senderFactories = new HashMap<>();
		senderFactories.put(SimpleMessageSenderDesc.TYPE, new SimpleMessageSenderFactory());
		senderFactories.putAll(getAdditionalSenderFactories());
		
		return new MessageTaskDescriptionsTableFactory(providerFactories, senderFactories);
	}


	@Override
	public MessageLoadingStatistics createLoadingStatistics(WoodpeckerDaemon daemon, Collection<String> operationNames)
	{
		return new MessageLoadingStatistics(daemon, operationNames);
	}


	@Override
	public LoadingGenerator createLoadingGenerator(DaemonSettings settings, WorkQueue workQueue,
	                                               TaskDescriptionsTable table)
	{
		MessageDaemonSettings mds = (MessageDaemonSettings) settings;
		MessageTaskDescriptionsTable msgTable = (MessageTaskDescriptionsTable) table;
		return new MessageLoadingGenerator(mds, workQueue, msgTable);
	}


	@Override
	public LoadingChartBuilder createLoadingChartBuilder()
	{
		return new MessageDaemonChartBuilder();
	}


	@Override
	public DaemonExecutionReportWriter createDaemonExecutionReportWriter()
	{
		return new MessageDaemonExecutionReportWriter();
	}
	
	
	private Class[] getOtherClassesForStartScript()
	{
		Class[] extended = getExtendedStartScriptClasses();
		int extCount = extended.length;
		
		Class[] result = new Class[extCount + 1];
		result[0] = SimpleMessageSenderDesc.class;
		if (extCount > 0)
			arraycopy(extended, 0, result, 1, extCount);
		return result;
	}
}

