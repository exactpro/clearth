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

import com.exactprosystems.clearth.connectivity.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class RunnableConnectionState
{
	private static final Logger logger = LoggerFactory.getLogger(BasicClearThMessageConnection.class);

	private String connectionName;
	protected volatile LocalDateTime startTime, stopTime;
	protected volatile boolean running = false;

	public RunnableConnectionState(String connectionName)
	{
		this.connectionName = connectionName;
	}

	public String getConnectionName()
	{
		return connectionName;
	}

	public void setConnectionName(String connectionName)
	{
		this.connectionName = connectionName;
	}
	
	public void reset()
	{
		startTime = null;
		stopTime = null;
	}

	public void start()
	{
		startTime = LocalDateTime.now();
		running = true;
	}

	public void stop()
	{
		stopTime = LocalDateTime.now();
		running = false;
	}

	public void restart()
	{
		startTime = LocalDateTime.now();
		stopTime = null;
	}

	public boolean isRunning()
	{
		return running;
	}

	public LocalDateTime getStartTime()
	{
		return startTime;
	}

	public LocalDateTime getStopTime()
	{
		return stopTime;
	}

	public boolean cantBeStarted()
	{
		boolean running = isRunning();
		if (running)
			logger.warn("Connection '{}' is already running.", connectionName);
		return running;
	}

	public boolean cantBeStopped()
	{
		boolean notRunning = !isRunning();
		if (notRunning)
			logger.warn("Connection '{}' is already stopped", connectionName);
		return notRunning;
	}

	public void checkCanBeReconnected() throws ConnectionException
	{
		boolean notRunning = !isRunning();

		if (notRunning)
		{
			logger.warn("Connection '{}' is stopped, reconnect won't be performed", connectionName);
			throw new ConnectionException("Connection '" + connectionName + "' is stopped, reconnect won't be performed");
		}
	}
}



