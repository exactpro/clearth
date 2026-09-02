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

package com.exactprosystems.clearth.woodpecker.execution.senders;

import com.exactprosystems.clearth.woodpecker.configuration.settings.OperationSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageSenderDesc;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageScheduledTask;
import com.exactprosystems.clearth.woodpecker.message.data.LinkedMessages;
import com.exactprosystems.clearth.woodpecker.message.data.Message;
import com.exactprosystems.clearth.woodpecker.message.data.TaskMessages;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageLoadingStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public abstract class MessageSender
{
	private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

	protected final String id;
	
	protected final String daemonName;
	protected final WoodpeckerNotifications notifications;
	protected final MessageLoadingStatistics loadingStatistics;
	protected final MessageSenderDesc description;
	protected final Map<String, Object> parameters;
	
	private final AtomicInteger seqErrorsCounter = new AtomicInteger();
	private volatile boolean invalidated;


	protected abstract void sendMessage(Message message, SendingCallback sendingCallback) throws WoodpeckerException;

	public void dispose() { /* Nothing to dispose by default */}
	
	
	public MessageSender(String id,
	                     String daemonName,
	                     WoodpeckerNotifications notifications,
	                     MessageLoadingStatistics loadingStatistics,
	                     MessageSenderDesc description,
	                     Map<String, Object> parameters)
	{
		this.id = id;
		this.daemonName = daemonName;
		this.notifications = notifications;
		this.loadingStatistics = loadingStatistics;
		this.description = description;
		this.parameters = parameters;
	}
	
	
	public final void send(TaskMessages taskMessages, MessageScheduledTask task)
	{
		if (isInvalidated())
			return;
		
		OperationSettings os = taskMessages.getOperationSettings();
		try
		{
			for (LinkedMessages lms : taskMessages)
			{
				for (int i = 0; i < lms.size(); i++)
				{
					Message message = lms.getMessage(i);
					boolean isLastLinkedMsg = i == (lms.size() - 1);
					
					SendingCallback sc = new SendingCallback(seqErrorsCounter, loadingStatistics, 
							os, isLastLinkedMsg, task);
					sendMessage(message, sc);
				}
			}
		}
		catch (Exception e)
		{
			handleError(e);
		}
	}


	protected void handleError(Exception e)
	{
		notifications.addWarning(daemonName, logger, e);
		updateErrorsCounter();
	}
	
	@SuppressWarnings("unused")
	protected void handleError(String errorMessage)
	{
		notifications.addWarning(daemonName, logger, errorMessage);
		updateErrorsCounter();
	}
	
	private void updateErrorsCounter()
	{
		if (seqErrorsCounter.incrementAndGet() > description.getMaxSeqErrorsCount())
			invalidate();
	}	

	private void invalidate()
	{
		invalidated = true;
		
		notifications.addWarning(daemonName, logger,
				format("Too many errors in the Sender %s. It has been stopped.", id));
		
		dispose();
	}


	public String getId()
	{
		return id;
	}

	public MessageSenderDesc getDescription()
	{
		return description;
	}

	public Map<String, Object> getParameters()
	{
		return parameters;
	}

	public boolean isInvalidated()
	{
		return invalidated;
	}

	
	protected static class SendingCallback
	{
		private final AtomicInteger seqErrorsCounter;
		private final MessageLoadingStatistics loadingStatistics;

		private final OperationSettings operationSettings;
		private final boolean lastLinkedMsg;
		private final MessageScheduledTask task;

		public SendingCallback(AtomicInteger seqErrorsCounter, MessageLoadingStatistics loadingStatistics,
		                       OperationSettings operationSettings, boolean lastLinkedMsg, MessageScheduledTask task)
		{
			this.seqErrorsCounter = seqErrorsCounter;
			this.loadingStatistics = loadingStatistics;
			this.operationSettings = operationSettings;
			this.lastLinkedMsg = lastLinkedMsg;
			this.task = task;
		}
		
		public void onMessageSent()
		{
			loadingStatistics.incSentMessages(operationSettings.getName());
			
			int opsCount = calculateSentOperationsCount();
			if (opsCount != 0)
				loadingStatistics.addSentOperations(operationSettings.getName(), opsCount);
		}
		
		public void onResponseReceived()
		{
			loadingStatistics.incMessageResponses(operationSettings.getName());

			int opsCount = calculateSentOperationsCount();
			if (opsCount != 0)
				loadingStatistics.addOperationResponses(operationSettings.getName(), opsCount);
		}
		
		public void onTaskCompleted()
		{
			seqErrorsCounter.set(0);
		}
		
		private int calculateSentOperationsCount()
		{
			switch (operationSettings.getUnit())
			{
				case RG:
					return task.getBatchSize();
				case MESSAGE:
					return 1;
				case LINKED_MESSAGES:
					return lastLinkedMsg ? 1 : 0;
				default:
					return 0;
			}
		}
	}
}
