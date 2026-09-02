/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

import com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerConfigFile;
import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.*;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescriptionsTableFactory;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProvider;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProviderFactory;
import com.exactprosystems.clearth.woodpecker.execution.senders.MessageSender;
import com.exactprosystems.clearth.woodpecker.execution.senders.MessageSenderFactory;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.statistics.LoadingStatistics;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.*;

import static java.lang.String.format;
import static org.apache.commons.collections4.MapUtils.isEmpty;

public class MessageTaskDescriptionsTableFactory implements TaskDescriptionsTableFactory
{
	private final Map<String, MessageProviderFactory> providerFactoriesByType;
	private final Map<String, MessageSenderFactory> senderFactoriesByType;
	
	
	public MessageTaskDescriptionsTableFactory(Map<String, MessageProviderFactory> providerFactoriesByType,
	                                           Map<String, MessageSenderFactory> senderFactoriesByType)
	{
		this.providerFactoriesByType = providerFactoriesByType;
		this.senderFactoriesByType = senderFactoriesByType;
	}
	

	@Override
	public MessageTaskDescriptionsTable create(StartScriptReader startScriptReader, DaemonSettings daemonSettings,
	                                           WoodpeckerNotifications notifications, LoadingStatistics loadingStatistics,
	                                           DaemonContext context) throws WoodpeckerException
	{
		MessageDaemonStartScriptReader msgStartScriptReader = (MessageDaemonStartScriptReader) startScriptReader;
		MessageLoadingStatistics messageLoadingStatistics = (MessageLoadingStatistics) loadingStatistics; 
		
		String daemonName = daemonSettings.getFullName();
		
		List<MessageProvider> providers = null;
		List<MessageSender> senders = null;
		try
		{
			providers = initMessageProviders(msgStartScriptReader, daemonName, notifications);
			senders = initMessageSenders(msgStartScriptReader, daemonName, notifications,
					messageLoadingStatistics, context);
			
			return createTable(providers, senders);
		}
		catch (Exception e)
		{
			if (providers != null)
				disposeProviders(providers);
			if (senders != null)
				disposeSenders(senders);
			throw e;
		}
	}

	
	private List<MessageProvider> initMessageProviders(MessageDaemonStartScriptReader reader, String daemonName,
	                                                   WoodpeckerNotifications notifications) throws WoodpeckerException
	{
		List<MessageProvider> providers = new ArrayList<>();
		Collection<String> executableOperationNames = reader.getExecutableOperationNames();
		Path replicaBaseDir = reader.getReplicaBaseDir();
		
		for (MessageProviderDesc providerDesc : reader.getProviderDescs())
		{
			if (!executableOperationNames.contains(providerDesc.getOperationName()))
				continue;
			
			String type = providerDesc.getProviderType();
			MessageProviderFactory factory = providerFactoriesByType.get(type);
			if (factory == null)
				throw new UnsupportedOperationException(format("Provider type '%s' is unknown.", type));
			
			MessageProvider provider = factory.create(daemonName, notifications, providerDesc, replicaBaseDir);
			providers.add(provider);
		}
		return providers;
	}
	
	private List<MessageSender> initMessageSenders(MessageDaemonStartScriptReader reader, String daemonName, 
	                                               WoodpeckerNotifications notifications, 
	                                               MessageLoadingStatistics loadingStatistics, DaemonContext context)
			throws WoodpeckerException
	{
		List<MessageSender> senders = new ArrayList<>();
		Collection<String> executableOperationNames = reader.getExecutableOperationNames();
		Path replicaBaseDir = reader.getReplicaBaseDir();
		
		for (MessageSenderDesc senderDesc : reader.getSenderDescs())
		{
			if (!executableOperationNames.contains(senderDesc.getOperationName()))
				continue;
			
			String type = senderDesc.getSenderType();
			MessageSenderFactory factory = senderFactoriesByType.get(type);
			if (factory == null)
				throw new UnsupportedOperationException(format("Sender type '%s' is unknown.", type));
			
			MessageSender sender = factory.create(daemonName, notifications, loadingStatistics, 
					context, senderDesc, replicaBaseDir);
			senders.add(sender);
		}
		return senders;
	}
	
	private MessageTaskDescriptionsTable createTable(List<MessageProvider> providers, List<MessageSender> senders) 
			throws WoodpeckerException
	{
		LinkedHashMap<String, MessageTaskDescription> descriptions = new LinkedHashMap<>();
		
		for (MessageProvider provider : providers)
		{
			MessageProviderDesc providerDesc = provider.getDescription();
			String opName = providerDesc.getOperationName();
			for (MessageSender sender : senders)
			{
				MessageSenderDesc senderDesc = sender.getDescription();
				if (StringUtils.equals(opName, senderDesc.getOperationName()))
				{
					String taskId = provider.getId() + "-" + sender.getId();
					MessageTaskDescription description = new MessageTaskDescription(taskId, senderDesc.getPart(),
							senderDesc.getThreadsCount(), providerDesc.isBatchEnabled(), provider, sender);
					descriptions.put(taskId, description);
				}
			}
		}
		
		if (isEmpty(descriptions))
			throw new WoodpeckerConfigException("Unable to start daemon: %s doesn't contain tasks to execute.",
					WoodpeckerConfigFile.START_FILE.fileName());
		
		return new MessageTaskDescriptionsTable(descriptions);
	}
	
	private void disposeProviders(Collection<MessageProvider> providers)
	{
		for (MessageProvider p : providers)
			p.dispose();
	}
	
	private void disposeSenders(Collection<MessageSender> senders)
	{
		for (MessageSender s : senders)
			s.dispose();
	}
}
