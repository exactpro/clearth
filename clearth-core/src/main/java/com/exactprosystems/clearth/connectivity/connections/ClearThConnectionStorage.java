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

package com.exactprosystems.clearth.connectivity.connections;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulerData;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.FavoriteConnectionManager;
import com.exactprosystems.clearth.connectivity.connections2.settings.SettingsModel;
import com.exactprosystems.clearth.connectivity.connections2.settings.Processor;
import com.exactprosystems.clearth.connectivity.validation.ConnectionStartValidator;
import com.exactprosystems.clearth.utils.NameValidator;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang.StringUtils.isBlank;

public abstract class ClearThConnectionStorage
{
	private static final Logger logger = LoggerFactory.getLogger(ClearThConnectionStorage.class);

	public static final String MQ = "MQ";
	
	protected final List<ClearThConnection<?,?>> connections = new CopyOnWriteArrayList<>();
	protected final Map<String, ClearThConnection<?,?>> connectionsByName = new ConcurrentHashMap<>();
	protected final Map<String, List<ClearThConnection<?,?>>> connectionsByType = new ConcurrentHashMap<>();
	protected final List<ConnectionErrorInfo> stoppedConnectionsErrors = new CopyOnWriteArrayList<>();

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	protected final Lock findRunningLock = rwLock.readLock();
	protected final Lock modifyListLock = rwLock.writeLock();
	
	protected final Map<String, ClearThConnectionFactory<?>> factories = new LinkedHashMap<>();
	protected final Map<String, SettingsModel> settingsModels = new HashMap<>();

	protected final Comparator<ClearThConnection<?,?>> connectionComparator;
	protected final ConnectionStartValidator connectionStartValidator = new ConnectionStartValidator();
	
	protected abstract void initFactories(Processor settingsProcessor) throws ConnectivityException;

	protected abstract void initConnectionStartValidator(ConnectionStartValidator validator);
	
	public ClearThConnectionStorage() throws ConnectivityException
	{
		Processor settingsProcessor = createSettingsProcessor();
		initFactories(settingsProcessor);
		initConnectionStartValidator(connectionStartValidator);
		connectionComparator = createConnectionComparator();
	}
	
	public Collection<String> getTypes()
	{
		return Collections.unmodifiableCollection(factories.keySet());
	}
	
	public SettingsModel getSettingsModel(String type)
	{
		return settingsModels.get(type);
	}
	
	public String getConnectionsPath(String type)
	{
		ClearThConnectionFactory<?> f = getConnectionFactory(type);
		if (f == null)
			throw new IllegalArgumentException("Unknown connection type");
		return f.getDirName();
	}
	
	// List Connections //

	/**
	 * Returns reference to connections list. Content of the list can be modified at any time.
	 * CopyOnWriteArrayList is used. So iteration by foreach is safe. 
	 * In other cases it is better to copy data. F.e. for indexed loop.
	 * 
	 * @return reference to connections list
	 */
	public List<ClearThConnection<?, ?>> getConnections()
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
	public List<ClearThConnection<?, ?>> getConnections(String type)
	{
		List<ClearThConnection<?, ?>> connections = connectionsByType.get(type);
		return (connections != null) ? connections : emptyList();
	}

	public List<ClearThMessageConnection<?, ?>> getMessageConnections()
	{
		List<ClearThMessageConnection<?, ?>> result = new ArrayList<>();
		for (ClearThConnection<?, ?> con : connections)
		{
			if (ClearThMessageConnection.isMessageConnection(con))
				result.add((ClearThMessageConnection<?, ?>)con);
		}
		return result;
	}
	
	public List<ClearThConnection<?, ?>> getConnections(Predicate<ClearThConnection<?, ?>> predicate)
	{
		return connections.stream()
				.filter(predicate)
				.collect(toList());
	}

	public<T extends ClearThMessageConnection<?, ?>> List<T> getConnections(Predicate<ClearThConnection<?, ?>> predicate, Class<T> clazz)
	{
		return connections.stream()
		                  .filter(predicate)
		                  .map (clazz::cast)
		                  .collect(Collectors.toList());
	}

	public<T extends ClearThMessageConnection<?, ?>> List<T> getConnections(String type, Predicate<ClearThConnection<?, ?>> predicate, Class<T> clazz)
	{
		return getConnections(type).stream()
				.filter(predicate)
				.map (clazz::cast)
				.collect(Collectors.toList());
	}

	public List<String> listConnections(Predicate<ClearThConnection<?, ?>> predicate)
	{
		return connections.stream()
				.filter(predicate)
				.map(ClearThConnection::getName)
				.collect(toList());
	}
	
	
	// Find Connections //
	
	public boolean containsConnection(String name)
	{
		return connectionsByName.containsKey(name);
	}
	
	public boolean containsConnection(ClearThConnection<?, ?> connection)
	{
		return connections.contains(connection);
	}

	public ClearThConnection<?, ?> findConnection(String name)
	{
		return connectionsByName.get(name);
	}

	public ClearThConnection<?, ?> findConnection(String name, String type)
	{
		ClearThConnection<?, ?> connection = connectionsByName.get(name);
		if ((connection != null) && StringUtils.equals(type, connection.getType()))
			return connection;
		else 
			return null;
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

	public ClearThConnection<?, ?> findRunningConnection(String conName) throws ConnectivityException
	{
		findRunningLock.lock();
		try
		{
			ClearThConnection<?, ?> connection = findConnection(conName);
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

	public ClearThConnection<?, ?> findRunningConnection(String conName, String type) throws ConnectivityException
	{
		findRunningLock.lock();
		try
		{
			ClearThConnection<?, ?> connection = findConnection(conName, type);
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
	
	protected void startIfNeeded(ClearThConnection<?, ?> connection) throws ConnectivityException
	{
		if (!connection.isRunning())
		{
			try
			{
				connection.start();
			}
			catch (Exception e)
			{
				throw new ConnectivityException(e, "Could not start connection '%s': %s", 
						connection.getName(), e.getMessage());
			}
		}
	}
	
	
	// Manage Connections
	
	public void autoStartConnections()
	{
		for (ClearThConnection<?, ?> connection : connections)
		{
			if (!connection.isAutoConnect())
				continue;

			if (!connection.isRunning())
			{
				try
				{
					connection.start();
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
	
	
	public void stopAllConnections()
	{
		logger.info("Stopping all connections.");
		for (ClearThConnection<?, ?> con : connections)
		{
			if (con.isRunning())
			{
				try
				{
					con.stop();
				}
				catch (Exception e)
				{
					logger.error("Could not stop connection '{}'", con.getName());
				}
			}
		}
	}
	
	
	// Modify Connections
	
	public void addConnection(ClearThConnection<?, ?> connection) throws ConnectivityException, SettingsException
	{
		modifyListLock.lock();
		try
		{
			validateName(connection.getName());
			connection.save();
			addLink(connection, true);
		}
		finally
		{
			modifyListLock.unlock();
		}
	}


	public void renameConnection(ClearThConnection connection, String newName) throws ConnectivityException, SettingsException
	{
		renameConnection(connection, newName, true);
	}
	
	private void renameConnection(ClearThConnection connection, String newName, boolean saveSettings) throws ConnectivityException, SettingsException
	{
		String oldName;
		Path oldSettingsPath;
		
		modifyListLock.lock();
		try
		{
			oldName = connection.getName();
			oldSettingsPath = Paths.get(connection.connectionFileName());

			validateName(newName);
			connection.setName(newName);

			ClearThCore.getInstance().getFavoriteConnections().changeName(oldName, newName);
			modifyConnectionToIgnoreFailures(oldName, newName);
			
			connectionsByName.remove(oldName);
			connectionsByName.put(newName, connection);
			removeStoppedConnectionErrors(oldName);
			sort();

			if (saveSettings)
				connection.save();
		}
		finally
		{
			modifyListLock.unlock();
		}
		
		try
		{
			Files.delete(oldSettingsPath);
		}
		catch (IOException e)
		{
			logger.warn("Cannot delete file '{}' containing settings of renamed connection '{}' -> '{}'.",
					new Object[]{oldSettingsPath.toAbsolutePath(), oldName, newName, e});
		}
	}
	
	
	public void modifyConnection(ClearThConnection connectionToModify, ClearThConnection connectionWithNewSettings) throws ConnectivityException, SettingsException
	{
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (connectionToModify)
		{
			if (!connections.contains(connectionToModify))
				throw new ConnectivityException("Cannot find connection to modify.");

			String oldName = connectionToModify.getName();
			String newName = connectionWithNewSettings.getName();
			if (!oldName.equals(newName))
				renameConnection(connectionToModify, newName, false);

			//noinspection unchecked
			connectionToModify.copy(connectionWithNewSettings);  //Copying properties from connectionWithNewSettings, but new instance won't be created

			connectionToModify.save();
		}
	}
	
	
	public void removeConnection(ClearThConnection<?, ?> connection) throws ConnectivityException
	{
		modifyListLock.lock();
		try
		{
			String connectionName = connection.getName();
			
			if (connection.isRunning())
			{
				try
				{
					connection.stop();
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

		Path settingsPath = Paths.get(connection.connectionFileName());
		try
		{
			Files.delete(settingsPath);
		}
		catch (IOException e)
		{
			logger.warn("Cannot delete file '{}' containing settings of removed connection '{}'.",
					new Object[]{settingsPath.toAbsolutePath(), connection.getName(), e});
		}
	}
	
	
	// Load Connections from disk
	
	public void loadConnections()
	{
		modifyListLock.lock();
		try
		{
			logger.info("Loading connections");

			for (ClearThConnectionFactory<?> factory : factories.values())
			{
				Path directoryPath = Paths.get(factory.getDirName());
				try (Stream<Path> dirStream = Files.list(directoryPath))
				{
					dirStream.filter(Files::isRegularFile)
							.filter(filePath -> "xml".equalsIgnoreCase(getExtension(filePath.toString())))
							.forEach(filePath ->
							{
								try
								{
									ClearThConnection<?, ?> connection = factory.loadConnection(filePath.toFile());
									addLink(connection, false);
								}
								catch (ConnectivityException e)
								{
									logger.error("Error while loading connection from file '{}'", filePath.toAbsolutePath(), e);
								}
							});
				}
				catch (IOException e)
				{
					logger.error("Unable to list files in '{}'", directoryPath.toAbsolutePath(), e);
				}
			}

			sort();
		}
		finally
		{
			modifyListLock.unlock();
		}
	}
	
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
	
	
	public ClearThConnectionFactory<?> getConnectionFactory(String type)
	{
		return factories.get(type);
	}


	protected Comparator<ClearThConnection<?,?>> createConnectionComparator()
	{
		return (c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName());
	}
	
	
	protected void addLink(ClearThConnection<?, ?> connection, boolean sort)
	{
		connections.add(connection);
		if (sort)
			connections.sort(connectionComparator);
		
		connectionsByName.put(connection.getName(), connection);
		
		String type = connection.getType();
		List<ClearThConnection<?,?>> byType = connectionsByType.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>());
		byType.add(connection);
		if (sort)
			byType.sort(connectionComparator);
	}
	
	protected void removeLink(ClearThConnection<?, ?> connection)
	{
		connections.remove(connection);
		connectionsByName.remove(connection.getName());
		removeStoppedConnectionErrors(connection.getName());

		String type = connection.getType();
		List<ClearThConnection<?,?>> byType = connectionsByType.get(type);
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
		for (List<ClearThConnection<?,?>> byType : connectionsByType.values())
		{
			byType.sort(connectionComparator);
		}
	}
	
	protected void validateName(String connectionName) throws SettingsException
	{
		if (isBlank(connectionName))
			throw new SettingsException("Connection name cannot be empty.");
		ClearThConnection<?, ?> connection = findConnection(connectionName);
		if (connection != null)
		{
			throw new SettingsException(format("Connection with name '%s' already exists and has type '%s'.",
					connectionName, connection.getType()));
		}
		
		NameValidator.validate(connectionName);
	}

	public ConnectionStartValidator getConnectionStartValidator()
	{
		return connectionStartValidator;
	}
	
	
	public void addStoppedConnectionError(ConnectionErrorInfo errorInfo)
	{
		stoppedConnectionsErrors.add(errorInfo);
	}
	
	public void removeStoppedConnectionErrors(String connectionName)
	{
		//Optimization for less copies of list created while removing multiple elements. Note that we're removing from CopyOnWriteArrayList
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
	
	
	private Processor createSettingsProcessor() throws ConnectivityException
	{
		try
		{
			return new Processor();
		}
		catch (Exception e)
		{
			throw new ConnectivityException("Could not create connection settings processor", e);
		}
	}
}
