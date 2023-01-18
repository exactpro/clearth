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

package com.exactprosystems.clearth.connectivity.connections2;

import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.connections2.listeners.BasicMessageListenerFactory;
import com.exactprosystems.clearth.connectivity.connections2.listeners.MessageListenerFactory;
import com.exactprosystems.clearth.utils.SettingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlElementWrapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class BasicClearThMessageConnection extends BasicClearThConnection implements ClearThMessageConnection
{
	private static final Logger logger = LoggerFactory.getLogger(BasicClearThMessageConnection.class);
	protected ClearThClient client;
	@XmlElementWrapper
	protected final List<ListenerConfiguration> listeners = new ArrayList<>();
	protected MessageListenerFactory listenerFactory;

	protected RunnableConnectionState connectionState;
	protected ReadWriteLock lock = new ReentrantReadWriteLock();
	// ...
	protected Lock writeLock = lock.writeLock();
	protected Lock readLock = lock.readLock();

	public BasicClearThMessageConnection()
	{
		super();
		connectionState = createConnectionState();
		listenerFactory = createListenerFactory();
	}

	protected RunnableConnectionState createConnectionState()
	{
		return new RunnableConnectionState(name);
	}

	private MessageListenerFactory createListenerFactory()
	{
		return new BasicMessageListenerFactory();
	}

	protected abstract ClearThClient createClient() throws SettingsException, ConnectionException;

	@Override
	public Object sendMessage(Object message) throws ConnectivityException
	{
		try
		{
			readLock.lock();
			return client.sendMessage(message);
		}
		catch (IOException e)
		{
			throw new ConnectivityException("Error while sending message " + message + " from connection " + name, e);
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
	public List<ListenerConfiguration> getListeners()
	{
		return listeners;
	}

	@Override
	public boolean isRunning()
	{
		return connectionState.isRunning();
	}

	@Override
	public LocalDateTime getStarted()
	{
		return connectionState.getStartTime();
	}

	@Override
	public LocalDateTime getStopped()
	{
		return connectionState.getStopTime();
	}

	@Override
	public void start() throws ConnectivityException, SettingsException
	{
		try
		{
			writeLock.lock();
			if (connectionState.cantBeStarted())
				return;

			connectionState.reset();

			logger.debug("Trying to start connection '{}'.", name);

			startClient();

			connectionState.start();

			logger.info("Connection '{}' is now running.", name);
		}
		finally
		{
			writeLock.unlock();
		}
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
	public void stop() throws ConnectivityException
	{
		try
		{
			writeLock.lock();
			if (connectionState.cantBeStopped())
				return;

			logger.debug("Trying to stop connection '{}'.", name);

			disposeResources();

			connectionState.stop();

			logger.info("Connection '{}' is now stopped.", name);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	@Override
	public void restart() throws ConnectivityException, SettingsException
	{
		try
		{
			writeLock.lock();
			connectionState.checkCanBeReconnected();

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

				connectionState.restart();
				logger.info("'{}' reconnected", name);
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
		finally
		{
			writeLock.unlock();
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
				throw (RuntimeException) e;
		}
		return implementations;
	}

	protected MessageListener createListener(ListenerConfiguration listenerConfiguration)
			throws SettingsException, ConnectivityException
	{
		return listenerFactory.createListener(this, listenerConfiguration);
	}

	protected void disposeResources()
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
}