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

package com.exactprosystems.clearth.automation.actions.db;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.sql.DefaultSQLValueTransformer;
import com.exactprosystems.clearth.utils.sql.SQLUtils;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class SQLAction extends Action
{

	private GlobalContext globalContext;
	private MatrixContext matrixContext;
	private StepContext stepContext;

	protected IValueTransformer valueTransformer;
	protected Map<String,String> queryParams;


	@Override
	public Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws FailoverException
	{
		this.globalContext = globalContext;
		this.matrixContext = matrixContext;
		this.stepContext = stepContext;

		this.valueTransformer = createValueTransformer();

		queryParams = new LinkedHashMap<String, String>(inputParams);
		
		try
		{
			return executeQuery();
		}
		catch (Exception e)
		{
			getLogger().error("Error while running action", e);
			if (SQLUtils.isConnectException(e))
				throw new FailoverException("Error while running action", e, FailoverReason.CONNECTION_ERROR, "database");
			return DefaultResult.failed(e);
		}
	}


	protected String getQuery() throws Exception
	{
		String query = getGlobalContext().getLoadedContext(getQueryName());

		if(query == null)
		{
			prepare();
			query = getGlobalContext().getLoadedContext(getQueryName());
		}

		return query;
	}

	protected Map<String, String> getQueryParams()
	{
		return queryParams;
	}

	protected abstract Result executeQuery() throws Exception;

	protected abstract Connection getDBConnection() throws Exception;

	/**
	 * Override this method to return true in implementations with DB Connection Pool.
	 * Connection will be returned to pool by calling Connection.close().
	 * 
	 * @return true if connection should be closed after using.
	 */
	protected boolean isNeedCloseDbConnection()
	{
		return false;
	}

	@Override
	public int getActionType()
	{
		return ActionType.DB;
	}

	public GlobalContext getGlobalContext()
	{
		return globalContext;
	}
	
	public MatrixContext getMatrixContext()
	{
		return matrixContext;
	}

	public StepContext getStepContext()
	{
		return stepContext;
	}

	protected IValueTransformer createValueTransformer ()
	{
		return new DefaultSQLValueTransformer();
	}


	protected void prepare() throws Exception
	{
		getGlobalContext().setLoadedContext(getQueryName(), SQLUtils.loadQuery(getQueryFileName()));
	}

	protected abstract String getQueryName();
	protected abstract String getQueryFileName();
}

