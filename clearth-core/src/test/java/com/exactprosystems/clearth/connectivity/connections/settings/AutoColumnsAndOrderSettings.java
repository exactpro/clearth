/*******************************************************************************
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

//This class doesn't define any columns to show with @ConnectionSettings.columns annotation and no fields order with @ConnectionSettings.order annotation.
//All columns, except for password, should be shown as columns
//Fields should go in the order the are defined in class
public class AutoColumnsAndOrderSettings implements ClearThConnectionSettings
{
	@ConnectionSetting
	private String host;
	
	@ConnectionSetting
	private int port;
	
	@ConnectionSetting
	private String login;
	
	@ConnectionSetting(inputType = InputType.PASSWORD)
	private String password;
	
	@Override
	public AutoColumnsAndOrderSettings copy()
	{
		AutoColumnsAndOrderSettings result = new AutoColumnsAndOrderSettings();
		result.host = host;
		result.port = port;
		result.login = login;
		result.password = password;
		return result;
	}

	@Override
	public void copyFrom(ClearThConnectionSettings settings)
	{
		AutoColumnsAndOrderSettings autoCs = (AutoColumnsAndOrderSettings)settings;
		host = autoCs.host;
		port = autoCs.port;
		login = autoCs.login;
		password = autoCs.password;
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
}