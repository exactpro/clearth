/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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
package com.exactprosystems.clearth.woodpecker.utils;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.XmlUtils;
import com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerSettings;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

import static com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerPaths.WOODPECKER_DIR;

public class WoodpeckerSettingsSaver<T extends WoodpeckerSettings>
{
	private final Class<T> woodpeckerSettingsClass;

	private static final String SETTINGS_FILENAME = ClearThCore.rootRelative(WOODPECKER_DIR + "woodpeckerSettings.xml");

	public WoodpeckerSettingsSaver(Class<T> woodpeckerSettingsClass)
	{
		this.woodpeckerSettingsClass = woodpeckerSettingsClass;
	}

	public void saveWoodpeckerSettings(T woodpeckerSettings) throws WoodpeckerException
	{
		marshalWoodpeckerSettings(woodpeckerSettings);
	}

	public T readWoodpeckerSettings() throws WoodpeckerException, ReflectiveOperationException
	{
		T settings = unmarshalWoodpeckerSettings();
		return settings != null ? settings : createEmptySettings();
	}

	protected String getWoodpeckerSettingsPath()
	{
		return SETTINGS_FILENAME;
	}

	protected Class<T> getWoodpeckerSettingsClass()
	{
		return woodpeckerSettingsClass;
	}

	protected void marshalWoodpeckerSettings(T woodpeckerSettings) throws WoodpeckerException
	{
		try
		{
			XmlUtils.marshalObject(woodpeckerSettings, getWoodpeckerSettingsPath());
		}
		catch (JAXBException e)
		{
			throw new WoodpeckerException("Could not write Woodpecker settings to file", e);
		}
	}

	public T createEmptySettings() throws ReflectiveOperationException
	{
		return getWoodpeckerSettingsClass().getDeclaredConstructor().newInstance();
	}

	protected T unmarshalWoodpeckerSettings() throws WoodpeckerException
	{
		File file = new File(getWoodpeckerSettingsPath());

		if (!file.exists())
			return null;

		try
		{
			return XmlUtils.unmarshalObject(getWoodpeckerSettingsClass(), getWoodpeckerSettingsPath());
		}
		catch (JAXBException | IOException e)
		{
			throw new WoodpeckerException("Could not read Woodpecker settings from file", e);
		}
	}
}
