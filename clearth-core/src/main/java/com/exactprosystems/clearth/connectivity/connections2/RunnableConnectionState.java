/******************************************************************************
 * Copyright (c) 2009-2023, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary 
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/

package com.exactprosystems.clearth.connectivity.connections2;

import com.exactprosystems.clearth.connectivity.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class RunnableConnectionState
{
	private static final Logger logger = LoggerFactory.getLogger(BasicClearThMessageConnection.class);

	protected String connectionName;
	protected LocalDateTime startTime, stopTime;
	protected volatile boolean running = false;

	public RunnableConnectionState(String connectionName)
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



