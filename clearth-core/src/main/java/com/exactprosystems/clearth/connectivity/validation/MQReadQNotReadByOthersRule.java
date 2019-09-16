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

package com.exactprosystems.clearth.connectivity.validation;

import static com.exactprosystems.clearth.ClearThCore.*;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.List;

import com.exactprosystems.clearth.connectivity.MQClient;
import com.exactprosystems.clearth.connectivity.MQConnectionSettings;
import org.apache.commons.lang.StringUtils;

import com.exactprosystems.clearth.connectivity.MQConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;

public class MQReadQNotReadByOthersRule extends AbstractReadQNotReadByOthersRule<MQConnection, MQConnectionSettings>
{

	@Override
	public boolean isConnectionSuitable(ClearThConnection<?, ?> connectionToCheck)
	{
		return connectionToCheck instanceof MQConnection;
	}

	
	@Override
	protected List<MQConnection> getOtherConnections(MQConnection connection)
	{
		return getInstance()
				.getConnectionStorage()
				.getConnections(connection.getType(),
				                ClearThConnection::isRunning,
				                MQConnection.class);
	}


	@Override
	protected boolean useSameReceiveQueue(MQConnectionSettings settingsToCheck, MQConnectionSettings anotherSettings)
	{
		return StringUtils.equals(settingsToCheck.receiveQueue, anotherSettings.receiveQueue)
				&& StringUtils.equals(settingsToCheck.queueManager, anotherSettings.queueManager)
				&& (getPort(settingsToCheck) == getPort(anotherSettings))
				&& hostsEquals(getHostname(settingsToCheck), getHostname(anotherSettings));
	}
	
	private int getPort(MQConnectionSettings settings)
	{
		return (settings.port > 0) ? settings.port : MQClient.DEFAULT_PORT;
	}
	
	private String getHostname(MQConnectionSettings settings)
	{
		return isBlank(settings.hostname) ? MQClient.DEFAULT_HOST : settings.hostname;
	}
}
