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

package com.exactprosystems.clearth.connectivity.connections2.listeners;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.connections2.exceptions.ListenerException;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;
import com.exactprosystems.clearth.connectivity.listeners.FileListener;
import com.exactprosystems.clearth.connectivity.listeners.ProxyListener;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static com.exactprosystems.clearth.connectivity.ListenerType.Proxy;
import static com.exactprosystems.clearth.connectivity.ListenerType.listenerTypeByLabel;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

public class BasicMessageListenerFactory implements MessageListenerFactory
{
	private static final Logger logger = LoggerFactory.getLogger(BasicMessageListenerFactory.class);

	public MessageListener createListener(String connectionName, ListenerConfiguration configuration)
			throws SettingsException, ListenerException
	{
		MessageListener listener;
		try
		{
			switch (listenerTypeByLabel(configuration.getType()))
			{
				case File :
				{
					listener = createFileListener(connectionName, configuration);
					break;
				}
				case Proxy :
				{
					listener = createProxyListener(connectionName, configuration);
					break;
				}
				case Collector :
				{
					listener = createMessageCollector(connectionName, configuration);
					break;
				}
				default :
				{
					listener = createListenerEx(connectionName, configuration);
					break;
				}
			}
		}
		catch (SettingsException e)
		{
			String msg = format("Could not create listener with name '%s' of type '%s'", configuration.getName(),
					configuration.getType());
			logger.error(msg, e);
			throw new SettingsException(msg + ". " + e.getMessage());
		}

		if (listener == null)
			throw new SettingsException(format("Listener '%s' has unknown type '%s'.", configuration.getName(),
					configuration.getType()));

		return listener;
	}

	private ListenerProperties createProperties(ListenerConfiguration configuration)
	{
		return new ListenerProperties(configuration.getName(),
				configuration.getType(),
				configuration.isActive(),
				configuration.isActiveForSent());
	}

	protected FileListener createFileListener(String connectionName, ListenerConfiguration configuration)
			throws SettingsException, ListenerException
	{
		ListenerProperties properties = createProperties(configuration);
		String fileName = configuration.getSettings();
		if (isBlank(fileName))
			throw new SettingsException("Could not create listener for file. Please specify file's path in listener's settings.");
		try
		{
			return new FileListener(properties, fileName);
		}
		catch (IOException e)
		{
			logger.error("Could not create listener for file '" + fileName + "'", e);
			throw new ListenerException("Could not create listener for file. Listener settings might be incorrect.", e);
		}
	}
	/*
		FIXME replace old version of connection with a new one when it will become possible
		It will become possible when we'll have new versions of ConnectionStorage and ProxyListner
	 */
	protected ProxyListener createProxyListener(String connectionName, ListenerConfiguration configuration)
			throws SettingsException, ListenerException
	{
		com.exactprosystems.clearth.connectivity.connections.ClearThConnection<?, ?> con =
				ClearThCore.connectionStorage().findConnection(configuration.getSettings());
		if (con == null)
			throw new SettingsException(format("Target connection '%s' for listener '%s' (type '%s') not found",
					configuration.getSettings(), configuration.getName(), Proxy));

		com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection<?, ?> msgCon;
		if (con instanceof com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection)
			msgCon = (com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection<?, ?>) con;
		else
			throw new SettingsException(format("Target connection '%s' for listener '%s' does not support messages.",
					configuration.getSettings(), configuration.getName()));

		try
		{
			return new ProxyListener(createProperties(configuration), msgCon);
		}
		catch (Exception e)
		{
			throw new ListenerException(format("Could not create '%s' listener '%s' for connection '%s'.",
					Proxy, configuration.getName(), configuration.getSettings()), e);
		}
	}

	protected MessageListener createMessageCollector(String connectionName, ListenerConfiguration configuration)
			throws SettingsException, ListenerException
	{
		Map<String, String> settings = KeyValueUtils.parseKeyValueString(configuration.getSettings(), ";", true);
		ListenerProperties properties = createProperties(configuration);
		String messageEndIndicator = getMessageCollectorMessageEndIndicator();
		String type = configuration.getType();
		if (type == null)
			return new ClearThMessageCollector(properties, connectionName, settings, messageEndIndicator);


		return new ClearThMessageCollector(properties, connectionName, createCodec(type), settings, messageEndIndicator);
	}

	protected MessageListener createListenerEx(String connectionName, ListenerConfiguration configuration)
			throws SettingsException, ListenerException
	{
		return null;
	}

	protected String getMessageCollectorMessageEndIndicator()
	{
		return ClearThMessageCollector.DEFAULT_MESSAGE_END_INDICATOR;
	}

	protected ICodec createCodec(String type) throws SettingsException
	{
		String[] names = type.split(",");
		if (names.length < 2)
			return ClearThCore.getInstance().createCodec(type);
		return new MultiCodec(names);
	}
}