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

package com.exactprosystems.clearth.connectivity.connections;

import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.connectivity.listeners.factories.BasicMessageListenerFactory;
import com.exactprosystems.clearth.connectivity.listeners.factories.MessageListenerFactory;
import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.utils.SettingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

public abstract class BasicClearThMessageConnection extends BasicClearThRunnableConnection implements ClearThMessageConnection
{
	private static final Logger logger = LoggerFactory.getLogger(BasicClearThMessageConnection.class);
	protected ClearThClient client;
	@XmlElementWrapper
	protected final List<ListenerConfiguration> listeners = new CopyOnWriteArrayList<>();
	protected final MessageListenerFactory listenerFactory;

	protected final Lock readLock = lock.readLock();
	protected DataHandlersFactory dataHandlersFactory;

	public BasicClearThMessageConnection()
	{
		super();
		listenerFactory = createListenerFactory();
	}

	protected MessageListenerFactory createListenerFactory()
	{
		return new BasicMessageListenerFactory();
	}

	protected abstract ClearThClient createClient() throws SettingsException, ConnectivityException;

	@Override
	public EncodedClearThMessage sendMessage(Object message) throws ConnectivityException
	{
		return doSendMessage(() -> client.sendMessage(message));
	}
	
	@Override
	public EncodedClearThMessage sendMessage(EncodedClearThMessage message) throws ConnectivityException
	{
		return doSendMessage(() -> client.sendMessage(message));
	}
	
	
	@Override
	public DataHandlersFactory getDataHandlersFactory()
	{
		return dataHandlersFactory;
	}
	
	@Override
	public void setDataHandlersFactory(DataHandlersFactory dataHandlersFactory)
	{
		this.dataHandlersFactory = dataHandlersFactory;
	}
	
	
	protected EncodedClearThMessage doSendMessage(Callable<EncodedClearThMessage> caller) throws ConnectivityException
	{
		try
		{
			readLock.lock();
			if (client == null)
				throw new ConnectivityException("Client is not initialized for connection " + name);
			if (!client.isRunning())
				throw new ConnectivityException("Client is not running for connection " + name);

			return caller.call();
		}
		catch (Exception e)
		{
			throw new ConnectivityException("Error while sending message from connection " + name, e);
		}
		finally
		{
			readLock.unlock();
		}
	}

	@Override
	public void addListener(ListenerConfiguration listener)
	{
		listeners.add(listener);
	}

	@Override
	public void removeListener(ListenerConfiguration listener)
	{
		listeners.remove(listener);
	}

	@Override
	public void removeAllListeners()
	{
		listeners.clear();
	}

	@Override
	public List<ListenerConfiguration> getListeners()
	{
		return listeners;
	}

	@Override
	protected void startResources() throws ConnectivityException, SettingsException
	{
		startClient();
	}

	protected void startClient() throws ConnectivityException, SettingsException
	{
		try
		{
			client = createClient();
			client.addMessageListeners(createListeners(listeners));
			client.start(true);
		}
		catch (Exception e)
		{
			logger.error("Could not start connection '{}'.", name, e);

			disposeResources(e);

			if (e instanceof SettingsException)
				throw (SettingsException) e;
			else if (e instanceof ConnectivityException)
				throw (ConnectivityException) e;
			else
				throw new ConnectivityException(e, "Could not start connection '%s'.", name);
		}
	}

	@Override
	protected void restartResources() throws ConnectionException
	{
		if (client != null)
		{
			try
			{
				client.dispose(false);  //Keeping listeners to reuse the same instances after reconnect
			}
			catch (ConnectivityException e)
			{
				logger.warn("{}: errors occurred while closing client before reconnect. In spite of that " +
						"reconnect will be performed", name, e);
			}
			client = null;
		}

		try
		{
			client = createClient();
			for (ListenerConfiguration listener : listeners)
				client.addMessageListener(listener.getImplementation());
			client.start(false);  //Listeners are already running, started by previous client instance

		}
		catch (Exception e)
		{
			String msg = "Could not reconnect '"+name+"'";
			logger.error(msg, e);
			client = null;

			if (e instanceof ConnectionException)
				throw (ConnectionException)e;
			throw new ConnectionException(msg, e);
		}
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
				
				logger.debug("Adding listener '{}' ({}) to connection '{}'", cfg.getName(), cfg.getType(), name);

				MessageListener listenerImpl = createListener(cfg);

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
					logger.error("Error while disposing listener '{}' ({})", listener.getName(), listener.getType(),
							e1);
				}
			}

			if(e instanceof SettingsException)
				throw (SettingsException) e;
			else if(e instanceof ConnectivityException)
				throw (ConnectivityException) e;
			else
				throw new ConnectivityException(e);
		}
		return implementations;
	}

	protected MessageListener createListener(ListenerConfiguration listenerConfiguration)
			throws SettingsException, ConnectivityException
	{
		return listenerFactory.createListener(this, listenerConfiguration);
	}

	@Override
	protected void stopResources()
	{
		disposeResources(null);
	}

	protected void disposeResources(Exception cause)
	{
		try
		{
			if (client != null)
			{
				client.dispose(false);
				client = null;
			}
		}
		catch (Exception e)
		{
			logger.error("Error while disposing client of connection '{}'.", name, e);

			if (cause != null)
				cause.addSuppressed(e);
		}

		disposeListeners();

		//No need to keep listeners instances of disposed connection
		for (ListenerConfiguration listener : listeners)
			listener.setImplementation(null);
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
	
	@Override
	public Set<Class<? extends MessageListener>> getSupportedListenerTypes()
	{
		return listenerFactory.getSupportedListenerTypes();
	}

	@Override
	public Class<?> getListenerClass(String type)
	{
		return listenerFactory.getListenerClass(type);
	}

	@Override
	public void copyFrom(ClearThConnection other)
	{
		super.copyFrom(other);
		
		ClearThMessageConnection msgOther = (ClearThMessageConnection) other;
		dataHandlersFactory = msgOther.getDataHandlersFactory();
		if (!listeners.isEmpty())
			listeners.clear();
		for (ListenerConfiguration configuration : msgOther.getListeners())
			listeners.add(new ListenerConfiguration(configuration));
	}

	@Override
	public MessageListener findListener(String listenerType)
	{
		if (client == null)
			return null;
		
		return client.findListener(listenerType);
	}

	@Override
	public long getSent()
	{
		return client == null ? 0 : client.getSent();
	}
	
	@Override
	public long getReceived()
	{
		return client == null ? 0 : client.getReceived();
	}
}