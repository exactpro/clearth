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

import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.BasicClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThCheckableConnection;
import com.exactprosystems.clearth.connectivity.connections.SettingsClass;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.*;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@SettingsClass(DbConnectionSettings.class)
public class DbConnection extends BasicClearThConnection implements ClearThCheckableConnection
{
	private static final String EXCEPTION_MESSAGE = "Could not start connection ";

	public DbConnection () {}

	@Override
	public DbConnectionSettings getSettings()
	{
		return (DbConnectionSettings) settings;
	}

	public Connection getConnection() throws ConnectivityException, SettingsException
	{
		if (StringUtils.isBlank(getSettings().getJdbcUrl()))
			throw new SettingsException(EXCEPTION_MESSAGE + "'" + name + "'" + ". URL is empty");

		Connection connection = getConnectionFromDriverManager();

		String query = getSettings().getInitializationQuery();

		if (!StringUtils.isBlank(query))
		{
			try (PreparedStatement preparedStatement = connection.prepareStatement(query))
			{
				preparedStatement.execute();
			}
			catch (Exception e)
			{
				throw new ConnectionException("Error occurred while executing initialization query", e);
			}
		}
		return connection;
	}

	@Override
	public void check() throws SettingsException, ConnectivityException
	{
		try (Connection conn = getConnection())
		{}
		catch (SQLException e)
		{
			throw new ConnectionException("Could not close connection '" + name + "'", e);
		}
	}

	protected Connection getConnectionFromDriverManager() throws ConnectionException
	{
		DbConnectionSettings conSettings = getSettings();
		String url = conSettings.getJdbcUrl(),
			username = conSettings.getUsername(),
			password = conSettings.getPassword();

		try
		{
			if (StringUtils.isBlank(username))
				return DriverManager.getConnection(url);
			else
				return DriverManager.getConnection(url, username, password);
		}
		catch (Exception | NoClassDefFoundError e)
		{
			throw new ConnectionException(EXCEPTION_MESSAGE + "'" + name + "'", e);
		}
	}
}