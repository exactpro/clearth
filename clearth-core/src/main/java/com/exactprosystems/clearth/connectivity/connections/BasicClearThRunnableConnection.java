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

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class BasicClearThRunnableConnection extends BasicClearThConnection implements ClearThRunnableConnection
{
	private static final Logger logger = LoggerFactory.getLogger(BasicClearThMessageConnection.class);
	protected final RunnableConnectionState connectionState;

	protected final ReadWriteLock lock = new ReentrantReadWriteLock();

	protected final Lock writeLock = lock.writeLock();
	protected final List<ConnectionErrorInfo> errorInfoList = new CopyOnWriteArrayList<>();

	public BasicClearThRunnableConnection()
	{
		connectionState = createConnectionState();
	}

	protected RunnableConnectionState createConnectionState()
	{
		return new RunnableConnectionState(name);
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

			startResources();

			connectionState.start();

			logger.info("Connection '{}' is now running.", name);
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

			restartResources();

			connectionState.restart();
			logger.info("'{}' reconnected", name);
		}
		finally
		{
			writeLock.unlock();
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

			stopResources();

			connectionState.stop();

			logger.info("Connection '{}' is now stopped.", name);
		}
		finally
		{
			writeLock.unlock();
		}
	}

	protected abstract void startResources() throws ConnectivityException, SettingsException;
	protected abstract void stopResources() throws ConnectivityException;
	protected abstract void restartResources() throws ConnectivityException;

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
	public void setName(String name)
	{
		super.setName(name);
		connectionState.setConnectionName(this.name);
	}
	
	@Override
	public List<ConnectionErrorInfo> getErrorInfo()
	{
		return Collections.unmodifiableList(errorInfoList);
	}

	@Override
	public void addErrorInfo(String errorMessage, Throwable reason, Instant occurred)
	{
		errorInfoList.add(new ConnectionErrorInfo(getName(), errorMessage, reason, occurred));
	}

	@Override
	public void clearErrorInfo()
	{
		errorInfoList.clear();
	}
}