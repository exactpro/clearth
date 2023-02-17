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

package com.exactprosystems.clearth.connectivity.db;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSetting;
import com.exactprosystems.clearth.connectivity.connections.settings.ConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.settings.InputType;
import com.exactprosystems.clearth.utils.LineBuilder;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import static java.lang.String.format;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@ConnectionSettings(order = {"jdbcUrl", "username", "password", "initializationQuery"},
		columns = {"jdbcUrl", "username"})
public class DbConnectionSettings implements ClearThConnectionSettings
{
	@XmlElement
	@ConnectionSetting(name = "URL")
	private String jdbcUrl;

	@XmlElement
	@ConnectionSetting(name = "Username")
	private String username;

	@XmlElement
	@ConnectionSetting(inputType = InputType.PASSWORD)
	private String password;

	@XmlElement
	@ConnectionSetting(name = "Initialization query", inputType = InputType.TEXTAREA)
	private String initializationQuery;

	public DbConnectionSettings()
	{
		jdbcUrl = "";
		username = "";
		password = "";
		initializationQuery = "";
	}

	public DbConnectionSettings(DbConnectionSettings settings)
	{
		copyFrom(settings);
	}

	public String getJdbcUrl()
	{
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl)
	{
		this.jdbcUrl = jdbcUrl;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getInitializationQuery()
	{
		return initializationQuery;
	}

	public void setInitializationQuery(String initializationQuery)
	{
		this.initializationQuery = initializationQuery;
	}
	
	@Override
	public String toString()
	{
		LineBuilder lb = new LineBuilder();
		lb.append("JDBC url = " + jdbcUrl);
		lb.append("username = " + username);
		lb.append("password = " + (StringUtils.isEmpty(password) ? "null" : "*****"));
		lb.append("initialization query = " + initializationQuery);
		return lb.toString();
	}

	@Override
	public DbConnectionSettings copy()
	{
		return new DbConnectionSettings(this);
	}

	@Override
	public void copyFrom(ClearThConnectionSettings settings1)
	{
		if (!this.getClass().isAssignableFrom(settings1.getClass()))
		{
			throw new IllegalArgumentException(format("Could not copy settings. " +
							"Expected settings of class '%s', got settings of class '%s'", 
					this.getClass().getSimpleName(), settings1.getClass().getSimpleName()));
		}
		
		DbConnectionSettings settings = (DbConnectionSettings) settings1;
		this.jdbcUrl = settings.jdbcUrl;
		this.username = settings.username;
		this.password = settings.password;
		this.initializationQuery = settings.initializationQuery;
	}
}