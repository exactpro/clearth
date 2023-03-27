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

package com.exactprosystems.clearth.utils.inputparams;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.connectivity.dummy.DummyMessageConnection;
import com.exactprosystems.clearth.connectivity.dummy.DummyRunnableConnection;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class InputParametersClassesTest
{
	protected static final Path testOutput = Paths.get("testOutput").resolve("ParametersUtils");
	protected static final String RES = "ParamsUtils/clearth.cfg";
	protected static final String DB = "DB", DB_CON = "dbCon", MESS_CON = "messageCon",
						RUNNABLE_CON = "runnableCon", INCORR_CON = "incorrectCon";
	protected ApplicationManager manager;
	protected ClearThConnectionStorage storage;

	@BeforeClass
	public void init() throws Exception
	{
		manager = new ApplicationManager(FileOperationUtils.resourceToAbsoluteFilePath(RES));
		storage = ClearThCore.connectionStorage();
	}

	@AfterClass
	public void dispose() throws Exception
	{
		if (manager != null)
			manager.dispose();
	}

	protected Path getConnectionsPath(String testOutputPath)
	{
		return StringUtils.isBlank(testOutputPath) ? testOutput : testOutput.resolve(testOutputPath);
	}
	
	protected void initConnections(Path testPath) throws ConnectivityException, SettingsException
	{
		storage.registerType(new ConnectionTypeInfo(DB, DbConnection.class, testPath.resolve("db")));
		DbConnection dbConnection = (DbConnection) storage.createConnection(DB);
		dbConnection.setName(DB_CON);
		dbConnection.getSettings().setJdbcUrl("jdbc:sqlite:DB_FILE");

		String dummyMess = DummyMessageConnection.TYPE;
		storage.registerType(new ConnectionTypeInfo(dummyMess, DummyMessageConnection.class, testPath.resolve(dummyMess)));
		DummyMessageConnection dummyMessageCon = (DummyMessageConnection) storage.createConnection(dummyMess);
		dummyMessageCon.setName(MESS_CON);

		String dummyRun = DummyRunnableConnection.TYPE;
		storage.registerType(new ConnectionTypeInfo(dummyRun, DummyRunnableConnection.class, testPath.resolve(dummyRun)));
		DummyRunnableConnection dummyRunnableCon = (DummyRunnableConnection) storage.createConnection(dummyRun);
		dummyRunnableCon.setName(RUNNABLE_CON);

		storage.addConnection(dbConnection);
		storage.addConnection(dummyMessageCon);
		storage.addConnection(dummyRunnableCon);
	}

}