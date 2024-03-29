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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.data.TestExecutionHandler;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;

import java.sql.Connection;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalContext
{
	public static final String TEST_MODE = "TestMode";

	private final Map<String, Object> loadedContext;
	private final Map<String, AutoCloseable> closeableContext;
	private final Calendar currentDate;
	private final boolean weekendHoliday;
	private final Map<String, Boolean> holidays;
	private final MatrixFunctions matrixFunctions;
	private final String startedByUser;
	private final List<String> attemptedConnections;
	private final Set<Statement> statements;
	private final TestExecutionHandler executionHandler;
	private final OpenedDbConnections openedDbConnections;
	private Date started, finished;
	
	public GlobalContext(Date currentDate, boolean weekendHoliday, Map<String, Boolean> holidays, MatrixFunctions matrixFunctions, String startedByUser,
			TestExecutionHandler executionHandler)
	{
		this.loadedContext = new HashMap<>();
		this.closeableContext = new HashMap<>();
		
		if (currentDate!=null)
		{
			this.currentDate = Calendar.getInstance();
			this.currentDate.setTime(currentDate);
		}
		else
			this.currentDate = null;
		
		this.weekendHoliday = weekendHoliday;
		this.holidays = holidays;
		this.matrixFunctions = matrixFunctions;
		this.startedByUser = startedByUser;
		this.attemptedConnections = new ArrayList<String>(0);
		this.statements = ConcurrentHashMap.newKeySet();
		this.openedDbConnections = new OpenedDbConnections();

		this.executionHandler = executionHandler;
		
		this.started = null;
		this.finished = null;
	}
	
	
	public <T> T getLoadedContext(String key)
	{
		try
		{
			//noinspection unchecked
			return (T) loadedContext.get(key);
		}
		catch (ClassCastException e)
		{
			throw new ResultException(e);
		}
	}

	public <T> T getCloseableContext(String key)
	{
		try
		{
			//noinspection unchecked
			return (T) closeableContext.get(key);
		}
		catch (ClassCastException e)
		{
			throw new ResultException(e);
		}
	}
	
	public void setLoadedContext(String key, Object value)
	{
		loadedContext.put(key, value);
	}

	public void setCloseableContext(String key, AutoCloseable value)
	{
		closeableContext.put(key, value);
	}
	
	public Date getCurrentDate()
	{
		if (currentDate!=null)
		{
			Calendar now = Calendar.getInstance();
			now.set(Calendar.YEAR, currentDate.get(Calendar.YEAR));
			now.set(Calendar.MONTH, currentDate.get(Calendar.MONTH));
			now.set(Calendar.DAY_OF_MONTH, currentDate.get(Calendar.DAY_OF_MONTH));
			return now.getTime();
		}
		else
			return new Date();
	}
	
	public boolean isWeekendHoliday()
	{
		return weekendHoliday;
	}
	
	public Map<String, Boolean> getHolidays()
	{
		return holidays;
	}
	
	public MatrixFunctions getMatrixFunctions()
	{
		return matrixFunctions;
	}
	
	public String getStartedByUser()
	{
		return startedByUser;
	}
	
	public List<String> getAttemptedConnections()
	{
		return attemptedConnections;
	}

	/**
	 * Package-private access used to make statements set accessible only by Scheduler and not by actions
	 */
	Set<Statement> getSqlStatements()
	{
		return statements;
	}
	
	public void registerStatement(Statement statement)
	{
		statements.add(statement);
	}
	
	public void unregisterStatement(Statement statement)
	{
		statements.remove(statement);
	}
	
	
	public TestExecutionHandler getExecutionHandler()
	{
		return executionHandler;
	}
	
	
	public Date getStarted()
	{
		return started;
	}
	
	public void setStarted(Date started)
	{
		this.started = started;
	}
	
	public Date getFinished()
	{
		return finished;
	}
	
	public void setFinished(Date finished)
	{
		this.finished = finished;
	}

	public void clearContext()
	{
		openedDbConnections.clear();
		loadedContext.clear();
		attemptedConnections.clear();
		holidays.clear();
		clearCloseableContext();
	}

	public Connection getDbConnection(String conName) throws ConnectivityException, SettingsException
	{
		return openedDbConnections.getConnection(conName);
	}
	
	
	protected void clearCloseableContext()
	{
		for (AutoCloseable entry : closeableContext.values())
		{
			Utils.closeResource(entry);
		}
		closeableContext.clear();
	}
}
