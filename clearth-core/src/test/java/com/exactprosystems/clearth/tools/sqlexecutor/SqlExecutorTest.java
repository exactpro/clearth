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

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.testng.Assert.*;

public class SqlExecutorTest
{
	private static final Path DB_DIR = Paths.get("testOutput").resolve(SqlExecutorTest.class.getSimpleName());
	private static final String DB_FILE = DB_DIR.resolve("file.db").toString(), CONN_NAME = "Executor_Con";
	private DbConnection connection;

	@BeforeClass
	public void init() throws ClearThException, IOException, SettingsException, SQLException
	{
		FileUtils.deleteDirectory(DB_DIR.toFile());
		Files.createDirectories(DB_DIR);
		prepareToTest();
	}
	
	@Test
	public void testCheckConnection() throws SQLException, ConnectivityException, SettingsException
	{
		try (Connection con = connection.getConnection())
		{
			new SqlExecutor().checkConnection(con);
			assertFalse(con.isClosed());
		}
	}

	@Test
	public void testExecuteQuery() throws SQLException, ConnectivityException, SettingsException
	{
		try (Connection con = connection.getConnection())
		{
			new SqlExecutor().executeQuery(con, "select * from tbl1", 0);
			assertFalse(con.isClosed());
		}
	}
	
	@Test
	public void testCloseConnectionIfStatementFailed() throws SQLException, ConnectivityException, SettingsException
	{
		Connection con = connection.getConnection();
		try
		{
			new SqlExecutor().executeQuery(con, "select_from", 0);
		}
		catch (SQLException e)
		{
			assertFalse(con.isClosed(), "Connection is closed after error");
		}
		finally
		{
			Utils.closeResource(con);
		}
	}
	
	private void prepareToTest() throws SettingsException, ConnectivityException, SQLException
	{
		connection = new DbConnection();

		connection.setName(CONN_NAME);
		connection.getSettings().setJdbcUrl("jdbc:sqlite:" + DB_FILE);
		connection.getSettings().setInitializationQuery("select 1");
		connection.check();
		
		Connection con = connection.getConnection();
		String createQuery = "create table tbl1 (id INTEGER PRIMARY KEY, param1 INTEGER, param2 INTEGER)",
				insertQuery = "insert into tbl1 (id, param1, param2) values (1, 22, 333)";

		execQuery(createQuery, con);
		execQuery(insertQuery, con);
	}
	
	private void execQuery(String query, Connection con) throws SQLException
	{
		try (PreparedStatement statement = con.prepareStatement(query))
		{
			statement.execute();
		}
	}
}