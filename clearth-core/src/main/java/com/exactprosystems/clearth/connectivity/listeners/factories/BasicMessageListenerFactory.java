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

package com.exactprosystems.clearth.connectivity.listeners.factories;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.ListenerProperties;
import com.exactprosystems.clearth.connectivity.MessageListener;
import com.exactprosystems.clearth.connectivity.MultiCodec;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.exceptions.ListenerException;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;
import com.exactprosystems.clearth.connectivity.listeners.FileListener;
import com.exactprosystems.clearth.connectivity.listeners.ProxyListener;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.exactprosystems.clearth.connectivity.ListenerType.*;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

public class BasicMessageListenerFactory implements MessageListenerFactory
{
	private static final Logger logger = LoggerFactory.getLogger(BasicMessageListenerFactory.class);
	private final Map<String, Class<? extends MessageListener>> supportedListenerTypes;

	public BasicMessageListenerFactory()
	{
		this.supportedListenerTypes = Collections.unmodifiableMap(createTypesMap());
	}

	protected Map<String, Class<? extends MessageListener>> createTypesMap()
	{
		Map<String, Class<? extends MessageListener>> typesMap = new LinkedHashMap<>();
		typesMap.put(File.getLabel(), FileListener.class);
		typesMap.put(Proxy.getLabel(), ProxyListener.class);
		typesMap.put(Collector.getLabel(), ClearThMessageCollector.class);
		return typesMap;
	}

	public MessageListener createListener(ClearThConnection connection, ListenerConfiguration configuration)
			throws SettingsException, ListenerException
	{
		MessageListener listener;
		try
		{
			switch (listenerTypeByLabel(configuration.getType()))
			{
				case File :
				{
					listener = createFileListener(connection, configuration);
					break;
				}
				case Proxy :
				{
					listener = createProxyListener(connection, configuration);
					break;
				}
				case Collector :
				{
					listener = createMessageCollector(connection, configuration);
					break;
				}
				default :
				{
					listener = createListenerEx(connection, configuration);
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

	@Override
	public Map<String, Class<? extends MessageListener>> getSupportedListenerTypes()
	{
		return supportedListenerTypes;
	}

	protected ListenerProperties createProperties(ListenerConfiguration configuration)
	{
		return new ListenerProperties(configuration.getName(),
				configuration.getType(),
				configuration.isActive(),
				configuration.isActiveForSent());
	}

	protected FileListener createFileListener(ClearThConnection connection, ListenerConfiguration configuration)
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
	
	protected ProxyListener createProxyListener(ClearThConnection connection, ListenerConfiguration configuration)
			throws SettingsException, ListenerException
	{
		ClearThConnection con = ClearThCore.connectionStorage().getConnection(configuration.getSettings());
		if (con == null)
			throw new SettingsException(format("Target connection '%s' for listener '%s' (type '%s') not found",
					configuration.getSettings(), configuration.getName(), Proxy));

		ClearThMessageConnection msgCon;
		if (con instanceof ClearThMessageConnection)
			msgCon = (ClearThMessageConnection) con;
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

	protected MessageListener createMessageCollector(ClearThConnection connection, ListenerConfiguration configuration)
			throws SettingsException, ListenerException
	{
		Map<String, String> settings = KeyValueUtils.parseKeyValueString(configuration.getSettings(), ";", true);
		ListenerProperties properties = createProperties(configuration);
		String messageEndIndicator = getMessageCollectorMessageEndIndicator();
		String type = settings.get(ClearThMessageCollector.TYPE_SETTING);
		if (type == null)
			return new ClearThMessageCollector(properties, connection.getName(), settings, messageEndIndicator);


		return new ClearThMessageCollector(properties, connection.getName(), createCodec(type), settings, messageEndIndicator);
	}

	protected MessageListener createListenerEx(ClearThConnection connection, ListenerConfiguration configuration)
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

	@Override
	public Class<?> getListenerClass(String type)
	{
		return supportedListenerTypes.get(type);
	}
}