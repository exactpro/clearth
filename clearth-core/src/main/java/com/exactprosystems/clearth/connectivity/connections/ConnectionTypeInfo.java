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

import com.exactprosystems.clearth.connectivity.connections.exceptions.ClassSetupException;
import com.exactprosystems.clearth.connectivity.validation.ClearThConnectionValidationRule;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConnectionTypeInfo
{
	private final String name;
	private final Class<? extends ClearThConnection> connectionClass;
	private final Class<? extends ClearThConnectionSettings> settingsClass;
	private final Path directory;

	private final Set<ClearThConnectionValidationRule> rules;


	public ConnectionTypeInfo(String name,
	                          Class<? extends ClearThConnection> connectionClass,
	                          Path directory)
	{
		this(name, connectionClass, directory, null);
	}

	public ConnectionTypeInfo(String name,
	                          Class<? extends ClearThConnection> connectionClass,
	                          Path directory,
	                          Set<ClearThConnectionValidationRule> rules)
	{
		this.name = name;
		this.connectionClass = connectionClass;
		this.settingsClass = fetchSettingsClass(this.connectionClass);
		this.directory = directory;
		this.rules =  rules != null ? Collections.unmodifiableSet(new HashSet<>(rules)) : Collections.emptySet();
	}

	private Class<? extends ClearThConnectionSettings> fetchSettingsClass(
			Class<? extends ClearThConnection> connectionClass)
	{
		SettingsClass settingsClassAnnotation = connectionClass.getAnnotation(SettingsClass.class);
		if (settingsClassAnnotation == null)
			throw new ClassSetupException("Missing 'SettingsClass' annotation for class " + getConnectionClass().getName());

		return settingsClassAnnotation.value();
	}

	public String getName()
	{
		return name;
	}

	public Class<? extends ClearThConnection> getConnectionClass()
	{
		return connectionClass;
	}

	public Class<? extends ClearThConnectionSettings> getSettingsClass()
	{
		return settingsClass;
	}

	public Path getDirectory()
	{
		return directory;
	}

	@Override
	public String toString()
	{
		return String.format("name = '%s', connectionClass = '%s', directory = '%s'", 
				name, connectionClass, directory);
	}

	public Set<ClearThConnectionValidationRule> getRules()
	{
		return rules;
	}
}