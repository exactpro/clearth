/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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
package com.exactprosystems.clearth.automation.actions.db.checkers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.ResultDetail;
import com.exactprosystems.clearth.automation.report.results.ContainerResult;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.report.results.DetailedResult;
import com.exactprosystems.clearth.utils.ComparisonUtils;

import static com.exactprosystems.clearth.utils.sql.SQLUtils.getColumnNamesSet;
import static java.lang.String.join;
import static java.text.MessageFormat.format;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public class DefaultActionResultSetChecker implements ActionResultSetChecker
{
	private static final ComparisonUtils comparisonUtils = ClearThCore.getInstance().getComparisonUtils();
	private static final int MAX_CHECKING_RECORDS_COUNT = 10;

	private final Map<String, String> inputParams;
	private final int expectedRecordsCount;

	private Set<String> outputParams;


	private DefaultActionResultSetChecker(Map<String, String> inputParams, int expectedRecordsCount)
	{
		this.inputParams = inputParams;
		this.expectedRecordsCount = expectedRecordsCount;
	}

	public DefaultActionResultSetChecker(Map<String, String> inputParams, Set<String> outputParams, int expectedRecordsCount)
	{
		this(inputParams, expectedRecordsCount);
		this.outputParams = isEmpty(outputParams) ? null : outputParams;
	}


	@Override
	public ActionSqlResult check(ResultSet resultSet) throws SQLException
	{
		ActionSqlResult actionSqlResult = new ActionSqlResult();

		Set<String> databaseColumnNames = getColumnNamesSet(resultSet);
		Map<String, String> firstRecordOutputParams = new LinkedHashMap<>();
		Result result = checkResultSet(resultSet, databaseColumnNames, firstRecordOutputParams);
		actionSqlResult.setResult(result);

		if (result instanceof DefaultResult)
			return actionSqlResult;

		if (outputParams != null && ((ContainerResult) result).getDetails().size() == 1)
			actionSqlResult.addOutputParams(firstRecordOutputParams);

		return actionSqlResult;
	}


	private Result checkResultSet(ResultSet resultSet, Set<String> databaseColumnNames, 
	                              Map<String, String> firstRecordOutputParams) throws SQLException
	{
		Set<String> columnNames = getAllColumnNames(databaseColumnNames, inputParams);
		int checkedRecords = 0;
		ContainerResult result;

		if (!resultSet.next())
		{
			if (expectedRecordsCount != 0)
				return DefaultResult.failed("Query result is empty.");
			else
				return DefaultResult.passed("Query result is empty");
		}
		else
		{
			result = new ContainerResult();

			do
			{
				if (checkedRecords == expectedRecordsCount + MAX_CHECKING_RECORDS_COUNT)
					break;

				DetailedResult checkedRecord = new DetailedResult();
				for (String columnName : columnNames)
				{
					String inputValue = inputParams.get(columnName);
					String queryValue = databaseColumnNames.contains(columnName) ? resultSet.getString(columnName) : null;
					ResultDetail resultDetail = comparisonUtils.createResultDetail(columnName, inputValue, queryValue,
							ComparisonUtils.InfoIndication.NULL_OR_EMPTY);
					checkedRecord.addResultDetail(resultDetail);

					if ((checkedRecords == 0) && (outputParams != null) && outputParams.contains(columnName))
						firstRecordOutputParams.put(columnName, queryValue);
				}

				result.addDetail(checkedRecord);
				checkedRecords++;
			} 
			while (resultSet.next());
		}

		checkRecordsCount(result, checkedRecords, expectedRecordsCount);
		checkOutputParams(result, databaseColumnNames);

		return result;
	}

	private void checkRecordsCount(Result result, int checkedRecordsCount, int expectedRecordsCount)
	{
		if (checkedRecordsCount == expectedRecordsCount)
			return;
		
		result.setSuccess(false);
		
		if (checkedRecordsCount == expectedRecordsCount + MAX_CHECKING_RECORDS_COUNT)
		{
			result.setComment(format("Query result contains more rows than expected. " +
							"Expected: {0}, actual: more than {1}.", expectedRecordsCount, checkedRecordsCount));
		}
		else if (checkedRecordsCount > expectedRecordsCount)
		{
			result.setComment(format("Query result contains more rows than expected. Expected: {0}, actual: {1}.",
					expectedRecordsCount, checkedRecordsCount));
		}
		else
		{
			result.setComment(format("Query result contains less rows than expected. Expected: {0}, actual: {1}.",
					expectedRecordsCount, checkedRecordsCount));
		}
	}

	private void checkOutputParams(Result result, Set<String> databaseColumnNames)
	{
		if (isEmpty(outputParams))
			return;

		Set<String> missedOutputParams = new LinkedHashSet<>(outputParams);
		if (databaseColumnNames != null)
			missedOutputParams.removeAll(databaseColumnNames);
		if (missedOutputParams.isEmpty())
			return;

		result.setFailReason(FailReason.FAILED);
		result.setSuccess(false);
		result.appendComment(format("The following output parameters are missed in query result: {0}", 
				join(", ", missedOutputParams)));
	}

	private Set<String> getAllColumnNames(Set<String> databaseColumnNames, Map<String, String> inputParams)
	{
		Set<String> columnNamesSet = new LinkedHashSet<>(databaseColumnNames);
		columnNamesSet.addAll(inputParams.keySet());
		return columnNamesSet;
	}
}
