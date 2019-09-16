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

package com.exactprosystems.clearth.connectivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.utils.SettingsException;

import static com.exactprosystems.clearth.ClearThCore.valueGenerators;

public class DefaultMQClient extends MQClient
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultMQClient.class);
	
	public DefaultMQClient(MQConnection owner) throws ConnectionException, SettingsException
	{
		super(owner);
	}
	
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}
	
	@Override
	protected ValueGenerator getValueGenerator()
	{
		return valueGenerators().getGenerator("lastgenerated_"+this.name+".txt", "");
	}

	@Override
	protected MessageProcessorThread createProcessorThread()
	{
		return new MessageProcessorThread(name+" (Processor thread)", messageQueue, receiveListeners);
	}

	@Override
	protected MessageReceiverThread createReceiverThread()
	{
		return new SimpleReceiverThread(name+" (Receiver thread)", owner, receiveQueue, messageQueue, storedSettings.charset, storedSettings.autoReconnect, storedSettings.readDelay);
	}
}
