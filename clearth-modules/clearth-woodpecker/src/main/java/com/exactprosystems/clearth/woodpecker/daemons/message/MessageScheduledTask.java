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

package com.exactprosystems.clearth.woodpecker.daemons.message;

import com.exactprosystems.clearth.woodpecker.configuration.settings.OperationSettings;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProvider;
import com.exactprosystems.clearth.woodpecker.execution.senders.MessageSender;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTask;
import com.exactprosystems.clearth.woodpecker.execution.workQueue.ScheduledTaskState;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;
import com.exactprosystems.clearth.woodpecker.message.data.TaskMessages;

public class MessageScheduledTask extends ScheduledTask
{
	private final MessageProvider provider;
	private final MessageSender sender;
	private final int batchSize;
	
	
	public MessageScheduledTask(MessageTaskDescription taskDescription, ScheduledTaskState taskState,
	                            long executeAfterNs, long executeBeforeNs, int batchSize, boolean availableImmediately)
	{
		super(taskDescription, taskState, executeAfterNs, executeBeforeNs, availableImmediately);
		provider = taskDescription.getProvider();
		sender = taskDescription.getSender();
		this.batchSize = batchSize;
	}


	public int getBatchSize()
	{
		return batchSize;
	}

	
	@Override
	public void execute(WoodpeckerFunctions functions)
	{
		if (!isExecutable())
			return;
		
		if (provider.isInvalidated() || sender.isInvalidated())
			return;

		taskState.onTaskInProgress(taskDescription.getTaskId(), calculateExpectedCountOfOperations());

		TaskMessages tms = provider.createMessages(functions, batchSize, sender.getParameters());
		if (tms == null)
			return;

		sender.send(tms, this);
	}

	
	private int calculateExpectedCountOfOperations()
	{
		OperationSettings settings = provider.getOperationSettings();
		switch (settings.getUnit())
		{
			case RG:
				return batchSize;
			case MESSAGE:
				return provider.getLinkedMessagesCount();
			case LINKED_MESSAGES:
				return 1;
			default:
				return 0;
		}
	}
}
