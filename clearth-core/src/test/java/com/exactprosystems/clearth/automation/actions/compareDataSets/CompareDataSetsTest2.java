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
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.sql.DbConnectionSupplier;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.ApplicationManager.*;
import static org.testng.Assert.assertTrue;

public class CompareDataSetsTest2
{
	private static ApplicationManager manager;
	private static final Path DB_DIR = Paths.get("testOutput").resolve(CompareDataSetsTest2.class.getSimpleName()),
					RES_DIR = USER_DIR.resolve("src").resolve("test").resolve("resources")
								.resolve("Action").resolve("CompareDataSetsDB");
	private static final String DB_FILE = DB_DIR.resolve("file.db").toString(),
					TYPE = "DB", CON1 = "con1", CON2 = "con2";
	private static final File SCHEDULER_STEP_CONFIG = RES_DIR.resolve("configs").resolve("config.cfg").toFile(),
					MATRIX_TEST_ACTION = RES_DIR.resolve("matrices").toFile(),
					CFG_FILE = RES_DIR.resolve("clearth.cfg").toFile();

	@BeforeClass
	public static void init() throws IOException, ClearThException, SettingsException, SQLException
	{
		FileUtils.deleteDirectory(DB_DIR.toFile());
		Files.createDirectories(DB_DIR);

		if(!CFG_FILE.isFile())
			throw new FileNotFoundException("File '" + CFG_FILE.getName() + "' not found");

		manager = new ApplicationManager(CFG_FILE.toString());

		ClearThConnectionStorage storage = createStorage();

		createTable("expectedTbl", (DbConnection) storage.getConnection(CON1));
		createTable("actualTbl", (DbConnection) storage.getConnection(CON2));
	}

	@AfterClass
	public static void dispose() throws IOException
	{
		if (manager != null)
			manager.dispose();
	}

	@AfterTest
	public void clearScheduler()
	{
		SchedulersManager manager = ClearThCore.getInstance().getSchedulersManager();
		List<Scheduler> userSchedulers = manager.getUserSchedulers(ADMIN);
		userSchedulers.clear();
	}

	@Test
	public void testStartAction() throws ClearThException, AutomationException
	{
		Scheduler scheduler = createScheduler(SCHEDULER_STEP_CONFIG, MATRIX_TEST_ACTION);
		scheduler.start(ADMIN);

		waitForSchedulerToStop(scheduler, 1000,10000);
		assertTrue(scheduler.isSuccessful());
	}

	@DataProvider(name = "needCloseDbConnection")
	public static Object[][] needCloseDbConnection()
	{
		return new Object[][]
			{
				{false, "Select * from expectedTbl", "Select * from actualTbl"},
				{true, "Select * from expectedTbl", "Select * from actualTbl"},
				{false, "Select * from expectedTbl123", "Select * from actualTbl456"},
				{true, "Select * from expectedTbl123", "Select * from actualTbl456"}
			};
	}

	@Test(dataProvider = "needCloseDbConnection")
	public void testRunCompareDataSets(boolean needCloseDbConnection, String selectQueryExp, String selectQueryActual)
			throws FailoverException, SQLException, ConnectivityException, SettingsException
	{
		GlobalContext globalContext = TestActionUtils.createGlobalContext(ADMIN);
		DbConnectionSupplier supplier = globalContext::getDbConnection;
		try
		{
			Connection connection = globalContext.getDbConnection(CON1);
			Assert.assertFalse(connection.isClosed());
			Action action = new CompareDataSets_DB(needCloseDbConnection, supplier);
			ActionSettings actionSettings = new ActionSettings();
			actionSettings.setParams(createInputParams(selectQueryExp, selectQueryActual));
			action.init(actionSettings);
			action.execute(new StepContext("Step1", new Date()),
					new MatrixContext(), globalContext);
			
			if (!needCloseDbConnection)
				Assert.assertFalse(connection.isClosed());
			else
				Assert.assertTrue(connection.isClosed());
		}
		finally
		{
			globalContext.clearContext();
		}
	}

	private Map<String, String> createInputParams(String expQuery, String actQuery)
	{
		Map<String, String> inputParams = new HashMap<>();
		inputParams.put("ID", "id1");
		inputParams.put("Globalstep", "Step1");
		inputParams.put("Action", "CompareDataSets");
		inputParams.put("ExpectedFormat", "Query");
		inputParams.put("ExpectedSource", expQuery);
		inputParams.put("ActualFormat", "Query");
		inputParams.put("ActualSource", actQuery);
		inputParams.put("ExpectedConnectionName", CON1);
		inputParams.put("ActualConnectionName", CON2);
		return inputParams;
	}

	private static ClearThConnectionStorage createStorage() throws ConnectivityException, SettingsException
	{
		ClearThConnectionStorage storage = ClearThCore.connectionStorage();
		storage.addConnection(createCon(storage, CON1));
		storage.addConnection(createCon(storage, CON2));
		return  storage;
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

	private Scheduler createScheduler(File stepConfig, File matrixDir) throws ClearThException
	{
		Scheduler scheduler = manager.getScheduler(ADMIN, ADMIN);
		manager.loadSteps(scheduler, stepConfig);
		manager.loadMatrices(scheduler, matrixDir);
		return scheduler;
	}
}