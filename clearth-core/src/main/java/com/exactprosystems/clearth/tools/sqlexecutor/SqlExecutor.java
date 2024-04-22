/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.tools.sqlexecutor;

import com.exactprosystems.clearth.utils.ObjectToStringTransformer;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class SqlExecutor
{
	private static final Logger logger = LoggerFactory.getLogger(SqlExecutor.class);
	
	private ObjectToStringTransformer transformer;

	public SqlExecutor() {
	}

	public SqlExecutor(ObjectToStringTransformer transformer)
	{
		this.transformer = transformer;
	}

	public final CompletableFuture<StringTableData> executeQuery(Connection dbConnection, String query, int maxRows)
			throws SQLException
	{
		PreparedStatement stmt = createStatement(dbConnection, query, maxRows);

		CompletableFuture<StringTableData> result = CompletableFuture.supplyAsync(() -> {
			try
			{
				if (transformer == null)
					return DbDataReader.read(stmt);
				else
					return DbDataReader.read(stmt, transformer);
			}
			catch (Exception e)
			{
				throw new CompletionException(e);
			}
		});

		result.whenComplete((r, e) -> handleCompletion(r, e, stmt));
		return result;
	}
	
	public final void checkConnection(Connection dbConnection) throws SQLException
	{
		String query = getCheckConnectionQuery(dbConnection);
		try (PreparedStatement stmt = dbConnection.prepareStatement(query))
		{
			stmt.execute();
		}
	}
	
	protected PreparedStatement createStatement(Connection dbConnection, String query, int maxRows) throws SQLException
	{
		PreparedStatement result = dbConnection.prepareStatement(query);
		if (maxRows > 0)
			result.setMaxRows(maxRows);
		return result;
	}
	
	protected void handleCompletion(StringTableData result, Throwable error, PreparedStatement stmt)
	{
		if (error instanceof CancellationException)
		{
			try
			{
				stmt.cancel();
			}
			catch (SQLException e)
			{
				logger.error("Error while cancelling query execution", e);
			}
		}
		Utils.closeResource(stmt);
	}
	
	protected String getCheckConnectionQuery(Connection dbConnection)
	{
		return "SELECT 1";
	}
}
