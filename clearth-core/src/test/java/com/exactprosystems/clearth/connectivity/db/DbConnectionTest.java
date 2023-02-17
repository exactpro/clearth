/*******************************************************************************
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

package com.exactprosystems.clearth.connectivity.db;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

public class DbConnectionTest
{
	private static DbConnection dbConnection;
	private static Path connPath = Paths.get("testOutput").resolve("DbConnection");
	private static String dbFile = connPath.resolve("file.db").toString();

	@BeforeClass
	public static void init() throws IOException, SettingsException, ConnectivityException
	{
		FileUtils.deleteDirectory(connPath.toFile());
		Files.createDirectories(connPath);

		dbConnection = new DbConnection();
		dbConnection.setName("dbConn");
		dbConnection.getSettings().setJdbcUrl("jdbc:sqlite:" + dbFile);
		dbConnection.getSettings().setInitializationQuery("select 1");
	}

	@Test
	public void getConnectionPassed() throws SQLException, ConnectivityException, SettingsException
	{
		try(Connection connection = dbConnection.getConnection())
		{
			Assert.assertTrue(connection.isValid(100));
		}
	}

	@Test(expectedExceptions = SettingsException.class)
	public void getConnectionFailed() throws ConnectivityException, SettingsException
	{
		DbConnection dbConn = new DbConnection();
		dbConn.getConnection();
	}

	@Test(expectedExceptions = SettingsException.class)
	public void checkFailed() throws ConnectivityException, SettingsException
	{
		DbConnection dbConn = new DbConnection();
		dbConn.check();
	}

	@Test
	public void checkPassed() throws ConnectivityException, SettingsException
	{
		dbConnection.check();
	}

	@Test
	public void getConnections() throws SQLException, ConnectivityException, SettingsException
	{
		try(Connection connection1 = dbConnection.getConnection();
			Connection connection2 = dbConnection.getConnection())
		{
			connection1.close();
			Assert.assertTrue(connection1.isClosed());
			Assert.assertFalse(connection2.isClosed());
		}
	}

	@Test(expectedExceptions = ConnectivityException.class)
	public void checkQueryFailed() throws ConnectivityException, SettingsException
	{
		DbConnection dbConn = new DbConnection();
		dbConn.setName("dbConn");
		dbConn.getSettings().setJdbcUrl("jdbc:sqlite:" + dbFile);
		dbConn.getSettings().setInitializationQuery("sel 1");
		dbConn.check();
	}
}