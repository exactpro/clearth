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

package com.exactprosystems.clearth.connectivity.connections;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ClearThClient;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.ReceiveListener;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Basic implementation of ClearThMessageConnection that can be used 
 * for creation of new Connections in Projects.
 * 
 * 08 April 2019
 */
public abstract class BasicClearThMessageConnection<C extends BasicClearThMessageConnection<C, S>,
		S extends ClearThConnectionSettings<S>>
	extends ClearThMessageConnection<C, S>
{
	private static final Logger log = LoggerFactory.getLogger(BasicClearThMessageConnection.class);

	protected final Object managingMonitor = new Object(); // Monitor for managing operations: start and stop.
	// Actually it is better to use ReadWriteLock here: write lock for managing operations (start, stop, ...)
	// and read lock for using (send message, ...).
	//TODO: implement it.
	
	
	protected abstract ClearThClient createClient() throws ConnectivityException, SettingsException;


	@Override
	public final void start() throws ConnectivityException, SettingsException
	{
		if (cantBeStarted())
			return;
		
		synchronized (managingMonitor)
		{
			if (cantBeStarted())
				return;
			
			started = null;
			stopped = null;

			log.debug("Trying to start connection '{}'.", name);
			try
			{
				client = createClient();
				client.addReceiveListeners(createListeners(listeners));
				client.start(true);
			}
			catch (Exception e)
			{
				log.error("Could not start connection '{}'.", name, e);
				
				disposeResources(e);
				
				if (e instanceof SettingsException)
					throw (SettingsException) e;
				else if (e instanceof ConnectivityException)
					throw (ConnectivityException) e;
				else 
					throw new ConnectivityException(e, "Could not start connection '%s'.", name);
			}

			started = new Date();
			running = true;
			
			log.info("Connection '{}' is now running.", name);
		}
	}
	
	protected boolean cantBeStarted()
	{
		boolean running = isRunning();
		if (running)
			log.warn("Connection '{}' is already running.", name);
		return running;
	}


	@Override
	public final void stop() throws Exception
	{
		if (cantBeStopped())
			return;
		
		synchronized (managingMonitor)
		{
			if (cantBeStopped())
				return;
			
			log.debug("Trying to stop connection '{}'.", name);
			
			disposeResources();

			stopped = new Date();
			running = false;

			log.info("Connection '{}' is now stopped.", name);
		}
	}
	
	protected boolean cantBeStopped()
	{
		boolean notRunning = !isRunning();
		if (notRunning)
			log.warn("Connection '{}' is already stopped.", name);
		return notRunning;
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
			log.error("Error while disposing client of connection '{}'.", name, e);
			
			if (cause != null)
				cause.addSuppressed(e);
		}
		
		disposeListeners();
		
		//No need to keep listeners instances of disposed connection
		for (ListenerConfiguration listener : listeners)
		{
			listener.setImplementation(null);
		}
	}


	
	@Override
	protected ReceiveListener createMessageCollector(String collectorName, Map<String, String> settings)
			throws SettingsException, ConnectivityException
	{
		String messageEndIndicator = Utils.EOL + Utils.EOL;

		String type = settings.get(ClearThMessageCollector.TYPE_SETTING);
		if (isBlank(type))
			return new ClearThMessageCollector(collectorName, name, settings, messageEndIndicator);
		else
		{
			ICodec codec = ClearThCore.getInstance().createCodec(type);
			return new ClearThMessageCollector(collectorName, name, codec, settings, messageEndIndicator);
		}
	}

	@Override
	public Class<?> getMessageCollectorClass()
	{
		return ClearThMessageCollector.class;
	}
}
