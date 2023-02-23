/*******************************************************************************
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

package com.exactprosystems.clearth.connectivity.ibmmq;

import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.SettingsException;

import static com.exactprosystems.clearth.ClearThCore.valueGenerators;

public class DefaultIbmMqClient extends BasicIbmMqClient
{

	public DefaultIbmMqClient(IbmMqConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}

	@Override
	protected ValueGenerator createValueGenerator()
	{
		return valueGenerators().getGenerator("lastgenerated_"+this.name+".txt");
	}

	@Override
	protected SimpleIbmMqReceiverThread createReceiverThread()
	{
		IbmMqConnectionSettings settings = getSettings();
		return new SimpleIbmMqReceiverThread(name+" (Receiver thread)", getOwner(), receiveQueue, receivedMessageQueue,
				settings.getCharset(), settings.isAutoReconnect(), settings.getReadDelay());
	}

	protected IbmMqConnection getOwner()
	{
		return (IbmMqConnection) owner;
	}
}