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

package com.exactprosystems.clearth.connectivity.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.ListenerDescription;
import com.exactprosystems.clearth.connectivity.ListenerProperties;
import com.exactprosystems.clearth.connectivity.ReceiveListener;
import com.exactprosystems.clearth.connectivity.SendListener;
import com.exactprosystems.clearth.connectivity.SettingsDetails;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.AbstractMessageListener;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;

@ListenerDescription(description = "ClearTH proxy listener")
@SettingsDetails(details = "Name of a connection which will send messages.")
public class ProxyListener extends AbstractMessageListener implements ReceiveListener
{
	protected Logger logger = LoggerFactory.getLogger(ProxyListener.class);
	protected ClearThMessageConnection<?,?> connector;

	/**
	 * Create ProxyListener
	 * @param properties listener properties (name, active directions)
	 * @param connector another connection to re-send messages with
	 */
	public ProxyListener(ListenerProperties properties, ClearThMessageConnection<?,?> connector) throws Exception
	{
		super(properties);
		
		this.connector = connector;
		if (!connector.isRunning())
		{
			logger.trace("Initializing connection '{}'", connector.getName());
			try
			{
				connector.start();
			}
			catch (Exception e)
			{
				logger.error("Error while starting connection '{}'", connector.getName(), e);
				throw e;
			}
		}
	}
	
	@Override
	public boolean isActiveForReceived()
	{
		return getProperties().isActiveForReceived();
	}
	
	@Override
	public void start()
	{
	}
	
	@Override
	public void onMessage(EncodedClearThMessage message)
	{
		logger.trace("Retransmitting message {}", message.getPayload());
		try
		{
			connector.sendMessage(message);
		}
		catch (Exception e)
		{
			logger.warn("Error while resending message through connection '{}'", connector.getName(), e);
		}
	}
	
	@Override
	public void dispose()
	{
		if (connector.isRunning())
		{
			logger.trace("Stopping connection '{}'", connector.getName());
			try
			{
				connector.stop();
			}
			catch (Exception e)
			{
				logger.error("Error while stopping connection '{}'", connector.getName(), e);
			}
		}
	}
}
