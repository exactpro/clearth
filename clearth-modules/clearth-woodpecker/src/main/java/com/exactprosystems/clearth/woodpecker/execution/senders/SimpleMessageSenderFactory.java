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

package com.exactprosystems.clearth.woodpecker.execution.senders;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.woodpecker.configuration.start.SimpleMessageSenderDesc;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageSenderDesc;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageLoadingStatistics;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;

public class SimpleMessageSenderFactory implements MessageSenderFactory
{
	@Override
	public MessageSender create(String daemonName, WoodpeckerNotifications notifications,
			MessageLoadingStatistics loadingStatistics, DaemonContext context, MessageSenderDesc description,
			Path configsDirPath) throws WoodpeckerException
	{
		SimpleMessageSenderDesc msd = (SimpleMessageSenderDesc)description;
		ClearThMessageConnection con = getConnection(msd.getConnectionName());
		
		String id = description.getOperationName()+"_"+msd.getConnectionName();
		return new SimpleMessageSender(id, daemonName, notifications, loadingStatistics, description, con);
	}
	
	
	protected ClearThMessageConnection getConnection(String name) throws WoodpeckerDaemonException
	{
		if (StringUtils.isEmpty(name))
			throw new WoodpeckerDaemonException("Sender must have 'connectionName' attribute");
		
		try
		{
			ClearThConnection result = ClearThCore.connectionStorage().findRunningConnection(name);
			if (!(result instanceof ClearThMessageConnection))
				throw new WoodpeckerDaemonException("Connection '"+name+"' is not a message connection");
			return (ClearThMessageConnection) result;
		}
		catch (ConnectivityException e)
		{
			throw new WoodpeckerDaemonException("Could not get connection '"+name+"'", e);
		}
	}
}
