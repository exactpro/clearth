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

import com.exactprosystems.clearth.connectivity.ibmmq.ClearThBasicMqConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.ibmmq.BasicIbmMqClient;
import com.exactprosystems.clearth.connectivity.ibmmq.IbmMqConnection;
import com.exactprosystems.clearth.connectivity.ibmmq.IbmMqConnectionSettings;
import org.apache.commons.lang.StringUtils;

import java.util.List;

import static com.exactprosystems.clearth.ClearThCore.getInstance;
import static org.apache.commons.lang.StringUtils.isBlank;

public class IbmMqReadQNotReadByOthersRule extends AbstractReadQNotReadByOthersRule
{
	@Override
	public boolean isConnectionSuitable(ClearThConnection connectionToCheck)
	{
		return connectionToCheck instanceof IbmMqConnection;
	}

	
	@Override
	protected List<ClearThMessageConnection> getOtherConnections(ClearThMessageConnection connection)
	{
		return getInstance()
				.getConnectionStorage()
				.getConnections(connection.getTypeInfo().getName(),
						(con) -> ((ClearThMessageConnection) con).isRunning(),
				                ClearThMessageConnection.class);
	}


	@Override
	protected boolean useSameReceiveQueue(ClearThBasicMqConnectionSettings settingsToCheck1,
	                                      ClearThBasicMqConnectionSettings anotherSettings1)
	{
		IbmMqConnectionSettings settingsToCheck = (IbmMqConnectionSettings) settingsToCheck1;
		IbmMqConnectionSettings anotherSettings = (IbmMqConnectionSettings) anotherSettings1;
		return StringUtils.equals(settingsToCheck.getReceiveQueue(), anotherSettings.getReceiveQueue())
				&& StringUtils.equals(settingsToCheck.getQueueManager(), anotherSettings.getQueueManager())
				&& (getPort(settingsToCheck) == getPort(anotherSettings))
				&& hostsEquals(getHostname(settingsToCheck), getHostname(anotherSettings));
	}
	
	private int getPort(ClearThBasicMqConnectionSettings settings)
	{
		return (settings.getPort() > 0) ? settings.getPort() : BasicIbmMqClient.DEFAULT_PORT;
	}
	
	private String getHostname(ClearThBasicMqConnectionSettings settings)
	{
		return isBlank(settings.getHostname()) ? BasicIbmMqClient.DEFAULT_HOST : settings.getHostname();
	}
}
