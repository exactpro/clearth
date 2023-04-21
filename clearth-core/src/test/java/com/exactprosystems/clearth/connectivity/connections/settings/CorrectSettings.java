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

import java.util.List;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;

@ConnectionSettings(columns = {"host", "login", "mode"},
		order = {"login", "password"})
public class CorrectSettings implements ClearThConnectionSettings
{
	//Fields cover various data types and field properties, i.e. custom name, inputType, etc.
	
	@ConnectionSetting
	private String host;
	
	@ConnectionSetting
	private int port;
	
	@ConnectionSetting(name = "Username")
	private String login;
	
	@ConnectionSetting(inputType = InputType.PASSWORD)
	private String password;
	
	private String dataCharset;
	
	@ConnectionSetting(name = "Connection mode")
	private ConnectionMode mode;
	
	@ConnectionSetting
	private long timeout;
	
	@ConnectionSetting
	private boolean autoReconnect;
	
	@ConnectionSetting
	private List<String> multiline;

	@Override
	public void copyFrom(ClearThConnectionSettings settingsFrom)
	{
		CorrectSettings correctSettingsFrom = (CorrectSettings)settingsFrom;
		this.host = correctSettingsFrom.host;
		this.port = correctSettingsFrom.port;
		this.login = correctSettingsFrom.login;
		this.password = correctSettingsFrom.password;
		this.dataCharset = correctSettingsFrom.dataCharset;
		this.mode = correctSettingsFrom.mode;
		this.timeout = correctSettingsFrom.timeout;
		this.autoReconnect = correctSettingsFrom.autoReconnect;
		this.multiline = correctSettingsFrom.multiline;
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
	
	
	public String getPassword()
	{
		return password;
	}
	
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	
	public String getDataCharset()
	{
		return dataCharset;
	}
	
	public void setDataCharset(String dataCharset)
	{
		this.dataCharset = dataCharset;
	}
	
	
	public ConnectionMode getMode()
	{
		return mode;
	}
	
	public void setMode(ConnectionMode mode)
	{
		this.mode = mode;
	}
	
	
	public long getTimeout()
	{
		return timeout;
	}
	
	public void setTimeout(long timeout)
	{
		this.timeout = timeout;
	}
	
	
	public boolean isAutoReconnect()
	{
		return autoReconnect;
	}
	
	public void setAutoReconnect(boolean autoReconnect)
	{
		this.autoReconnect = autoReconnect;
	}
	
	
	public List<String> getMultiline()
	{
		return multiline;
	}
	
	public void setMultiline(List<String> multiline)
	{
		this.multiline = multiline;
	}
}