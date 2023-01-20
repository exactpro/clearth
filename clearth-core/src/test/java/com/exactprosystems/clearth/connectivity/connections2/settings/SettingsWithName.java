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

package com.exactprosystems.clearth.connectivity.connections2.settings;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;

//This class has field called 'Name'. This field name is reserved for connection name and settings should not contain it
public class SettingsWithName extends ClearThConnectionSettings<SettingsWithName>
{
	@ConnectionSetting
	private String host;
	
	@ConnectionSetting
	private int port;
	
	@ConnectionSetting
	private String Name;
	
	@ConnectionSetting(inputType = InputType.PASSWORD)
	private String password;
	
	@Override
	public SettingsWithName copy()
	{
		SettingsWithName result = new SettingsWithName();
		result.host = host;
		result.port = port;
		result.Name = Name;
		result.password = password;
		return result;
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
	
	
	public String getName()
	{
		return Name;
	}
	
	public void setName(String name)
	{
		Name = name;
	}
	
	
	public String getPassword()
	{
		return password;
	}
	
	public void setPassword(String password)
	{
		this.password = password;
	}
}