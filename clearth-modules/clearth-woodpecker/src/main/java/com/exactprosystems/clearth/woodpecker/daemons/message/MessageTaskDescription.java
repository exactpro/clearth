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

import com.exactprosystems.clearth.woodpecker.execution.distribution.TaskDescription;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProvider;
import com.exactprosystems.clearth.woodpecker.execution.senders.MessageSender;

public class MessageTaskDescription extends TaskDescription
{
	private final boolean batchEnabled;
	
	private final MessageProvider provider;
	private final MessageSender sender;

	public MessageTaskDescription(String taskId, int probability, int threadsCount, boolean batchEnabled,
	                              MessageProvider provider, MessageSender sender)
	{
		super(taskId, probability, threadsCount);
		this.batchEnabled = batchEnabled;
		this.provider = provider;
		this.sender = sender;
	}

	public boolean isBatchEnabled()
	{
		return batchEnabled;
	}

	public MessageProvider getProvider()
	{
		return provider;
	}

	public MessageSender getSender()
	{
		return sender;
	}

	@Override
	public boolean isInvalidated()
	{
		return provider.isInvalidated() || sender.isInvalidated();
	}
}
