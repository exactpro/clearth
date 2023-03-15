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

package com.exactprosystems.clearth.tools.sqlexecutor;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlExecutorTest
{
	private static ApplicationManager appManager;
	private static SqlExecutor sqlExecutor;
	private static Connection connection;
	private static final Path RES_DIR = Paths.get("SQLExecutorTest").resolve("clearth.cfg"),
					DB_DIR = Paths.get("testOutput").resolve(SqlExecutorTest.class.getSimpleName());
	private static final String TYPE = "DB", DB_FILE = DB_DIR.resolve("file.db").toString(),
					CONN_NAME = "Executor_Con";

	@BeforeClass
	public static void init() throws ClearThException, IOException, SettingsException, SQLException
	{
		FileUtils.deleteDirectory(DB_DIR.toFile());
		Files.createDirectories(DB_DIR);

		String configFile = FileOperationUtils.resourceToAbsoluteFilePath(RES_DIR.toString());
		appManager = new ApplicationManager(configFile);
		sqlExecutor = new SqlExecutor();

		prepareToTest();
	}

	@Test
	public void testCheckConnection() throws SQLException
	{
		sqlExecutor.checkConnection(connection);
	}

	@Test
	public void testCreateStatement() throws SQLException
	{
		try (PreparedStatement statement = sqlExecutor.createStatement(connection, "select * from tbl1", 0))
		{
			Assert.assertTrue(statement.execute());
		}
	}

	@AfterClass
	public static void dispose() throws IOException, SQLException
	{
		connection.close();

		if (appManager != null)
			appManager.dispose();
	}

	private static void prepareToTest() throws SettingsException, ConnectivityException, SQLException
	{
		ClearThConnectionStorage connectionStorage = ClearThCore.connectionStorage();
		DbConnection dbConnection = (DbConnection) connectionStorage.createConnection(TYPE);

		dbConnection.setName(CONN_NAME);
		dbConnection.getSettings().setJdbcUrl("jdbc:sqlite:" + DB_FILE);
		dbConnection.getSettings().setInitializationQuery("select 1");
		dbConnection.check();

		connectionStorage.addConnection(dbConnection);

		connection = dbConnection.getConnection();

		String createQuery = "create table tbl1 (id INTEGER PRIMARY KEY, param1 INTEGER, param2 INTEGER)",
		insertQuery = "insert into tbl1 (id, param1, param2) values (1, 22, 333)";

		try(PreparedStatement prStatement1 = connection.prepareStatement(createQuery))
		{
			prStatement1.execute();
		}
		try(PreparedStatement prStatement2 = connection.prepareStatement(insertQuery))
		{
			prStatement2.execute();
		}
	}

}