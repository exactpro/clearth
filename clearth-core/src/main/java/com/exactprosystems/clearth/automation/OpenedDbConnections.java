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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class OpenedDbConnections
{
	private static final Logger logger = LoggerFactory.getLogger(OpenedDbConnections.class);
	private final Map<String, Connection> dbConnections;

	public OpenedDbConnections()
	{
		this.dbConnections = new HashMap<>();
	}

	public Connection getConnection(String connectionName) throws ConnectivityException, SettingsException
	{
		Connection connection = dbConnections.get(connectionName);
		try
		{
			if (connection == null || connection.isClosed())
				return addConnection(connectionName);
		}
		catch (SQLException e)
		{
			throw new ConnectivityException(e);
		}
		return connection;
	}

	private Connection addConnection(String connectionName) throws ConnectivityException, SettingsException
	{
		DbConnection dbConnection = (DbConnection) ClearThCore.connectionStorage().getConnection(connectionName,"DB");
		if (dbConnection == null)
			throw new ConnectivityException("Connection '%s' does not exist", connectionName);
		Connection connection = dbConnection.getConnection();
		dbConnections.put(connectionName,connection);

		return connection;
	}

	public void closeConnections()
	{
		for (Map.Entry<String, Connection> entry : dbConnections.entrySet())
		{
			try
			{
				if (!entry.getValue().isClosed())
					Utils.closeResource(entry.getValue());
			}
			catch (SQLException e)
			{
				logger.error("Exception during closing connection '{}'", entry.getKey(), e);
			}
		}
	}

	public void clear()
	{
		closeConnections();
		dbConnections.clear();
	}
}
