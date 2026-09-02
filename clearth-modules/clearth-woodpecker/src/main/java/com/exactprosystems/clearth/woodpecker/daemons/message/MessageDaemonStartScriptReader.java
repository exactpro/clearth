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

import com.exactprosystems.clearth.woodpecker.configuration.start.MessageDaemonStartScript;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageProviderDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageSenderDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.StartFileElement;
import com.exactprosystems.clearth.woodpecker.configuration.start.StartScriptReader;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException;

import java.nio.file.Path;
import java.util.*;

import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerConfigFile.START_FILE;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerConfigException.whenInvalidContent;
import static java.lang.String.format;

public class MessageDaemonStartScriptReader extends StartScriptReader<MessageDaemonStartScript>
{
	
	public MessageDaemonStartScriptReader(Path replicaBaseDir, 
	                                      Class<? extends MessageDaemonStartScript> extendedStartScriptClass,
	                                      Class... otherExtendedClasses)
	{
		super(replicaBaseDir, extendedStartScriptClass, otherExtendedClasses);
	}
	
	
	public List<MessageProviderDesc> getProviderDescs()
	{
		return startScript.getMessageProvidersBlock().getProviderDescs();
	}

	public List<MessageSenderDesc> getSenderDescs()
	{
		return startScript.getMessageSendersBlock().getEnabledSenders();
	}
	

	@Override
	protected void checkContent(MessageDaemonStartScript startScript, Path path) throws WoodpeckerConfigException
	{
		if (startScript.getMessageProvidersBlock() == null)
			throw whenInvalidContent(path, START_FILE.description(),
					String.format("Required %s is absent.", StartFileElement.PROVIDERS_BLOCK));
		
		if (startScript.getMessageSendersBlock() == null)
			throw whenInvalidContent(path, START_FILE.description(),
					format("Required %s is absent.", StartFileElement.SENDERS_BLOCK));
	}
	
	
	@Override
	protected Set<String> findExecutableOperationNames(MessageDaemonStartScript startScript) throws WoodpeckerConfigException
	{
		Set<String> result = new LinkedHashSet<>();
		
		Set<String> fromProviders = startScript.getMessageProvidersBlock().getOperationNames();
		Set<String> fromSenders = startScript.getMessageSendersBlock().getOperationNames();
		
		for (String opName : fromProviders)
		{
			if (fromSenders.contains(opName))
				result.add(opName);
		}
		
		if (result.isEmpty())
			throw new WoodpeckerConfigException("Nothing to execute. " +
					"No Provider-Sender pairs linked by operationName are present in Start script.");
		else 
			return result;
	}
}
