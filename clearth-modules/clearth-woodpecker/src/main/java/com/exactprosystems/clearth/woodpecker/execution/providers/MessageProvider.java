/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.execution.providers;

import com.exactprosystems.clearth.woodpecker.configuration.settings.OperationSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageProviderDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.OperationUnit;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerNotEnoughInitialDataException;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;
import com.exactprosystems.clearth.woodpecker.message.data.LinkedMessages;
import com.exactprosystems.clearth.woodpecker.message.data.TaskMessages;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.exactprosystems.clearth.woodpecker.message.generator.MessageTemplateVars.BATCH_SIZE;
import static java.lang.String.format;

public abstract class MessageProvider
{
	private static final Logger logger = LoggerFactory.getLogger(MessageProvider.class);
	
	protected final String id;
	protected final String daemonName;
	protected final WoodpeckerNotifications notifications;
	protected final MessageProviderDesc description;
	protected final OperationSettings operationSettings;
	
	private final AtomicInteger seqErrorsCounter = new AtomicInteger();
	private volatile boolean invalidated;
	
	protected MessageProvider(String id,
	                          String daemonName,
	                          WoodpeckerNotifications notifications,
	                          MessageProviderDesc description,
	                          OperationSettings operationSettings)
	{
		this.id = id;
		this.daemonName = daemonName;
		this.notifications = notifications;
		this.description = description;
		this.operationSettings = operationSettings;
	}

	
	protected abstract LinkedMessages createMessagesImpl(WoodpeckerFunctions functions,
	                                                     Map<String, Object> externalParameters) throws WoodpeckerException;
	
	public abstract int getLinkedMessagesCount();
	
	
	public void dispose() { /* Nothing to dispose by default */ }


	public String getId()
	{
		return id;
	}

	public MessageProviderDesc getDescription()
	{
		return description;
	}

	public OperationSettings getOperationSettings()
	{
		return operationSettings;
	}

	public boolean isInvalidated()
	{
		return invalidated;
	}
	

	public final TaskMessages createMessages(WoodpeckerFunctions functions, 
	                                         int batchSize, 
	                                         Map<String, Object> senderParameters)
	{
		if (!invalidated)
		{
			try
			{
				TaskMessages tms = new TaskMessages(operationSettings);
				if (operationSettings.getUnit() == OperationUnit.RG)
				{
					Map<String, Object> parameters = new HashMap<>(senderParameters);
					parameters.put(BATCH_SIZE, batchSize);
					tms.addMessages(createMessagesImpl(functions, parameters));
				}
				else 
				{
					for (int i = 0; i < batchSize; i++)
					{
						tms.addMessages(createMessagesImpl(functions, senderParameters));
					}
				}
				onSuccess();
				return tms;
			}
			catch (WoodpeckerNotEnoughInitialDataException e)
			{
				logger.debug("Not enough data to generate message: {}", e.getMessage());
			}
			catch (Exception e)
			{
				processError(e);
			}
		}
		return null;
	}
	
	
	private void onSuccess()
	{
		seqErrorsCounter.set(0);
	}
	
	
	private void processError(Exception e)
	{
		notifications.addWarning(daemonName, logger, e);
		if (seqErrorsCounter.incrementAndGet() > description.getMaxSeqErrorsCount())
			invalidate();
	}
	
	private void invalidate()
	{
		invalidated = true;
		
		notifications.addWarning(daemonName, logger,
				format("Too many errors in the Provider %s. It has been stopped.", id));
		
		dispose();
	}
}
