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

package com.exactprosystems.clearth.connectivity.fix;

import com.exactprosystems.clearth.connectivity.ClearThClient;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.BasicClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.SettingsClass;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.SettingsException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;

@XmlRootElement(name="FixConnection")
@XmlAccessorType(XmlAccessType.NONE)
@SettingsClass(FixConnectionSettings.class)
public class FixConnection extends BasicClearThMessageConnection
{
	public static final String SETTING_CONNECTION_TYPE = "ConnectionType",
			TYPE_ACCEPTOR = "acceptor";
	
	@Override
	public boolean isAutoConnect()
	{
		return false;
	}
	
	@Override
	protected ClearThClient createClient() throws SettingsException, ConnectivityException
	{
		try
		{
			String settingsStr = ((FixConnectionSettings) settings).getFixSettings(),
					type = getFixApplicationType(settingsStr);
			if (TYPE_ACCEPTOR.equalsIgnoreCase(type))
				return createAcceptor();
			return createInitiator();
		}
		catch (SettingsException | ConnectivityException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new ConnectivityException(e);
		}
	}
	
	protected String getFixApplicationType(String settings)
	{
		for (String s : settings.split("\\R"))
		{
			if (s.startsWith("#") || StringUtils.isBlank(s))
				continue;
			
			Pair<String, String> pair = KeyValueUtils.parseKeyValueString(s, false);
			if (SETTING_CONNECTION_TYPE.equals(pair.getFirst()))
				return pair.getSecond();
		}
		return null;
	}
	
	protected ClearThClient createAcceptor() throws ConnectivityException, SettingsException
	{
		return new FixAcceptor(this);
	}
	
	protected ClearThClient createInitiator() throws ConnectivityException, SettingsException
	{
		return new FixInitiator(this);
	}
}
