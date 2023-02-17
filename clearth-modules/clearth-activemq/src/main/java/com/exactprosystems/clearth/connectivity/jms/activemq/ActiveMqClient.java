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

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.jms.JmsClient;
import com.exactprosystems.clearth.connectivity.jms.JmsConnectionSettings;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;

import javax.jms.ConnectionFactory;

public class ActiveMqClient extends JmsClient
{
	public ActiveMqClient(ActiveMqConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}

	@Override
	protected ConnectionFactory createConnectionFactory()
	{
		JmsConnectionSettings jmsSettings = (JmsConnectionSettings) storedSettings;
		String hostname = jmsSettings.getHostname();
		int port = jmsSettings.getPort();
		return new ActiveMQConnectionFactory("tcp://" + hostname + ":" + port);
	}

	@Override
	public boolean isClosedSession()
	{
		if (session == null)
			return false;
		else if (session instanceof ActiveMQSession)
			return ((ActiveMQSession) session).isClosed();
		throw new IllegalStateException("Could not get session state. Session class name: '"
				+ session.getClass().getSimpleName() + "', but expected '" + ActiveMQSession.class.getSimpleName() + "'");
	}
}
