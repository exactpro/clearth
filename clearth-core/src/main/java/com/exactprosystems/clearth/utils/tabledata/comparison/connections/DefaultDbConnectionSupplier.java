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

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.util.Map;

public class DefaultDbConnectionSupplier implements DbConnectionSupplier
{
	public static final String EXPECTED_CONNECTION_PARAM_NAME = "ExpectedConnectionName",
								ACTUAL_CONNECTION_PARAM_NAME = "ActualConnectionName";
	protected GlobalContext globalContext;
	protected Connection expectedCon, actualCon;
	protected Map<String, String> actionParameters;


	public DefaultDbConnectionSupplier(Map<String, String> actionParameters, GlobalContext globalContext)
	{
		this.globalContext = globalContext;
		this.actionParameters = actionParameters;
	}

	protected Connection getDbCon(String paramName) throws ConnectivityException, SettingsException, ParametersException
	{
		String conName = InputParamsUtils.getStringOrDefault(actionParameters, paramName, "");
		if (StringUtils.isBlank(conName))
			throw new ParametersException(String.format("Parameter '%s' is empty or missing", paramName));
		return globalContext.getDbConnection(conName);
	}

	@Override
	public Connection getConnection(boolean forExpectedData)
			throws ConnectivityException, SettingsException, ParametersException
	{
		if (forExpectedData)
			return (expectedCon == null) ? expectedCon = getDbCon(EXPECTED_CONNECTION_PARAM_NAME) : expectedCon;
		return (actualCon == null) ? actualCon = getDbCon(ACTUAL_CONNECTION_PARAM_NAME) : actualCon;
	}

	@Override
	public IValueTransformer getValueTransformer()
	{
		return null;
	}

	@Override
	public void close() throws Exception
	{
		Utils.closeResource(expectedCon);
		Utils.closeResource(actualCon);
	}
}
