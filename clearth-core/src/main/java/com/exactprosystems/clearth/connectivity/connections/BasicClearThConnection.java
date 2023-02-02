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

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.exceptions.ClassSetupException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class BasicClearThConnection implements ClearThConnection
{
	@XmlElement
	protected String name = "";
	@XmlElement
	protected ClearThConnectionSettings settings;
	protected ConnectionTypeInfo typeInfo;

	public BasicClearThConnection()
	{
		settings = createSettings();
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public ConnectionTypeInfo getTypeInfo()
	{
		return typeInfo;
	}

	@Override
	public void setTypeInfo(ConnectionTypeInfo typeInfo)
	{
		this.typeInfo = typeInfo;
	}

	@Override
	public ClearThConnectionSettings getSettings()
	{
		return settings;
	}

	@Override
	public void copyFrom(ClearThConnection other)
	{
		settings.copyFrom(other.getSettings());
		this.setName(other.getName());
		this.setTypeInfo(other.getTypeInfo());
	}

	@Override
	public ClearThConnection copy() throws ConnectivityException
	{
		try
		{
			ClearThConnection newConnection = this.getClass().newInstance();
			newConnection.copyFrom(this);
			return newConnection;
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new ConnectivityException(e, "Cannot copy connection with name = %s", getName());
		}
	}

	protected final ClearThConnectionSettings createSettings()
	{
		SettingsClass settingsAnnotation = getClass().getAnnotation(SettingsClass.class);
		if (settingsAnnotation == null)
			throw new ClassSetupException("Couldn't create settings - " +
					"SettingsClass annotation is missing for class " + getClass());

		try
		{
			return settingsAnnotation.value().newInstance();
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new ClassSetupException("Couldn't create new settings", e);
		}
	}
}