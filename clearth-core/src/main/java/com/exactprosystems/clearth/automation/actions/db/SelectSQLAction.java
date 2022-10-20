/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.actions.db;

import java.io.IOException;
import java.sql.*;

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.sql.SQLUtils;

public abstract class SelectSQLAction extends SQLAction
{
	protected PreparedStatement prepareStatement(String query, String[] keys, Connection con) throws SQLException {
		PreparedStatement parametrizedQuery = SQLUtils.prepareQuery(query, keys, getQueryParams(), con, valueTransformer, getVerificationMapping());

//		String queryText = "Using query: " + parametrizedQuery.toString();
//
//		if (queryText.contains("oracle.jdbc.driver.OraclePreparedStatementWrapper"))
//		{
//			queryText = "Probable query: " + SQLUtils.probableQueryTextForLogOnly(query, keys, getQueryParams(),
//					valueTransformer);
//		}
//
//
//		getLogger().debug(queryText);
		return parametrizedQuery;
	}
	
	@Override
	protected Result executeQuery() throws Exception
	{
		String query = getQuery();
		String[] keys = SQLUtils.getKeysFromQuery(query);
		Connection con = getDBConnection();
		PreparedStatement parametrizedQuery = null;
		try
		{
			parametrizedQuery = this.prepareStatement(query, keys, con);

			ResultSet rs = parametrizedQuery.executeQuery();
			return processResultSet(rs, keys);
		}
		finally
		{
			Utils.closeStatement(parametrizedQuery);
			if (isNeedCloseDbConnection())
				Utils.closeResource(con);
		}
	}

	protected abstract Result processResultSet(ResultSet rs, String[] keys) throws SQLException, IOException, ResultException;
}
