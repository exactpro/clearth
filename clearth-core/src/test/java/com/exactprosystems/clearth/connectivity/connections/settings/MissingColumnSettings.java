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

package com.exactprosystems.clearth.connectivity.connections.settings;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;

//This class uses reference to missing column in @ConnectionSettings.columns annotation
@ConnectionSettings(columns = {"host", "login", "forceReconnect"})
public class MissingColumnSettings implements ClearThConnectionSettings
{
	@ConnectionSetting
	private String host;
	
	@ConnectionSetting
	private int port;
	
	@ConnectionSetting
	private String login;

	@Override
	public void copyFrom(ClearThConnectionSettings settingsFrom)
	{
		MissingColumnSettings missingColumnSettingsFrom = (MissingColumnSettings)settingsFrom;

		this.host = missingColumnSettingsFrom.host;
		this.port = missingColumnSettingsFrom.port;
		this.login = missingColumnSettingsFrom.login;
	}


	public String getHost()
	{
		return host;
	}
	
	public void setHost(String host)
	{
		this.host = host;
	}
	
	
	public int getPort()
	{
		return port;
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	
	public String getLogin()
	{
		return login;
	}
	
	public void setLogin(String login)
	{
		this.login = login;
	}

}