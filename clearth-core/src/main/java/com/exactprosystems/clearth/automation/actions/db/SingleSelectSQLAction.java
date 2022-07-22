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

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.Utils;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SingleSelectSQLAction extends SelectSQLAction
{
	protected IValueTransformer dbValueTransformer = createDbValueTransformer();

	protected IValueTransformer createDbValueTransformer()
	{
		return null;
	}

	@Override
	protected Result processResultSet(ResultSet rs, String[] keys) throws ResultException
	{
		try
		{
			Result result = processQueryResult(rs, 1);
			checkExtraRecords(result, rs);

			return result;
		}
		catch (SQLException e)
		{
			return DefaultResult.failed("An error occurred while processing the query's result", e);
		}
	}

	protected void checkExtraRecords(Result result, ResultSet resultSet) throws SQLException
	{
		int count = 0;
		while (resultSet.next())
			count++;

		if (count > 0)
			result.appendComment(Utils.EOL + String.format("Action returned %d extra rows", count));
	}

}
