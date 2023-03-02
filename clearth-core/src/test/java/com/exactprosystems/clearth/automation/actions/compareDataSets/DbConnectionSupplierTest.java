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

package com.exactprosystems.clearth.automation.actions.compareDataSets;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static com.exactprosystems.clearth.ApplicationManager.*;

public class DbConnectionSupplierTest
{
	private static ApplicationManager manager;
	private static final Path DB_DIR = Paths.get("testOutput").resolve(DbConnectionSupplierTest.class.getSimpleName()),
								RES_DIR = USER_DIR.resolve("src/test/resources/Action/CompareDataSetsDB");
	private static final String DB_FILE = DB_DIR.resolve("file.db").toString(), TYPE = "DB";
	private static final File SCHEDULER_STEP_CONFIG = RES_DIR.resolve("configs").resolve("config.cfg").toFile(),
								MATRIX_TEST_ACTION = RES_DIR.resolve("matrices").toFile();
	private static final File CFG_FILE = RES_DIR.resolve("clearth.cfg").toFile();
	private static String con1 = "con1", con2 = "con2";

	@BeforeClass
	public static void init() throws Exception
	{
		FileUtils.deleteDirectory(DB_DIR.toFile());
		Files.createDirectories(DB_DIR);

		if(!CFG_FILE.isFile())
			throw new FileNotFoundException("File '" + CFG_FILE.getName() + "' not found");

		manager = new ApplicationManager(CFG_FILE.toString());

		createStorage();
	}

	@AfterClass
	public static void dispose() throws IOException
	{
		manager.dispose();
	}

	@Test
	public void testCreateConnectionSupplier()
			throws ClearThException, AutomationException, SQLException, SettingsException
	{
		ClearThConnectionStorage storage = ClearThCore.connectionStorage();
		createTable("expectedTable", (DbConnection) storage.getConnection(con1));
		createTable("actualTable", (DbConnection) storage.getConnection(con2));

		Scheduler scheduler = manager.getScheduler(ADMIN, ADMIN);
		manager.loadSteps(scheduler, SCHEDULER_STEP_CONFIG);
		manager.loadMatrices(scheduler,MATRIX_TEST_ACTION);
		scheduler.start(ADMIN);

		waitForSchedulerToStop(scheduler, 1000,10000);
	}

	protected static void createStorage() throws ConnectivityException, SettingsException
	{
		ClearThConnectionStorage connectionStorage = ClearThCore.connectionStorage();

		connectionStorage.addConnection(createCon(connectionStorage, con1));
		connectionStorage.addConnection(createCon(connectionStorage, con2));
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

	private static void createTable(String tblName, DbConnection connection)
			throws ConnectivityException, SettingsException, SQLException
	{
		String createQuery = "create table " + tblName + " (id INTEGER PRIMARY KEY, param1 INTEGER, param2 INTEGER)",
				insertQuery = "insert into " + tblName + " (id, param1, param2) values (1, 123, 123)";

		try(PreparedStatement prStatement1 = connection.getConnection().prepareStatement(createQuery))
		{
			prStatement1.execute();
		}
		try(PreparedStatement prStatement2 = connection.getConnection().prepareStatement(insertQuery))
		{
			prStatement2.execute();
		}

	}

}
