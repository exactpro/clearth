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

package com.exactprosystems.clearth.utils.tabledata.comparison.connections;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;

import java.sql.Connection;
import java.util.Map;

public class DefaultDbConnectionSupplier implements DbConnectionSupplier
{
	public static final String EXPECTED_CONNECTION_PARAM_NAME = "ExpectedConnectionName",
								ACTUAL_CONNECTION_PARAM_NAME = "ActualConnectionName";
	public static final String TYPE = "DB";
	protected ClearThConnectionStorage storage;
	protected Map<String, String> actionParameters;
	protected Connection expectedConnection, actualConnection;

	public DefaultDbConnectionSupplier(Map<String, String> actionParameters, GlobalContext globalContext)
	{
		this.actionParameters = actionParameters;
		storage = ClearThCore.connectionStorage();
	}

	protected Connection createConnection(boolean forExpectedData) throws ConnectivityException, SettingsException
	{
		String connParamName = (forExpectedData ? EXPECTED_CONNECTION_PARAM_NAME : ACTUAL_CONNECTION_PARAM_NAME);
		String name = InputParamsUtils.getRequiredString(actionParameters, connParamName);
		DbConnection dbConnection = (DbConnection) storage.getConnection(name, TYPE);

		if (dbConnection == null)
			throw new ConnectivityException("Connection '%s' with type '%s' does not exist.", name, TYPE);

		return dbConnection.getConnection();
	}

	@Override
	public Connection getConnection(boolean forExpectedData) throws ConnectivityException, SettingsException
	{
		if (forExpectedData)
		{
			if (expectedConnection == null)
				expectedConnection = createConnection(true);
			return expectedConnection;
		}
		else
		{
			if (actualConnection == null)
				actualConnection = createConnection(false);
			return actualConnection;
		}
	}

	@Override
	public IValueTransformer getValueTransformer()
	{
		return null;
	}

	@Override
	public void close() throws Exception
	{
		Utils.closeResource(expectedConnection);
		Utils.closeResource(actualConnection);
	}
}
