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

package com.exactprosystems.clearth.web.beans.tools.sqlexecutor;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.tools.sqlexecutor.QueryData;
import com.exactprosystems.clearth.tools.sqlexecutor.QueryOperator;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class QueryBean extends ClearThBean
{
	private SqlExecutorBean sqlBean;
	private List<String> queryNames;
	private final QueryOperator operator;
	
	public QueryBean()
	{
		operator = createOperator();
	}
	
	public void setSqlBean(SqlExecutorBean sqlBean)
	{
		this.sqlBean = sqlBean;
	}
	
	
	public void loadQueryNames()
	{
		String selectedConnection = sqlBean.getSelectedConnection();
		if (isEmpty(selectedConnection))
		{
			queryNames = null;
			resetQuery();
			return;
		}
		
		try
		{
			queryNames = operator.getQueryNames(selectedConnection);
			updateQuery();
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while loading query names", e, getLogger());
		}
	}
	
	public List<String> getQueryNames()
	{
		return queryNames;
	}
	
	public void loadQuery()
	{
		String selectedConnection = sqlBean.getSelectedConnection(),
				queryName = sqlBean.getQueryName();
		if (isEmpty(selectedConnection) || isEmpty(queryName))
		{
			resetQuery();
			return;
		}
		
		try
		{
			QueryData data = operator.loadQuery(selectedConnection, queryName);
			updateQuery(data);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while loading query", e, getLogger());
		}
	}
	
	public void saveQuery()
	{
		String selectedConnection = sqlBean.getSelectedConnection(),
				queryName = sqlBean.getQueryName();
		if (isEmpty(selectedConnection) || isEmpty(queryName) || isEmpty(sqlBean.getQueryBody()))
		{
			MessageUtils.addWarningMessage("Could not save query", "DB connection must be selected, query name and text specified");
			return;
		}
		
		try
		{
			operator.saveQuery(selectedConnection, queryName, createQueryData());
			getLogger().info("saved query '{}'", queryName);
			loadQueryNames();
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Error while saving query", e, getLogger());
		}
		catch (SettingsException e)
		{
			MessageUtils.addErrorMessage("Invalid query", e.getMessage());
		}
	}
	
	
	protected QueryOperator createOperator()
	{
		return new QueryOperator(Paths.get(ClearThCore.rootRelative("queries")), QueryData.class);
	}
	
	protected QueryData createQueryData()
	{
		return new QueryData(sqlBean.getQueryBody(), sqlBean.getQueryComment());
	}
	
	
	protected void resetQuery()
	{
		sqlBean.setQueryName(null);
		sqlBean.setQueryBody(null);
		sqlBean.setQueryComment(null);
	}
	
	protected void updateQuery(QueryData data)
	{
		sqlBean.setQueryBody(data.getQuery());
		sqlBean.setQueryComment(data.getComment());
	}
	
	
	private void updateQuery()
	{
		String queryName = sqlBean.getQueryName();
		if (isEmpty(queryName))
			return;
		
		if (queryNames == null || !queryNames.contains(queryName))
			resetQuery();
		else
			loadQuery();
	}
}