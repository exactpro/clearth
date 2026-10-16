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

package com.exactprosystems.clearth.web.api;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.ws.rs.core.Response;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.connectivity.db.DbConnectionSettings;
import com.exactprosystems.clearth.tools.sqlexecutor.SqlExecutor;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;
import com.exactprosystems.clearth.utils.tabledata.writers.CsvDataWriter;

public class SqlExecutorApi
{
	private static final Logger logger = LoggerFactory.getLogger(SqlExecutorApi.class);
	
	private static final String PREFIX = "api_query_",
			ELLIPSIS = "...";
	
	public Response executeQuery(String connectionName, String query, int maxRows)
	{
		Connection con;
		try
		{
			con = getConnection(connectionName);
			if (con == null)
				return noConnection(connectionName);
			
			try (PreparedStatement stmt = createStatement(con, query, maxRows);
					DbDataReader reader = createReader(stmt);)
			{
				File file = createStorageFile();
				writeData(reader, file);
				
				File zip = compressFile(file);
				return ApiUtils.fileResponse(zip.getAbsolutePath(), zip.getName());
			}
			catch (IOException e)
			{
				String msg = "Error while writing query result to file";
				logger.error(msg, e);
				return ApiUtils.error(msg, "Check logs for details");
			}
			catch (SQLException e)
			{
				return ApiUtils.errorWithLog("Error while executing query "+getLimitedQueryText(query), e, logger);
			}
			finally
			{
				Utils.closeResource(con);
			}
		}
		catch (SQLException e)
		{
			return connectionError(connectionName, e);
		}
	}
	
	public Response checkConnection(String connectionName)
	{
		Connection con;
		try
		{
			con = getConnection(connectionName);
			if (con == null)
				return noConnection(connectionName);
			
			try
			{
				SqlExecutor executor = new SqlExecutor();
				executor.checkConnection(con);
				return ApiUtils.ok("DB connection '"+connectionName+"' is valid");
			}
			catch (SQLException e)
			{
				return ApiUtils.errorWithLog("Error while checking DB connection '"+connectionName+"'", e, logger);
			}
			finally
			{
				Utils.closeResource(con);
			}
		}
		catch (SQLException e)
		{
			return connectionError(connectionName, e);
		}
	}
	
	
	private Connection getConnection(String name) throws SQLException
	{
		DbConnection con = (DbConnection)ClearThCore.getInstance().getConnectionStorage().getConnection(name, "DB");
		if (con == null)
			return null;
		
		DbConnectionSettings settings = con.getSettings();
		return ClearThCore.createDBConnection(settings.getJdbcUrl(), settings.getUsername(), settings.getPassword());
	}
	
	private PreparedStatement createStatement(Connection con, String query, int maxRows) throws SQLException
	{
		PreparedStatement result = con.prepareStatement(query);
		if (maxRows > 0)
			result.setMaxRows(maxRows);
		return result;
	}
	
	private DbDataReader createReader(PreparedStatement stmt) throws IOException
	{
		DbDataReader result = new DbDataReader(stmt);
		result.start();
		return result;
	}
	
	private void writeData(DbDataReader reader, File file) throws IOException
	{
		if (reader.hasMoreData())
			writeQueryResult(reader, file);
		else
			writeResultColumns(reader, file);
	}
	
	private File createStorageFile() throws IOException
	{
		Path temp = Paths.get(ClearThCore.tempPath());
		return Files.createTempFile(temp, PREFIX, ".csv").toFile();
	}
	
	private File compressFile(File file) throws IOException
	{
		File result = Files.createTempFile(file.getParentFile().toPath(), PREFIX, ".zip").toFile();
		FileOperationUtils.zipFiles(result, ArrayUtils.toArray(file));
		return result;
	}
	
	private void writeQueryResult(DbDataReader reader, File file) throws IOException
	{
		try (CsvDataWriter writer = new CsvDataWriter(reader.getTableData().getHeader(), new BufferedWriter(new FileWriter(file)), true))
		{
			do
			{
				TableRow<String, String> row = reader.readRow();
				writer.write(row);
			}
			while (reader.hasMoreData());
		}
	}
	
	private void writeResultColumns(DbDataReader reader, File file) throws IOException
	{
		try (CSVPrinter printer = new CSVPrinter(new BufferedWriter(new FileWriter(file)), CSVFormat.DEFAULT))
		{
			printer.printRecord(reader.getTableData().getHeader());
		}
	}
	
	
	private Response noConnection(String connectionName)
	{
		return ApiUtils.badRequest("DB connection '"+connectionName+"' does not exist");
	}
	
	private Response connectionError(String connectionName, Exception e)
	{
		return ApiUtils.errorWithLog("Error while establishing connection '"+connectionName+"'", e, logger);
	}
	
	private String getLimitedQueryText(String query)
	{
		int limit = 100;
		return query.length() > limit+ELLIPSIS.length() ? query.substring(0, limit)+ELLIPSIS : query;
	}
}
