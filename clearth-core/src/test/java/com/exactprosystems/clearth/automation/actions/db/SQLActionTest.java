/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.actions.db;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static com.exactprosystems.clearth.ApplicationManager.waitForSchedulerToStop;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

public class SQLActionTest
{
	private static ApplicationManager appManager;
	private static final Path DB_DIR = Paths.get("testOutput").resolve(SQLActionTest.class.getSimpleName());
	private static File matrices, config, cfgFile;
	private static Path RES_DIR;
	private static final String MATRIX_DIR = "SQLActionTest/matrices",
							SCHEDULER_CFG = "SQLActionTest/config/config.cfg", CTH_CFG = "clearth.cfg",
							DB_FILE = DB_DIR.resolve("file.db").toString(), TYPE = "DB",
							SCHEDULER_NAME = "scheduler1";

	@BeforeClass
	public static void init() throws ClearThException, IOException, SQLException, SettingsException
	{
		FileUtils.deleteDirectory(DB_DIR.toFile());
		Files.createDirectories(DB_DIR);

		RES_DIR = Paths.get(resourceToAbsoluteFilePath("Action")).resolve("dbActions");

		matrices = RES_DIR.resolve(MATRIX_DIR).toFile();
		config = RES_DIR.resolve(SCHEDULER_CFG).toFile();
		cfgFile = RES_DIR.resolve(CTH_CFG).toFile();

		appManager = ApplicationManager.builder().configFilePath(cfgFile.toString()).build();
		prepareToTest();
	}

	@AfterClass
	public static void dispose() throws IOException, AutomationException
	{
		SchedulersManager sm = ClearThCore.getInstance().getSchedulersManager();
		Scheduler scheduler = sm.getSchedulerByName(SCHEDULER_NAME, SCHEDULER_NAME);
		scheduler.clearSteps();
		if (appManager != null)
			appManager.dispose();
	}

	@Test
	public void testExecuteQuery() throws ClearThException, AutomationException
	{
		Scheduler scheduler = appManager.getScheduler(SCHEDULER_NAME, SCHEDULER_NAME);
		appManager.loadSteps(scheduler,config);
		appManager.loadMatrices(scheduler, matrices);

		scheduler.start("admin");
		waitForSchedulerToStop(scheduler, 1000, 10000);

		Assert.assertTrue(scheduler.isSuccessful());
	}

	private static void prepareToTest() throws SettingsException, ConnectivityException, SQLException
	{
		String conName = "con1", table = "testTable";

		ClearThConnectionStorage connectionStorage = ClearThCore.connectionStorage();
		DbConnection connection = (DbConnection) connectionStorage.createConnection(TYPE);
		connection.setName(conName);
		connection.getSettings().setJdbcUrl("jdbc:sqlite:" + DB_FILE);
		connection.getSettings().setInitializationQuery("select 1");
		connection.check();

		connectionStorage.addConnection(connection);
		
		String[] queries = {"create table " + table + " (id INTEGER PRIMARY KEY, param1 INTEGER, param2 INTEGER)",
				"insert into " + table + " (id, param1, param2) values (1, 123, 123)",
				"create table empty_table (id INTEGER PRIMARY KEY)"};
		try (Connection con = connection.getConnection())
		{
			for (String q : queries)
			{
				try (PreparedStatement pstmt = con.prepareStatement(q))
				{
					pstmt.execute();
				}
			}
		}
	}
}