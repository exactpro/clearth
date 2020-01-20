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
import java.sql.SQLException;
import java.util.*;

public class ParametrizedQuery
{
	private static final Logger log = LoggerFactory.getLogger(ParametrizedQuery.class);
	
	private String query;
	private final List<String> queryParams = new ArrayList<>();
	private final Map<String, Object[]> multiParams = new HashMap<>();
	private String multiParamsDelimiter = ",";

	public ParametrizedQuery(String query) throws SQLException
	{
		this(query, new ArrayList<String>());
	}

	public ParametrizedQuery(String query, List<String> queryParams) throws SQLException 
	{
		this(query, queryParams, new HashSet<String>());
	}

	public ParametrizedQuery(String query, List<String> queryParams, Set<String> multiParams, String multiParamsDelimiter)
			throws SQLException
	{
		this(query, queryParams, multiParams);
		this.multiParamsDelimiter = multiParamsDelimiter;
	}

	public ParametrizedQuery(String query, List<String> queryParams, Set<String> multiParams) throws SQLException
	{
		if (query == null) 
			throw new SQLException("Null query statement");

		this.query = query;
		if (queryParams != null)
			this.queryParams.addAll(queryParams);
		if (multiParams != null)
			multiParams.forEach(v -> this.multiParams.put(v, null));
	}

	public PreparedStatement createPreparedStatement(Connection connection, Map<String, ?> paramsMap) throws SQLException 
	{
		paramsMap = paramsMap != null ? paramsMap : new HashMap<String, Object>();
		processParameters(paramsMap);
		PreparedStatement statement = connection.prepareStatement(query);
		List<Object> puttedParamValues;
		try
		{
			puttedParamValues = setPreparedStatementParams(statement, paramsMap);
		}
		catch (SQLException e)
		{
			Utils.closeResource(statement);
			throw e;
		}
		if (log.isDebugEnabled())
			log.debug("Created query statement:{}{}", Utils.EOL, createQueryOutput(puttedParamValues));
		
		return statement;
	}

	public int executeUpdate(Connection connection, Map<String, ?> paramsMap) throws SQLException 
	{
		try(PreparedStatement preparedStatement = createPreparedStatement(connection, paramsMap))
		{
			return preparedStatement.executeUpdate();
		}
	}

	protected void processParameters(Map<String, ?> paramsMap) throws SQLException 
	{
		Set<String> paramsInMap = paramsMap.keySet();
		int paramIndex = 0;
		int bias = 0;
		for (String queryParam : queryParams) 
		{
			if (!paramsInMap.contains(queryParam))
				throw new SQLException(String.format("Required param '%s' is not specified", queryParam));
			
			if (multiParams.containsKey(queryParam))
			{
				Object[] arrayFromParamValue = getArrayFromParamValue(paramsMap.get(queryParam));
				multiParams.put(queryParam, arrayFromParamValue);
				int length = arrayFromParamValue.length;
				modifyQueryPlaceholders(paramIndex, bias, length);
				bias += length - 1;
			}
			paramIndex++;
		}
	}

	protected void modifyQueryPlaceholders(int paramIndex, int bias, int paramNumber)
	{
		int phPos = StringUtils.ordinalIndexOf(query, "?", paramIndex + bias + 1);
		String overlay = StringUtils.repeat("?", ",", paramNumber);
		query = StringUtils.overlay(query, overlay, phPos, phPos + 1);
	}

	protected List<Object> setPreparedStatementParams(PreparedStatement statement, Map<String, ?> paramsMap)
			throws SQLException
	{
		List<Object> puttedParamValues = new ArrayList<>();
		List<String> queryParams = getQueryParamsList();
		int bias = 0;
		for (int i = 0; i < queryParams.size(); i++)
		{
			String paramName = queryParams.get(i);
			Object paramValue = paramsMap.get(paramName);
			Object[] values = multiParams.get(paramName);
			if (values != null)
			{
				int internalIterationNumber = 0;
				for (Object value : values)
				{
					if (internalIterationNumber++ > 0)
						bias++;
					statement.setObject(i + bias + 1, value);
				}
			}
			else
				statement.setObject(i + bias + 1, paramValue);

			puttedParamValues.add(paramValue);
		}
		
		return puttedParamValues;
	}

	public List<String> getQueryParamsList() 
	{
		return new ArrayList<String>(queryParams);
	}

	public int getRequiredParamsCount()
	{
		return queryParams.size();
	}

	protected String createQueryOutput(List<Object> params)
	{
		StringBuilder output = new StringBuilder(query);
		if (CollectionUtils.isNotEmpty(params))
			output.append(Utils.EOL).append("Params: ").append(StringUtils.join(params, ", "));
		return output.toString();
	}

	@Override
	public boolean equals(Object o) 
	{
		if (this == o) 
			return true;
		if (o == null || getClass() != o.getClass()) 
			return false;

		ParametrizedQuery that = (ParametrizedQuery) o;

		if (!query.equals(that.query)) 
			return false;
		
		return queryParams.equals(that.queryParams);
	}

	@Override
	public int hashCode()
	{
		int result = query.hashCode();
		result = 31 * result + queryParams.hashCode();
		return result;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("----SQL START----").append(Utils.EOL)
				.append(query).append(Utils.EOL)
				.append("-----SQL END-----");
		if (CollectionUtils.isNotEmpty(queryParams))
			sb.append(Utils.EOL).append("Params: ").append(StringUtils.join(queryParams, ", "));
		return sb.toString();
	}

	public String getQuery()
	{
		return query;
	}

	protected Object[] getArrayFromParamValue(Object value)
	{
		Object[] result;

		if (value == null)
		{
			result = new Object[]{null};
			return result;
		}

		if (value instanceof Collection)
		{
			result = ((Collection<?>) value).toArray();
			return result;
		}

		if (value instanceof String)
		{
			result = ((String) value).split(multiParamsDelimiter);
			return result;
		}

		if (value.getClass().isArray())
		{
			result = (Object[]) value;
			return result;
		}

		result = new Object[]{value};
		return result;
	}

}
