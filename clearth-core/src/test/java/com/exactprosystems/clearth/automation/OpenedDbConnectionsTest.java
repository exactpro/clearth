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

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import static com.exactprosystems.clearth.ApplicationManager.ADMIN;
import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

public class OpenedDbConnectionsTest
{
	private static ApplicationManager manager;
	private static final Path DB_DIR = Paths.get("testOutput").resolve(OpenedDbConnectionsTest.class.getSimpleName());
	private static final String DB_FILE = DB_DIR.resolve("file.db").toString(), TYPE = "DB";
	private static final File CFG_FILE = USER_DIR.resolve("src").resolve("test").resolve("resources")
					.resolve("OpenedDbConnectionsTest/clearth.cfg").toFile();
	private static final String CON1 = "con1", CON2 = "con2";

	@BeforeClass
	public static void init() throws IOException, ClearThException, SettingsException
	{
		FileUtils.deleteDirectory(DB_DIR.toFile());
		Files.createDirectories(DB_DIR);

		if(!CFG_FILE.isFile())
			throw new FileNotFoundException("File '" + CFG_FILE.getName() + "' not found");

		manager = new ApplicationManager(CFG_FILE.toString());

		initStorage();
	}

	@AfterClass
	public static void dispose() throws IOException
	{
		if (manager != null)
			manager.dispose();
	}

	@Test
	public void testGetConnection() throws ConnectivityException, SettingsException, SQLException
	{
		OpenedDbConnections connections = new OpenedDbConnections();
		try
		{
			Assert.assertFalse(connections.getConnection(CON2).isClosed());
		}
		finally
		{
			connections.clear();
		}
	}

	@Test
	public void testCloseConnections() throws SQLException, ConnectivityException, SettingsException
	{
		GlobalContext context = createGlobalContext(ADMIN);
		try
		{
			Connection globalContextConn = context.getDbConnection(CON2);
			Assert.assertFalse(globalContextConn.isClosed());

			context.clearContext();
			Assert.assertTrue(globalContextConn.isClosed());
		}
		finally
		{
			context.clearContext();
		}

	}

	@Test(expectedExceptions = ConnectivityException.class)
	public void testUnexpectedConnectionName() throws ConnectivityException, SettingsException
	{
		GlobalContext context = createGlobalContext(ADMIN);
		try
		{
			context.getDbConnection(CON1);
		}
		finally
		{
			context.clearContext();
		}
	}

	private GlobalContext createGlobalContext(String user)
	{
		return TestActionUtils.createGlobalContext(user);
	}

	private static void initStorage() throws ConnectivityException, SettingsException
	{
		ClearThConnectionStorage storage = ClearThCore.connectionStorage();
		storage.addConnection(createCon(storage, CON2));
	}

	private static DbConnection createCon(ClearThConnectionStorage storage, String conName)
		throws SettingsException, ConnectivityException
	{
		DbConnection connection = (DbConnection) storage.createConnection(TYPE);
		connection.setName(conName);
		connection.getSettings().setJdbcUrl("jdbc:sqlite:" + DB_FILE);
		connection.getSettings().setInitializationQuery("select 1");
		connection.check();

		return connection;
	}

}