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

package com.exactprosystems.clearth.woodpecker.configuration;

import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonDesc;
import com.exactprosystems.clearth.woodpecker.configuration.daemonsDictionary.DaemonsDictionary;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemonFactory;
import com.exactprosystems.clearth.woodpecker.daemons.DaemonsPool;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;
import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerPaths.DAEMONS_DICTIONARY_FILE;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerPaths.WOODPECKER_CONFIGS_BASE_DIR;
import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerPaths.WOODPECKER_DAEMONS_SETTINGS_DIR;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException.fromIOException;
import static java.lang.String.format;
import static java.nio.file.Files.*;

public class DaemonsLoader
{
	private static final Logger logger = LoggerFactory.getLogger(DaemonsLoader.class);
	
	private final Map<String, WoodpeckerDaemonFactory> daemonFactories;
	
	private final Map<Class, Unmarshaller> unmarshallers = new HashMap<>();

	
	public DaemonsLoader(Map<String, WoodpeckerDaemonFactory> daemonFactories)
	{
		this.daemonFactories = daemonFactories;
	}


	public List<DaemonsPool> loadDaemons() throws WoodpeckerDaemonException
	{
		createSettingsDirIfNotExists();
		
		DaemonsDictionary daemonsDictionary = DaemonsDictionary.load(rootRelative(DAEMONS_DICTIONARY_FILE));
		
		List<DaemonsPool> allPools = new ArrayList<>();
		for (DaemonDesc daemonDesc : daemonsDictionary.getDaemons())
		{
			WoodpeckerDaemonFactory factory = daemonFactories.get(daemonDesc.getFactoryName());
			if (factory == null)
			{
				logger.warn("Factory for description class {} isn't found.", daemonDesc.getClass().getName());
				continue;
			}
			
			DaemonsPool daemonsPool = loadDaemons(daemonDesc, factory);
			if (daemonsPool != null)
				allPools.add(daemonsPool);
		}
		return allPools;
	}
	
	private void createSettingsDirIfNotExists() throws WoodpeckerDaemonException
	{
		Path settingsDir = Paths.get(rootRelative(WOODPECKER_DAEMONS_SETTINGS_DIR));
		try
		{
			createDirectories(settingsDir);
		}
		catch (IOException e)
		{
			throw fromIOException(e, "Unable to create directory '%s' to store settings.", settingsDir);
		}
	}
	
	private DaemonsPool loadDaemons(DaemonDesc description, WoodpeckerDaemonFactory factory) throws WoodpeckerDaemonException
	{
		DaemonsPool daemonsPool = null;
		Path daemonDir = Paths.get(rootRelative(WOODPECKER_CONFIGS_BASE_DIR), description.getBaseDirectory());
		try (DirectoryStream<Path> ds = newDirectoryStream(daemonDir))
		{
			for (Path instanceDir : ds)
			{
				WoodpeckerDaemon daemon = loadDaemon(factory, description, instanceDir);
				if (daemon == null)
					continue;
				
				if (daemonsPool == null)
					daemonsPool = new DaemonsPool(description.getType());
				
				daemonsPool.addDaemon(daemon.getSettings().getDaemonName(), daemon);
			}
		}
		catch (IOException e)
		{
			throw fromIOException(e, "Error while trying to list '%s'.", daemonDir);
		}
		
		if (daemonsPool == null)
			logger.warn("No daemon instances found in {}.", daemonDir);
		
		return daemonsPool;
	}
	
	private WoodpeckerDaemon loadDaemon(WoodpeckerDaemonFactory factory, DaemonDesc description, Path instanceDir)
	{
		if (!isDirectory(instanceDir) || !containsDaemonInstance(instanceDir))
			return null;

		String instanceName = instanceDir.getFileName().toString();
		try
		{
			DaemonSettings settings = loadDaemonSettings(factory, description, instanceName);
			return factory.createDaemon(instanceDir, settings, description);
		}
		catch (WoodpeckerDaemonException e)
		{
			logger.error(format("Unable to load '%s' daemon '%s'.", description.getType(), instanceName), e);
			return null;
		}
	}
	
	
	private boolean containsDaemonInstance(Path directory)
	{
		Path startFile = directory.resolve(WoodpeckerConfigFile.START_FILE.fileName());
		return exists(startFile);
	}
	
	
	private DaemonSettings loadDaemonSettings(WoodpeckerDaemonFactory factory, DaemonDesc description, 
	                                          String instanceName) throws WoodpeckerDaemonException
	{
		Path filePath = getDaemonSettingsPath(description, instanceName);
		if (exists(filePath))
			return loadDaemonSettings(factory, filePath.toFile());
		else
			return initNewSettings(description, instanceName, factory);
	}
	
	private DaemonSettings loadDaemonSettings(WoodpeckerDaemonFactory factory, File file) throws WoodpeckerDaemonException
	{
		Class<? extends DaemonSettings> settingsClass = factory.getSettingsClass();
		try
		{
			Unmarshaller unmarshaller = unmarshallers.get(settingsClass);
			if (unmarshaller == null)
			{
				unmarshaller = JAXBContext.newInstance(settingsClass).createUnmarshaller();
				unmarshallers.put(settingsClass, unmarshaller);
			}

			try (FileInputStream is = new FileInputStream(file))
			{
				return unmarshaller.unmarshal(new StreamSource(is), settingsClass).getValue();
			}
		}
		catch (JAXBException e)
		{
			throw new WoodpeckerDaemonException(format("Error while loading '%s': %s", file,
					(e.getLinkedException() != null) ?
							e.getLinkedException().getMessage() :
							e.getMessage()), e);
		}
		catch (IOException e)
		{
			throw fromIOException(e, "Error while loading '%s'", file);
		}
	}
	
	private DaemonSettings initNewSettings(DaemonDesc description, String instanceName, WoodpeckerDaemonFactory factory)
	{
		DaemonSettings settings = factory.createSettings();
		settings.setDaemonType(description.getType());
		settings.setDaemonName(instanceName);
		return settings;
	}
	
	private Path getDaemonSettingsPath(DaemonDesc description, String instanceName)
	{
		return Paths.get(rootRelative(WOODPECKER_DAEMONS_SETTINGS_DIR), 
				description.getType(),
				instanceName + ".xml");
	}
}
