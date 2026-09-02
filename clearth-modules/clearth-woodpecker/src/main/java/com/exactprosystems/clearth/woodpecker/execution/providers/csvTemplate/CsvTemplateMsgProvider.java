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

package com.exactprosystems.clearth.woodpecker.execution.providers.csvTemplate;

import com.exactprosystems.clearth.woodpecker.configuration.start.MessageProviderDesc;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;
import com.exactprosystems.clearth.woodpecker.message.data.LinkedMessages;
import com.exactprosystems.clearth.woodpecker.message.generator.MessageGenerator;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import com.exactprosystems.clearth.woodpecker.execution.providers.MessageProvider;
import com.exactprosystems.clearth.woodpecker.message.generator.dictionary.LinkedMessagesDescription;

import java.util.Map;

import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;

public class CsvTemplateMsgProvider extends MessageProvider
{
	private final MessageGenerator generator;
	private final LinkedMessagesDescription linkedMessagesDescription;
	
	CsvTemplateMsgProvider(String id,
	                       String daemonName,
	                       WoodpeckerNotifications notifications,
	                       MessageProviderDesc description,
	                       LinkedMessagesDescription linkedMessagesDescription)
	{
		super(id, daemonName, notifications, description, linkedMessagesDescription.getOperationSettings());
		this.generator = woodpecker().getMessageGenerator();
		this.linkedMessagesDescription = linkedMessagesDescription;
	}
	

	@Override
	protected LinkedMessages createMessagesImpl(WoodpeckerFunctions functions,
	                                            Map<String, Object> externalParameters) throws WoodpeckerException
	{
		return generator.generate(linkedMessagesDescription,
				operationSettings.getUnit(),
				functions,
				externalParameters);
	}

	@Override
	public int getLinkedMessagesCount()
	{
		return linkedMessagesDescription.size();
	}
}
