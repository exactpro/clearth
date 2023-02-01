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

import com.exactprosystems.clearth.connectivity.connections2.validation.ClearThConnectionValidationRule;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConnectionTypeInfo
{
	private final String type;
	private final Class<? extends ClearThConnection> connectionClass;
	private final Class<? extends ClearThConnectionSettings> settingsClass;
	private final Path directory;

	private final Set<ClearThConnectionValidationRule> rules;


	public ConnectionTypeInfo(String type, Class<? extends ClearThConnection> connectionClass,
	                          Class<? extends ClearThConnectionSettings> settingsClass, Path directory)
	{
		this(type, connectionClass, settingsClass, directory, null);
	}

	public ConnectionTypeInfo(String type, Class<? extends ClearThConnection> connectionClass,
	                          Class<? extends ClearThConnectionSettings> settingsClass, Path directory,
	                          Set<ClearThConnectionValidationRule> rules)
	{
		this.type = type;
		this.connectionClass = connectionClass;
		this.settingsClass = settingsClass;
		this.directory = directory;
		this.rules =  rules != null ? Collections.unmodifiableSet(new HashSet<>(rules)) : Collections.emptySet();
	}

	public String getType()
	{
		return type;
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
		return String.format("type = '%s', connectionClass = '%s', settingsClass = '%s', directory = '%s'", type,
				connectionClass, settingsClass, directory);
	}

	public Set<ClearThConnectionValidationRule> getRules()
	{
		return rules;
	}
}