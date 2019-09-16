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

package com.exactprosystems.clearth.utils.sql;

import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ParametrizedQuery
{
	private static final Logger log = LoggerFactory.getLogger(ParametrizedQuery.class);
	
	private final String query;
	private final List<String> queryParams;

	public ParametrizedQuery(String query) throws SQLException {
		this(query, new ArrayList<String>());
	}

	public ParametrizedQuery(String query, List<String> queryParams) throws SQLException {
		if (query == null) {
			throw new SQLException("Null query statement");
		}

		this.query = query;
		this.queryParams = queryParams != null ? queryParams : new ArrayList<String>();
	}

	public PreparedStatement createPreparedStatement(Connection connection, Map<String, ?> paramsMap) throws SQLException {
		paramsMap = paramsMap != null ? paramsMap : new HashMap<String, Object>();
		checkParamsMap(paramsMap);
		PreparedStatement statement = connection.prepareStatement(query);
		List<Object> puttedParamValues = setPreparedStatementParams(statement, paramsMap);
		if (log.isDebugEnabled()) {
			log.debug("Created query statement:" + Utils.EOL + "{}", createQueryOutput(puttedParamValues));
		}
		return statement;
	}

	public int executeUpdate(Connection connection, Map<String, ?> paramsMap) throws SQLException {
		try(PreparedStatement preparedStatement = createPreparedStatement(connection, paramsMap))
		{
			return preparedStatement.executeUpdate();
		}
	}

	protected void checkParamsMap(Map<String, ?> paramsMap) throws SQLException {
		Set<String> paramsInMap = paramsMap.keySet();
		for (String queryParam : queryParams) {
			if (!paramsInMap.contains(queryParam)) {
				throw new SQLException(String.format("Required param '%s' is not specified", queryParam));
			}
		}
	}

	protected List<Object> setPreparedStatementParams(PreparedStatement statement, Map<String, ?> paramsMap) throws SQLException {
		List<Object> puttedParamValues = new ArrayList<Object>();
		for (int i = 0; i < queryParams.size(); i++) {
			Object paramValue = paramsMap.get(queryParams.get(i));
			statement.setObject(i + 1, paramValue);
			puttedParamValues.add(paramValue);
		}
		return puttedParamValues;
	}

	public List<String> getQueryParamsList() {
		return new ArrayList<String>(queryParams);
	}

	public int getRequiredParamsCount() {
		return queryParams.size();
	}

	protected String createQueryOutput(List<Object> params) {
		StringBuilder output = new StringBuilder(query);
		if (CollectionUtils.isNotEmpty(params)) {
			output.append(Utils.EOL).append("Params: ").append(StringUtils.join(params, ", "));
		}
		return output.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ParametrizedQuery that = (ParametrizedQuery) o;

		if (!query.equals(that.query)) return false;
		return queryParams.equals(that.queryParams);
	}

	@Override
	public int hashCode() {
		int result = query.hashCode();
		result = 31 * result + queryParams.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("----SQL START----").append(Utils.EOL)
				.append(query).append(Utils.EOL)
				.append("-----SQL END-----");
		if (CollectionUtils.isNotEmpty(queryParams)) {
			sb.append(Utils.EOL).append("Params: ").append(StringUtils.join(queryParams, ", "));
		}
		return sb.toString();
	}

	public String getQuery()
	{
		return query;
	}
}
