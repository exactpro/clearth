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

package com.exactprosystems.clearth.automation.actions.db;


import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Class to get data for CheckPointer from DB according to query 
 */

public class SQLCheckPointer extends SelectSQLAction
{
	private static final String POSTFIX = "_cp",
			QUERY_POSTFIX = "Query",
			QUERY_FILE = "QueryFile";

	public static String getCPName(String actionId)
	{
		return actionId+POSTFIX;
	}
	
	protected String getQuery(GlobalContext globalContext)
	{
		return (String)globalContext.getLoadedContext(this.getName() + QUERY_POSTFIX);
	}

	@Override
	protected Result processResultSet(ResultSet rs, String[] keys) throws SQLException, ResultException
	{
		ResultSetMetaData md = rs.getMetaData();
		if (!rs.next()) //No data found
			return DefaultResult.failed("No data returned, CheckPointer not set. " +
					"Please, check query that gets data for CheckPointer");
		
		Map<String, String> cp = new LinkedHashMap<String, String>();
		for (int i=0; i<md.getColumnCount(); i++)
			cp.put(md.getColumnName(i+1), rs.getString(i+1));
		
		getMatrixContext().setContext(getCPName(idInMatrix), cp);

		logger.info("{} has successfully set CheckPointer", idInMatrix);
		return null;
	}

}
