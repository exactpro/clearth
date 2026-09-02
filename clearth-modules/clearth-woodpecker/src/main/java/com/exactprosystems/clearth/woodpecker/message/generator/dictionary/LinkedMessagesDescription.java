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

package com.exactprosystems.clearth.woodpecker.message.generator.dictionary;

import com.exactprosystems.clearth.woodpecker.configuration.settings.OperationSettings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LinkedMessagesDescription implements Iterable<MessageDescription>
{
	private final OperationSettings operationSettings;
	private final String codecName;
	private final boolean batchEnabled;
	private final List<MessageDescription> messageDescriptions = new ArrayList<>();
	
	public LinkedMessagesDescription(OperationSettings operationSettings, String codecName, boolean batchEnabled)
	{
		this.operationSettings = operationSettings;
		this.codecName = codecName;
		this.batchEnabled = batchEnabled;
	}

	public OperationSettings getOperationSettings()
	{
		return operationSettings;
	}

	public String getCodecName()
	{
		return codecName;
	}

	public boolean isBatchEnabled()
	{
		return batchEnabled;
	}

	public int size()
	{
		return messageDescriptions.size();
	}

	public List<MessageDescription> getMessageDescs()
	{
		return messageDescriptions;
	}

	public void addMessageDesc(MessageDescription messageDescription)
	{
		messageDescriptions.add(messageDescription);
	}

	@Override
	public Iterator<MessageDescription> iterator()
	{
		return messageDescriptions.iterator();
	}
}
