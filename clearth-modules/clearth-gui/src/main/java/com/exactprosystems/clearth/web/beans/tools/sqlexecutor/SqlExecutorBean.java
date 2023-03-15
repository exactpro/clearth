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
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.tools.sqlexecutor.SqlExecutor;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.writers.CsvDataWriter;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.primefaces.model.StreamedContent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class SqlExecutorBean extends ClearThBean
{
	private static final String ELLIPSIS = "...";
	
	private final SqlExecutor executor;
	private String selectedConnection,
			queryBody,
			queryName,
			queryComment;
	
	private CompletableFuture<StringTableData> resultFuture;
	private StringTableData result;
	private Set<String> header;
	private static final String TYPE = "DB";
	
	public SqlExecutorBean()
	{
		executor = createExecutor();
	}

	public Set<String> getConnectionNames()
	{
		List<ClearThConnection> connectionList = ClearThCore.connectionStorage().getConnections(TYPE);
		Set<String> conNames = new LinkedHashSet<>();

		for(ClearThConnection connection : connectionList)
			conNames.add(connection.getName());

		return conNames;
	}

	public boolean isRunning()
	{
		return resultFuture != null && !resultFuture.isDone();
	}
	
	public void executeQuery()
	{
		if (isBlank(queryBody))
			return;
		
		if (isEmpty(selectedConnection))
		{
			MessageUtils.addWarningMessage("No DB connection selected", "");
			return;
		}
		
		if (isRunning())
		{
			MessageUtils.addInfoMessage("Another query is being executed", "");
			return;
		}
		
		Connection con = getConnection(selectedConnection);
		if (con == null)
			return;
		
		try
		{
			resetResult();
			resultFuture = executor.executeQuery(con, queryBody, getMaxRows());
			resultFuture.whenComplete((r, e) -> handleQueryCompletion(r, e));
			getLogger().info("started execution of query: {}", getLimitedQueryText(queryBody));
		}
		catch (SQLException e)
		{
			WebUtils.logAndGrowlException("Error while preparing query", e, getLogger());
		}
	}
	
	public StreamedContent downloadCsvResult()
	{
		if (result == null)
			return null;
		
		try
		{
			Path temp = Paths.get(ClearThCore.tempPath());
			String prefix = "query_";
			File file = Files.createTempFile(temp, prefix, ".csv").toFile();
			CsvDataWriter.write(result, new BufferedWriter(new FileWriter(file)), true);
			
			File zip = Files.createTempFile(temp, prefix, ".zip").toFile();
			FileOperationUtils.zipFiles(zip, ArrayUtils.toArray(file));
			return WebUtils.downloadFile(zip);
		}
		catch (Exception e)
		{
			String msg = "Error while saving result";
			getLogger().error(msg, e);
			MessageUtils.addErrorMessage(msg, "Check logs for details");
			return null;
		}
	}
	
	public void cancel()
	{
		if (!isRunning())
			return;
		
		try
		{
			resultFuture.cancel(true);
			getLogger().info("cancelled execution of query: {}", getLimitedQueryText(queryBody));
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Error while cancelling execution", e, getLogger());
		}
	}
	
	protected void handleQueryCompletion(StringTableData result, Throwable error)
	{
		if (error != null)
		{
			WebUtils.logAndGrowlException("Error while executing query", error, getLogger());
			return;
		}
		
		processResult(result);
		resultFuture = null;
	}
	
	
	public void checkConnection()
	{
		if (isEmpty(selectedConnection))
			return;
		
		try
		{
			Connection con = getConnection(selectedConnection);
			if (con == null)
				return;
			
			executor.checkConnection(con);
			
			MessageUtils.addInfoMessage("DB connection '"+selectedConnection+"' is valid", "");
		}
		catch (SQLException e)
		{
			WebUtils.logAndGrowlException("Error while checking DB connection '"+selectedConnection+"'", e, getLogger());
		}
	}
	
	public Set<String> getResultHeader()
	{
		return header;
	}
	
	public List<TableRow<String, String>> getResultRows()
	{
		return result != null ? result.getRows() : null;
	}
	
	
	public String getSelectedConnection()
	{
		return selectedConnection;
	}
	
	public void setSelectedConnection(String selectedConnection)
	{
		this.selectedConnection = selectedConnection;
	}
	
	
	public String getQueryBody()
	{
		return queryBody;
	}
	
	public void setQueryBody(String queryBody)
	{
		this.queryBody = queryBody;
	}
	
	
	public String getQueryName()
	{
		return queryName;
	}
	
	public void setQueryName(String queryName)
	{
		this.queryName = queryName;
	}
	
	
	public String getQueryComment()
	{
		return queryComment;
	}
	
	public void setQueryComment(String queryComment)
	{
		this.queryComment = queryComment;
	}
	
	
	protected Connection getConnection(String name)
	{
		try
		{
			DbConnection dbConnection = (DbConnection) ClearThCore.connectionStorage().getConnection(name, TYPE);
			if(dbConnection == null)
			{
				MessageUtils.addErrorMessage("No connection", "DB connection '"+name+"' doesn't exist");
				return null;
			}
			return dbConnection.getConnection();
		}
		catch (ConnectivityException e)
		{
			WebUtils.logAndGrowlException("Error while opening DB connection '"+name+"'", e, getLogger());
			return null;
		}
		catch (SettingsException e)
		{
			MessageUtils.addErrorMessage("Settings of DB connection '"+name+"' are invalid", e.getMessage());
			return null;
		}
	}
	
	protected void processResult(StringTableData result)
	{
		this.result = result;
		header = getHeader(result);
	}
	
	protected void resetResult()
	{
		result = null;
		header = null;
	}

	protected final String getLimitedQueryText(String query)
	{
		return getLimitedStringValue(query, getQueryLimit());
	}

	public String getLimitedColumnValueString(String columnValue)
	{
		return getLimitedStringValue(columnValue, getColumnValueLimit());
	}

	public String getLimitedStringValue(String value, int limit)
	{
		return value.length() > limit+ELLIPSIS.length() ? value.substring(0, limit)+ELLIPSIS : value;
	}
	
	protected SqlExecutor createExecutor()
	{
		return new SqlExecutor();
	}
	
	protected int getMaxRows()
	{
		return 10000;
	}
	
	protected int getQueryLimit()
	{
		return 100;
	}

	protected int getColumnValueLimit()
	{
		return 1000;
	}
	
	private Set<String> getHeader(StringTableData table)
	{
		Set<String> header = new LinkedHashSet<>();
		for (String h : table.getHeader())
			header.add(h);
		return header;
	}
}
