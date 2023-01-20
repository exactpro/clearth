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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulerData;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.FavoriteConnectionManager;
import com.exactprosystems.clearth.connectivity.connections.ConnectionErrorInfo;
import com.exactprosystems.clearth.connectivity.connections2.validation.ConnectionStartValidator;
import com.exactprosystems.clearth.connectivity.connections2.validation.DefaultConnectionStartValidator;
import com.exactprosystems.clearth.utils.NameValidator;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isBlank;

public class DefaultClearThConnectionStorage implements ClearThConnectionStorage
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultClearThConnectionStorage.class);

	protected final List<ClearThConnection> connections = new CopyOnWriteArrayList<>();
	protected final Map<String, ClearThConnection> connectionsByName = new ConcurrentHashMap<>();
	protected final Map<String, List<ClearThConnection>> connectionsByType = new ConcurrentHashMap<>();
	protected final List<ConnectionErrorInfo> stoppedConnectionsErrors = new CopyOnWriteArrayList<>();

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	protected final Lock findRunningLock = rwLock.readLock();
	protected final Lock modifyListLock = rwLock.writeLock();

	protected final Comparator<ClearThConnection> connectionComparator;

	protected Map<String, ConnectionTypeInfo> typeInfoMap = new LinkedHashMap<>();

	protected ConnectionFileOperator connectionFileOperator;

	protected final ConnectionStartValidator connectionStartValidator;

	public DefaultClearThConnectionStorage() throws ConnectivityException
	{
		connectionComparator = createConnectionComparator();
		connectionFileOperator = createConnectionFileOperator();
		connectionStartValidator = createConnectionStartValidator();
	}

	@Override
	public void registerType(ConnectionTypeInfo info)
	{
		typeInfoMap.put(info.getType(), info);
	}

	protected ConnectionFileOperator createConnectionFileOperator()
	{
		return new ConnectionFileOperator();
	}

	protected ConnectionStartValidator createConnectionStartValidator()
	{
		return new DefaultConnectionStartValidator();
	}

	@Override
	public Collection<String> getTypes()
	{
		return Collections.unmodifiableSet(typeInfoMap.keySet());
	}

	/**
	 * Returns reference to connections list. Content of the list can be modified at any time.
	 * CopyOnWriteArrayList is used. So iteration by foreach is safe. 
	 * In other cases it is better to copy data. F.e. for indexed loop.
	 * 
	 * @return reference to connections list
	 */
	@Override
	public List<ClearThConnection> getConnections()
	{
		return connections;
	}
	
	/**
	 * Returns reference to connections list with specified type. Content of the list can be modified at any time.
	 * CopyOnWriteArrayList is used. So iteration by foreach is safe. 
	 * In other cases it is better to copy data. F.e. for indexed loop.
	 *
	 * @param type connection type
	 * 
	 * @return reference to connections list by specified type
	 */
	@Override
	public List<ClearThConnection> getConnections(String type)
	{
		List<ClearThConnection> connections = connectionsByType.get(type);
		return (connections != null) ? connections : emptyList();
	}

	@Override
	public List<ClearThConnection> getConnections(Predicate<ClearThConnection> predicate)
	{
		return connections.stream()
				.filter(predicate)
				.collect(toList());
	}

	@Override
	public<T extends ClearThConnection> List<T> getConnections(Predicate<ClearThConnection> predicate, Class<T> clazz)
	{
		return connections.stream()
		                  .filter(predicate)
		                  .map (clazz::cast)
		                  .collect(Collectors.toList());
	}

	@Override
	public<T extends ClearThConnection> List<T> getConnections(String type, Predicate<ClearThConnection> predicate,
	                                                           Class<T> clazz)
	{
		return getConnections(type).stream()
				.filter(predicate)
				.map (clazz::cast)
				.collect(Collectors.toList());
	}

	@Override
	public List<String> listConnections(Predicate<ClearThConnection> predicate)
	{
		return connections.stream()
				.filter(predicate)
				.map(ClearThConnection::getName)
				.collect(toList());
	}

	@Override
	public boolean containsConnection(String name)
	{
		return connectionsByName.containsKey(name);
	}

	@Override
	public ClearThConnection getConnection(String name)
	{
		return connectionsByName.get(name);
	}

	@Override
	public ClearThConnection getConnection(String name, String type)
	{
		ClearThConnection connection = connectionsByName.get(name);
		return ((connection != null) && StringUtils.equals(type, connection.getTypeInfo().getType()))
				? connection : null;
	}


	/*
	 * Read lock in findRunningConnection is needed to prevent the following case:
	 *
	 * Thread A:     gets reference to connection                            starts connection
	 * Thread B:                                     removes connection
	 *
	 * Result: Running connection isn't available in other threads and can be stopped only by Thread A
	 *
	 * So we need to find and stop atomically.
	 * */
	@Override
	public ClearThConnection findRunningConnection(String conName) throws ConnectivityException
	{
		findRunningLock.lock();
		try
		{
			ClearThConnection connection = getConnection(conName);
			if (connection == null)
				throw new ConnectivityException("Connection with name '" + conName + "' doesn't exist");
			startIfNeeded(connection);
			return connection;
		}
		finally
		{
			findRunningLock.unlock();
		}
	}
	@Override
	public ClearThConnection findRunningConnection(String conName, String type) throws ConnectivityException
	{
		findRunningLock.lock();
		try
		{
			ClearThConnection
					connection = getConnection(conName, type);
			if (connection == null)
				throw new ConnectivityException(type + " connection with name '" + conName + "' doesn't exist");
			startIfNeeded(connection);
			return connection;
		}
		finally
		{
			findRunningLock.unlock();
		}
	}
	protected void startIfNeeded(ClearThConnection connection) throws ConnectivityException
	{
		if (!(connection instanceof ClearThRunnableConnection))
		{
			return;
		}
		ClearThRunnableConnection runnableConnection = (ClearThRunnableConnection) connection;
		if (runnableConnection.isRunning())
			return;
		try
		{
			runnableConnection.start();
		}
		catch (Exception e)
		{
			throw new ConnectivityException(e, "Could not start connection '%s': %s",
					connection.getName(), e.getMessage());
		}
	}

	@Override
	public void autoStartConnections()
	{
		for (ClearThConnection connection : connections)
		{
			if (!(connection instanceof ClearThRunnableConnection))
			{
				continue;
			}
			ClearThRunnableConnection runnableConnection = (ClearThRunnableConnection) connection;

			if (!runnableConnection.isAutoConnect())
				continue;

			if (!runnableConnection.isRunning())
			{
				try
				{
					runnableConnection.start();
					logger.info("Connection '{}' is now running", connection.getName());
				}
				catch (Exception e)
				{
					logger.error("Error occurred while starting connection '{}'", connection.getName(), e);
				}
			}
			else
				logger.info("Connection '{}' is already running", connection.getName());
		}
	}
	
	

	@Override
	public void stopAllConnections()
	{
		logger.info("Stopping all connections.");
		for (ClearThConnection con : connections)
		{
			if (!(con instanceof ClearThRunnableConnection))
			{
				continue;
			}
			ClearThRunnableConnection runnableConnection = (ClearThRunnableConnection) con;

			if (runnableConnection.isRunning())
			{
				try
				{
					runnableConnection.stop();
				}
				catch (Exception e)
				{
					logger.error("Could not stop connection '{}'", con.getName());
				}
			}
		}
	}

	@Override
	public ClearThConnection createConnection(String type) throws ConnectivityException
	{
		ConnectionTypeInfo info = getConnectionTypeInfo(type);
		if (info == null)
			throw new ConnectivityException("Cannot create connection with type = '{}'. This type is not registered " +
					"in storage.");
		try
		{
			ClearThConnection connection = info.getConnectionClass().newInstance();
			setConnectionInfoType(connection, info);
			return connection;
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new ConnectivityException(e, "Cannot create connection of class '%s' with type '%s'",
					info.getConnectionClass(), info.getType());
		}

	}

	public ConnectionTypeInfo getConnectionTypeInfo(String type) throws ConnectivityException
	{
		ConnectionTypeInfo info = typeInfoMap.get(type);
		if (info == null)
			throw new ConnectivityException("Type with name = '%s' is not registered.", type);
		return info;
	}

	protected void setConnectionInfoType(ClearThConnection connection, ConnectionTypeInfo info)
	{
		connection.setTypeInfo(info);
	}

	@Override
	public void addConnection(ClearThConnection connection) throws ConnectivityException,
			SettingsException
	{
		ConnectionTypeInfo info = connection.getTypeInfo();
		String connectionName = connection.getName();

		checkConnectionType(connectionName, info);
		if (!info.getConnectionClass().isAssignableFrom(connection.getClass()))
			throw new ConnectivityException("Connection '{}' has invalid class '{}' for ['{}'].", connectionName,
					connection.getClass(), info);

		modifyListLock.lock();

		try
		{
			validateName(connection.getName());
			connectionFileOperator.save(connection);
			addLink(connection, info, true);
		}
		finally
		{
			modifyListLock.unlock();
		}
	}


	@Override
	public void renameConnection(ClearThConnection connection, String newName) throws ConnectivityException, SettingsException
	{
		renameConnection(connection, newName, true);
	}
	
	private void renameConnection(ClearThConnection connection, String newName, boolean saveSettings)
			throws ConnectivityException, SettingsException
	{
		String oldName;
		ConnectionTypeInfo info = connection.getTypeInfo();
		oldName = connection.getName();
		checkConnectionType(oldName, info);

		modifyListLock.lock();
		try
		{
			validateName(newName);
			connection.setName(newName);

			ClearThCore.getInstance().getFavoriteConnections().changeName(oldName, newName);
			modifyConnectionToIgnoreFailures(oldName, newName);
			
			connectionsByName.remove(oldName);
			connectionsByName.put(newName, connection);
			removeStoppedConnectionErrors(oldName);
			sort();

			if (saveSettings)
				connectionFileOperator.save(connection);
		}
		finally
		{
			modifyListLock.unlock();
		}

		connectionFileOperator.delete(oldName, info);
	}
	
	
	@Override
	public void modifyConnection(ClearThConnection connectionToModify, ClearThConnection connectionWithNewSettings)
			throws ConnectivityException, SettingsException
	{
		ConnectionTypeInfo info = connectionToModify.getTypeInfo();
		String oldName = connectionToModify.getName();
		checkConnectionType(oldName, info);

		synchronized (connectionToModify)
		{
			if (!connections.contains(connectionToModify))
				throw new ConnectivityException("Cannot find connection to modify.");

			String newName = connectionWithNewSettings.getName();
			if (!oldName.equals(newName))
				renameConnection(connectionToModify, newName, false);

			//noinspection unchecked
			connectionToModify.copyFrom(connectionWithNewSettings);  //Copying properties from connectionWithNewSettings, but new instance won't be created

			connectionFileOperator.save(connectionToModify);
		}
	}
	
	
	@Override
	public void removeConnection(ClearThConnection connection) throws ConnectivityException
	{
		ConnectionTypeInfo info = connection.getTypeInfo();
		String connectionName = connection.getName();
		checkConnectionType(connectionName, info);

		modifyListLock.lock();
		try
		{
			if (!(connection instanceof ClearThRunnableConnection))
			{
				return;
			}
			ClearThRunnableConnection runnableConnection = (ClearThRunnableConnection) connection;

			if (runnableConnection.isRunning())
			{
				try
				{
					runnableConnection.stop();
				}
				catch (Exception e)
				{
					throw new ConnectivityException(e, "Cannot stop connection '%s'. Running connection cannot be removed.",
							connectionName);
				}
			}

			removeLink(connection);

			FavoriteConnectionManager fcManager = ClearThCore.getInstance().getFavoriteConnections();
			fcManager.changeName(connectionName, null);
		}
		finally
		{
			modifyListLock.unlock();
		}

		connectionFileOperator.delete(connectionName, info);

	}

	protected void checkConnectionType(String connectionName, ConnectionTypeInfo info) throws ConnectivityException
	{
		if (info == null)
			throw new ConnectivityException("Connection '{}' is invalid. Type info is missing.", connectionName);
		if (!typeInfoMap.containsKey(info.getType()))
			throw new ConnectivityException("Type '{}' of connection '{}' is not registered in the storage.",
					info.getType(), connectionName);

	}

	@Override
	public void validateConnectionStart(ClearThConnection connection) throws ConnectivityException, SettingsException
	{
		ConnectionTypeInfo info = connection.getTypeInfo();
		checkConnectionType(connection.getName(), info);
		connectionStartValidator.checkIfCanStartConnection(connection, info.getRules());
	}

	@Override
	public void loadConnections()
	{
		modifyListLock.lock();
		try
		{
			logger.info("Loading connections");

			for (ConnectionTypeInfo info : typeInfoMap.values())
			{
				List<ClearThConnection> connections = connectionFileOperator.loadConnections(info);
				connections.forEach(connection -> addLink(connection, info, false));
			}
			sort();
		}
		finally
		{
			modifyListLock.unlock();
		}
	}
	
	@Override
	public void reloadConnections()
	{
		modifyListLock.lock();
		try
		{
			stopAllConnections();

			connections.clear();
			connectionsByName.clear();
			connectionsByType.clear();
			stoppedConnectionsErrors.clear();

			loadConnections();
			
			autoStartConnections();
		}
		finally
		{
			modifyListLock.unlock();
		}
	}

	protected Comparator<ClearThConnection> createConnectionComparator()
	{
		return (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName());
	}
	
	
	protected void addLink(ClearThConnection connection, ConnectionTypeInfo info, boolean sort)
	{
		connections.add(connection);
		if (sort)
			connections.sort(connectionComparator);
		
		connectionsByName.put(connection.getName(), connection);
		
		String type = info.getType();
		List<ClearThConnection> byType = connectionsByType.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
		byType.add(connection);
		if (sort)
			byType.sort(connectionComparator);
	}
	
	protected void removeLink(ClearThConnection connection)
	{
		ConnectionTypeInfo info = connection.getTypeInfo();
		connections.remove(connection);
		connectionsByName.remove(connection.getName());
		removeStoppedConnectionErrors(connection.getName());

		String type = info.getType();
		List<ClearThConnection> byType = connectionsByType.get(type);
		if (byType != null)
		{
			byType.remove(connection);
			if (byType.isEmpty())
				connectionsByType.remove(type);
		}
		
		modifyConnectionToIgnoreFailures(connection.getName(), null);
	}

	protected void sort()
	{
		connections.sort(connectionComparator);
		for (List<ClearThConnection> byType : connectionsByType.values())
		{
			byType.sort(connectionComparator);
		}
	}

	protected void validateName(String connectionName) throws SettingsException
	{
		if (isBlank(connectionName))
			throw new SettingsException("Connection name cannot be empty.");
		ClearThConnection connection = getConnection(connectionName);
		if (connection != null)
		{
			throw new SettingsException(format("Connection with name '%s' already exists and has type '%s'.",
					connectionName, connection.getTypeInfo().getType()));
		}

		NameValidator.validate(connectionName);
	}

	public void addStoppedConnectionError(ConnectionErrorInfo errorInfo)
	{
		stoppedConnectionsErrors.add(errorInfo);
	}

	public void removeStoppedConnectionErrors(String connectionName)
	{
		List<ConnectionErrorInfo> toRemove = new ArrayList<ConnectionErrorInfo>();
		for (ConnectionErrorInfo cei : stoppedConnectionsErrors)
		{
			if (cei.getConnectionName().equals(connectionName))
				toRemove.add(cei);
		}
		stoppedConnectionsErrors.removeAll(toRemove);
	}

	public Collection<ConnectionErrorInfo> getStoppedConnectionsErrors()
	{
		return stoppedConnectionsErrors;
	}


	/**
	 * Modifies or removes (depending on second parameter) specified connection from all sets of ones for which failures should be ignored.
	 * @param connectionName original name of connection to work with.
	 * @param newConnectionName new name of selected connection. If not specified, connection will be removed from the set.
	 */
	protected void modifyConnectionToIgnoreFailures(String connectionName, String newConnectionName)
	{
		SchedulersManager schedulersManager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> allSchedulers = new ArrayList<>(schedulersManager.getCommonSchedulers());
		schedulersManager.getUsersSchedulers().values().forEach(allSchedulers::addAll);
		boolean removing = StringUtils.isBlank(newConnectionName);
		
		for (Scheduler scheduler : allSchedulers)
		{
			SchedulerData data = scheduler.getSchedulerData();
			Set<String> connectionsToIgnoreFailures = data.getConnectionsToIgnoreFailures();
			if (connectionsToIgnoreFailures.remove(connectionName))
			{
				if (!removing)
					connectionsToIgnoreFailures.add(newConnectionName);
				
				try
				{
					data.saveConnectionsToIgnoreFailures();
				}
				catch (IOException e)
				{
					boolean commonScheduler = SchedulersManager.COMMON_SCHEDULERS_KEY.equals(scheduler.getForUser());
					logger.error("Couldn't save changes of connections to ignore failures after {} '{}' for {}scheduler '{}'{}",
							removing ? "removing" : "modifying", connectionName, commonScheduler ? "common " : "",
							scheduler.getName(), !commonScheduler ? " of user '" + scheduler.getForUser() + "'" : "", e);
				}
			}
		}
	}

}
