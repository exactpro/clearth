/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.sql.SQLUtils;

/**
 * Created by vitaly.barkhatov on 7/29/14.
 */
public abstract class UpdateSQLAction extends SQLAction
{
	@Override
	protected Result executeQuery() throws Exception
	{
		String verificationQuery = getQuery();
		String[] keys = SQLUtils.getKeysFromQuery(verificationQuery);
		Connection con = getDBConnection();
		PreparedStatement query = null;
		try
		{
			query = SQLUtils.prepareQuery(verificationQuery, keys, getQueryParams(), con, valueTransformer,null);

			getLogger().debug("Using query: {}", query);

			int recordCount = query.executeUpdate();
			DefaultResult result = new DefaultResult();
			result.setSuccess(true);
			result.setComment(recordCount + " records updated");
			return result;
		}
		finally
		{
			Utils.closeStatement(query);
			if (isNeedCloseDbConnection())
				Utils.closeResource(con);
		}
	}
}
