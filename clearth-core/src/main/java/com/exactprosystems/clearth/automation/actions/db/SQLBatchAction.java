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

import com.exactprosystems.clearth.automation.SubActionData;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.SpecialValue;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.sql.ParametrizedQuery;
import com.exactprosystems.clearth.utils.sql.SQLUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public class SQLBatchAction extends SQLAction
{
	public static final String VALUES_ACTIONS = "ValuesActions";
	public static final String SIZE_OF_BATCH = "SizeOfBatch";
	private Path pathToQuery;
	private int sizeOfPart;

	@Override
	protected Result executeQuery() throws Exception
	{
		InputParamsHandler paramsHandler = new InputParamsHandler(inputParams);
		String path = paramsHandler.getRequiredFilePath(QUERY_FILE);
		String valuesGroups = paramsHandler.getRequiredString(VALUES_ACTIONS);
		sizeOfPart = paramsHandler.getInteger(SIZE_OF_BATCH, 1000);
		paramsHandler.check();
		pathToQuery = Paths.get(path);
		List<String> valuesGroupsList = Arrays.asList(valuesGroups.split(","));

		Connection connection = getDBConnection();
		if (connection == null)
			return DefaultResult.failed("DB Connection does not exist.");
		try
		{
			ParametrizedQuery parametrizedQuery = SQLUtils.parseSQLTemplate(pathToQuery.toFile());
			String query = parametrizedQuery.getQuery();

			checkValuesActionsAvailability(parametrizedQuery, valuesGroupsList);

			try (PreparedStatement ps = connection.prepareStatement(query))
			{
				executeBatches(ps, parametrizedQuery, valuesGroupsList);
				return DefaultResult.passed(String.format("%d rows were successfully processed with the query",
						valuesGroupsList.size()));
			}
		}
		finally
		{
			if (isNeedCloseDbConnection())
				connection.close();
		}
	}

	private void executeBatches(PreparedStatement ps, ParametrizedQuery parametrizedQuery,
	                            List<String> valuesGroupsList)
	{
		int i=-1, iteration = 0;
		for (String groupId : valuesGroupsList)
		{
			i++;
			try
			{
				SubActionData subActionData = getMatrixContext().getSubActionData(groupId);
				Map<String, String> valuesGroup = subActionData.getParams();
				addParamsForBatch(ps, parametrizedQuery, valuesGroup);
				ps.addBatch();
				if (i > 0 && i % (sizeOfPart - 1) == 0 || i == valuesGroupsList.size() - 1)
				{
					iteration++;
					ps.executeBatch();
				}
			}
			catch (SQLException e)
			{
				String msg = String.format("Error while creating query on iteration #%d", iteration);
				logger.error(msg, e); 
				throw new ResultException(msg+": "+e.getMessage());
			}
		}
	}

	private void addParamsForBatch(PreparedStatement ps, ParametrizedQuery parametrizedQuery,
	                               Map<String, String> valuesGroup) throws SQLException
	{

		List<String> paramsList = parametrizedQuery.getQueryParamsList();
		int i=0;
		for (String expParam: paramsList)
		{
			String paramValue = valuesGroup.get(expParam);
			i++;
			if(SpecialValue.NULL.matrixName().equals(paramValue))
			{
				ps.setString(i, null);
			}
			else
			{
				ps.setString(i, paramValue);
			}
		}
	}

	private void checkValuesActionsAvailability(ParametrizedQuery parametrizedQuery, List<String> valuesGroupsList)
	{

		List<String> missedSubActions = new ArrayList<>();
		Map<String, List<String>> missedParamsInSubActions = new LinkedHashMap<>();
		for (String groupId : valuesGroupsList)
		{
			SubActionData subActionData = getMatrixContext().getSubActionData(groupId);
			if (subActionData == null)
			{
				missedSubActions.add(groupId);
			}
			else
			{
				List<String> missedParams = new ArrayList<>();
				List<String> paramsList = parametrizedQuery.getQueryParamsList();
				Map<String, String> valuesGroup = subActionData.getParams();
				for (String expParam : paramsList)
				{
					if (valuesGroup.get(expParam) == null)
						missedParams.add(expParam);
				}
				if (missedParams.size() > 0)
				{
					missedParamsInSubActions.put(groupId, missedParams);
				}
			}
		}

		if (!missedSubActions.isEmpty())
		{
			throw new ResultException(String.format("The following actions were not found: %s", missedSubActions));
		}
		if (!missedParamsInSubActions.isEmpty())
		{
			throw new ResultException(String.format("There are missed parameters in the following actions: %s",
					missedParamsInSubActions));
		}
	}
}
