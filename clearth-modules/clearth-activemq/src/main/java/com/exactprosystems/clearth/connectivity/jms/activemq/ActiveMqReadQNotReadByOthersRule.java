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

package com.exactprosystems.clearth.connectivity.jms.activemq;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.mq.ClearThBasicMqConnectionSettings;
import com.exactprosystems.clearth.connectivity.validation.AbstractReadQNotReadByOthersRule;
import org.apache.commons.lang.StringUtils;

public class ActiveMqReadQNotReadByOthersRule extends AbstractReadQNotReadByOthersRule
{

	@Override
	public boolean isConnectionSuitable(ClearThConnection connectionToCheck)
	{
		return connectionToCheck instanceof ActiveMqConnection;
	}

	@Override
	protected boolean useSameReceiveQueue(ClearThBasicMqConnectionSettings settingsToCheck,
	                                      ClearThBasicMqConnectionSettings anotherSettings)
	{
		return StringUtils.equals(settingsToCheck.getReceiveQueue(), anotherSettings.getReceiveQueue())
				&& (settingsToCheck.getPort() == anotherSettings.getPort())
				&& hostsEquals(settingsToCheck.getHostname(), anotherSettings.getHostname());
	}
}
