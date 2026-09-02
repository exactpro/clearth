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

package com.exactprosystems.clearth.woodpecker.message.data;

import com.exactprosystems.clearth.woodpecker.configuration.settings.OperationSettings;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class TaskMessages implements Iterable<LinkedMessages>
{
	private final List<LinkedMessages> messages;
	private final OperationSettings operationSettings;
	
	public TaskMessages(OperationSettings operationSettings) 
	{
		this.operationSettings = operationSettings;
		messages = new ArrayList<>();
	}
	
	public TaskMessages(LinkedMessages lms, OperationSettings operationSettings)
	{
		this(operationSettings);
		addMessages(lms);
	}

	public List<LinkedMessages> getMessages()
	{
		return messages;
	}

	public void addMessages(LinkedMessages lms)
	{
		messages.add(lms);
	}

	@Override
	public Iterator<LinkedMessages> iterator()
	{
		return messages.iterator();
	}

	public OperationSettings getOperationSettings()
	{
		return operationSettings;
	}
}
