/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.connections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlElementWrapper;

import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;

import org.slf4j.Logger;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.listeners.FileListener;
import com.exactprosystems.clearth.connectivity.listeners.ProxyListener;
import com.exactprosystems.clearth.messages.PlainMessageSender;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;

import static com.exactprosystems.clearth.connectivity.ListenerType.Proxy;
import static com.exactprosystems.clearth.connectivity.ListenerType.listenerTypeByLabel;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class ClearThMessageConnection<C extends ClearThMessageConnection<C,S>, 
		S extends ClearThConnectionSettings<S>>
		extends ClearThConnection<C, S> implements PlainMessageSender
{
	@XmlElementWrapper
	protected List<ListenerConfiguration> listeners = new ArrayList<>();
	
	protected ClearThClient client;
	
	public ClearThMessageConnection()
	{
		super();
	}
	
	@Override
	public Object sendMessage(Object message) throws IOException, ConnectivityException
	{
		checkRunning();
		return client.sendMessage(message);
	}

	@Override
	public Object sendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		checkRunning();
		return client.sendMessage(message);
	}

	public void copy(C copyFrom)  //Used when changing settings of connection by supplying settings in another one, but new instance shouldn't be created
	{
		this.name = copyFrom.name;
		this.settings = copyFrom.settings.copy();
		this.type = copyFrom.getType();
		
		if (copyFrom.listeners==null)
			this.listeners = null;
		else
		{
			this.listeners = new ArrayList<>(copyFrom.listeners.size());
			for (ListenerConfiguration configuration : copyFrom.listeners)
			{
				this.listeners.add(new ListenerConfiguration(configuration));
			}
		}
		this.client = copyFrom.client;
	}
	
	public abstract Logger getLogger();
	
	
	protected abstract MessageListener createListenerEx(ListenerProperties props, String settings) throws SettingsException, ConnectivityException;
	protected abstract MessageListener createMessageCollector(ListenerProperties props, Map<String, String> settings) throws SettingsException, ConnectivityException;
	public abstract Class<?> getMessageCollectorClass();
	protected abstract Class<?> getListenerClassEx(String type);
	
	public MessageListener findListener(String type)
	{
		if (getLogger().isDebugEnabled())
		{
			for (ListenerConfiguration cfg : listeners)
				getLogger().debug(cfg.toString());
		}
		
		for (ListenerConfiguration cfg : listeners)
		{
			if (cfg.getType().equalsIgnoreCase(type))
				return cfg.getImplementation();
		}
		return null;
	}
	
	public MessageListener findListener(Class<?> listenerClass)
	{
		for (ListenerConfiguration cfg : listeners)
			if (getListenerClass(cfg.getType()).equals(listenerClass))
				return cfg.getImplementation();
		return null;
	}

	public List<ListenerConfiguration> getListeners()
	{
		if (listeners != null)
			return listeners;
		else return (listeners = new ArrayList<>());
	}

	public void setListeners(List<ListenerConfiguration> listeners)
	{
		this.listeners = listeners;
	}

	public void addListener(ListenerConfiguration listener)
	{
		this.listeners.add(listener);
	}

	public void removeListener(ListenerConfiguration listener)
	{
		this.listeners.remove(listener);
	}
	
	protected List<MessageListener> createListeners(List<ListenerConfiguration> configurations)
		throws SettingsException, ConnectivityException
	{
		List<MessageListener> implementations = new ArrayList<>(configurations.size());
		
		try
		{
			for (ListenerConfiguration cfg : configurations)
			{
				if (!cfg.isActive() && !cfg.isActiveForSent())
					continue;
				
				String listenerName = cfg.getName();
				String listenerType = cfg.getType();
				getLogger().debug("Adding listener '{}' ({}) to connection '{}'", listenerName, listenerType, name);
				
				ListenerProperties props = new ListenerProperties(listenerName, listenerType, cfg.isActive(), cfg.isActiveForSent());
				MessageListener listenerImpl = createListener(props, cfg.getSettings());
				
				cfg.setImplementation(listenerImpl);
				implementations.add(listenerImpl);
			}
		}
		catch (Exception e)
		{
			for (MessageListener listener : implementations)
			{
				try
				{
					listener.dispose();
				}
				catch (Exception e1)
				{
					getLogger().error("Error while disposing listener '{}' ({})", listener.getName(), listener.getType(), e1);
				}
			}
			
			if(e instanceof SettingsException)
				throw (SettingsException) e;
			else if(e instanceof ConnectivityException)
				throw (ConnectivityException) e;
			else
				throw (RuntimeException) e;
		}
		return implementations;
	}

	protected MessageListener createListener(ListenerProperties props, String settings) 
			throws SettingsException, ConnectivityException
	{
		MessageListener listener;
		Map<String, String> settingsMap = KeyValueUtils.parseKeyValueString(settings, ";", true);
		try
		{
			switch (listenerTypeByLabel(props.getType()))
			{
				case File :
				{
					listener = createFileListener(props, settings);
					break;
				}
				case Proxy :
				{
					listener = createProxyListener(props, settings);
					break;
				}
				case Collector :
				{
					listener = createMessageCollector(props, settingsMap);
					break;
				}
				default : 
				{
					listener = createListenerEx(props, settings);
					break;
				}
			}
		}
		catch (SettingsException e)
		{
			String msg = format("Could not create listener with '%s' (%s)", props.getName(), props.getType());
			getLogger().error(msg, e);
			throw new SettingsException(msg + ". " + e.getMessage());
		}
		
		if (listener == null)
			throw new SettingsException(format("Listener '%s' has unknown type '%s'.", name, type));
		
		// log listener settings
		if (getLogger().isInfoEnabled() && !settingsMap.isEmpty())
		{
			StringBuilder settingsString = new StringBuilder();		
			for (Entry<String, String> setting : settingsMap.entrySet())
			{
				settingsString.append(Utils.EOL).append(setting.getKey()).append("=").append(setting.getValue());
			}
			getLogger().info("Listener '"+name+"' ("+type+") settings:"+ settingsString);
		}
		
		return listener;
	}
	
	protected FileListener createFileListener(ListenerProperties props, String fileName) throws SettingsException, ConnectivityException
	{
		if (isBlank(fileName))
			throw new SettingsException("Could not create listener for file. Please specify file's path in listener's settings.");		
		try
		{
			return new FileListener(props, fileName);
		}
		catch (IOException e)
		{
			getLogger().error("Could not create listener for file '" + fileName + "'", e);
			throw new ConnectivityException("Could not create listener for file. Listener settings might be incorrect.", e);
		}
	}
	
	protected ProxyListener createProxyListener(ListenerProperties props, String connectionName) throws SettingsException, ConnectivityException
	{
		ClearThConnection<?,?> con = ClearThCore.connectionStorage().findConnection(connectionName);
		if (con == null)
			throw new SettingsException(format("Target connection '%s' for listener '%s' (type '%s') not found",
					connectionName, props.getName(), Proxy));
		
		ClearThMessageConnection<?,?> msgCon;
		if (isMessageConnection(con))
			msgCon = (ClearThMessageConnection<?,?>)con;
		else 
			throw new SettingsException(format("Target connection '%s' for listener '%s' doesn't support Proxies.", 
					connectionName, props.getName()));
			
		try
		{
			return new ProxyListener(props, msgCon);
		}
		catch (Exception e)
		{
			throw new ConnectivityException(format("Could not create '%s' listener '%s' for connection '%s'.", 
					Proxy, props.getName(), connectionName), e);
		}
	}
	

	public Class<?> getListenerClass(String type)
	{
		switch (listenerTypeByLabel(type))
		{
		case File : return FileListener.class;
		case Proxy : return ProxyListener.class;
		case Collector : return getMessageCollectorClass();
		default : return getListenerClassEx(type);
		}
	}
	
	
	public long getSent()
	{
		return client != null ? client.getSent() : 0;
	}
	
	public long getReceived()
	{
		return client != null ? client.getReceived() : 0;
	}
	
	public long getWarnings()
	{
		return client != null ? client.getWarnings() : 0;
	}
	
	
	public static boolean isMessageConnection(ClearThConnection<?,?> connection)
	{
		return connection instanceof ClearThMessageConnection;
	}

	protected void disposeListeners()
	{
		for (ListenerConfiguration configuration : listeners)
		{
			MessageListener impl = configuration.getImplementation();
			if (impl != null)
				impl.dispose();
		}
	}
	
	
	protected ICodec createCodec(String type) throws SettingsException
	{
		String[] names = type.split(",");
		if (names.length < 2)
			return ClearThCore.getInstance().createCodec(type);
		return new MultiCodec(names);
	}
	
	
	private void checkRunning() throws ConnectionException
	{
		if (!running)
			throw new ConnectionException("Connection '"+name+"' is not running");
	}
}
