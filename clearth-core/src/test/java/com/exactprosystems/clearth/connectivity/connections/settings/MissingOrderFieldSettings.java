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

//This class uses reference to missing field in @ConnectionSettings.order annotation
@ConnectionSettings(order = {"host", "port", "altPort, login"})
public class MissingOrderFieldSettings implements ClearThConnectionSettings
{
	@ConnectionSetting
	private String login;
	
	@ConnectionSetting
	private String password;
	
	@ConnectionSetting
	private String host;
	
	@ConnectionSetting
	private int port;
	

	@Override
	public void copyFrom(ClearThConnectionSettings settingsFrom)
	{
		MissingOrderFieldSettings missingSettingsFrom = (MissingOrderFieldSettings)settingsFrom;;
		this.login = missingSettingsFrom.login;
		this.password = missingSettingsFrom.password;
		this.host = missingSettingsFrom.host;
		this.port = missingSettingsFrom.port;
	}


	public String getLogin()
	{
		return login;
	}
	
	public void setLogin(String login)
	{
		this.login = login;
	}
	
	
	public String getPassword()
	{
		return password;
	}
	
	public void setPassword(String password)
	{
		this.password = password;
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
}