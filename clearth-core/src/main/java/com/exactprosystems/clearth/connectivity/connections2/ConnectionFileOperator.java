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

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.notExists;
import static org.apache.commons.io.FilenameUtils.getExtension;

public class ConnectionFileOperator
{
	private static final Logger logger = LoggerFactory.getLogger(ConnectionFileOperator.class);
	protected Map<String, Marshaller> marshallerMap = new HashMap<>();
	protected Map<String, Unmarshaller> unmarshallerMap = new HashMap<>();


	public ClearThConnection load(File file, ConnectionTypeInfo info) throws ConnectivityException
	{
		FileInputStream is = null;
		Unmarshaller unmarshaller = getUnmarshaller(info);
		try
		{
			is = new FileInputStream(file);
			return (ClearThConnection) unmarshaller.unmarshal(is);
		}
		catch (FileNotFoundException e)
		{
			throw new ConnectivityException(e, "Could not load connection settings from file '%s'. File not found.",
					file.getAbsolutePath());
		}
		catch (JAXBException e)
		{
			throw new ConnectivityException(e, "Could not load connection settings from file '%s'.",
					file.getAbsolutePath());
		}
		finally
		{
			Utils.closeResource(is);
		}
	}

	public List<ClearThConnection> loadConnections(ConnectionTypeInfo info)
	{
		List<ClearThConnection> connections = new ArrayList<>();
		Path directoryPath = info.getDirectory();
		try (Stream<Path> dirStream = Files.list(directoryPath))
		{
			dirStream.filter(Files::isRegularFile)
					.filter(filePath -> "xml".equalsIgnoreCase(getExtension(filePath.toString())))
					.forEach(filePath ->
					{
						try
						{
							ClearThConnection connection = load(filePath.toFile(), info);
							connection.setTypeInfo(info);
							connections.add(connection);
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
		return connections;
	}

	public void save(ClearThConnection connection) throws ConnectivityException
	{
		ConnectionTypeInfo info = connection.getTypeInfo();
		File file = getConnectionFile(getConnectionFileName(connection.getName()), info);
		createParentDirForSettings(file);
		try
		{
			Marshaller m = getMarshaller(info);
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(connection, file);
		}
		catch (JAXBException e)
		{
			throw new ConnectivityException(e, "Could not save settings of connection '%s' to file '%s'.",
					connection.getName(), file.getAbsolutePath());
		}
	}

	public void delete(String connectionName, ConnectionTypeInfo info)
	{
		Path path = getConnectionFile(getConnectionFileName(connectionName), info).toPath();
		try
		{
			Files.delete(path);
		}
		catch (IOException e)
		{
			logger.warn("Cannot delete file '{}' containing settings of removed connection '{}'.",
					path.toAbsolutePath(), connectionName, e);
		}
	}

	protected Marshaller getMarshaller(ConnectionTypeInfo info) throws ConnectivityException
	{
		String type = info.getType();
		Marshaller marshaller = marshallerMap.get(type);
		if (marshaller != null)
			return marshaller;
		JAXBContext jc = null;
		try
		{
			jc = JAXBContext.newInstance(info.getConnectionClass(), info.getSettingsClass());
			marshaller = jc.createMarshaller();
			marshallerMap.put(type, marshaller);
			return marshaller;
		}
		catch (JAXBException e)
		{
			throw new ConnectivityException(e, "Could not create marshaller for connections of type '%s'", type);
		}
	}


	protected Unmarshaller getUnmarshaller(ConnectionTypeInfo info) throws ConnectivityException
	{
		String type = info.getType();
		Unmarshaller unmarshaller = unmarshallerMap.get(type);
		if (unmarshaller != null)
			return unmarshaller;
		try
		{
			unmarshaller = JAXBContext.newInstance(info.getConnectionClass(),
					info.getSettingsClass()).createUnmarshaller();
			unmarshallerMap.put(type, unmarshaller);
			return unmarshaller;
		}
		catch (JAXBException e)
		{
			throw new ConnectivityException(e, "Could not create unmarshaller for connections of type '%s'", type);
		}
	}

	protected String getConnectionFileName(String connectionName)
	{
		return connectionName + ".xml";
	}


	protected void createParentDirForSettings(File settingsFile) throws ConnectivityException
	{
		Path parentDir = settingsFile.getParentFile().toPath();
		if (notExists(parentDir))
		{
			try
			{
				createDirectories(parentDir);
			}
			catch (IOException e)
			{
				throw new ConnectivityException(e,
						"Unable to create directory '%s' to save connection settings file '%s'.",
						parentDir.toAbsolutePath(), settingsFile.getName());
			}
		}
	}

	protected File getConnectionFile(String connectionName, ConnectionTypeInfo info)
	{
		return info.getDirectory().resolve(connectionName).toFile();
	}

}