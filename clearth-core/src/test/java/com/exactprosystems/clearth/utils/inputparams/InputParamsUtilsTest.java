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

package com.exactprosystems.clearth.utils.inputparams;

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.connectivity.dummy.DummyMessageConnection;
import com.exactprosystems.clearth.connectivity.dummy.DummyRunnableConnection;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;

public class InputParamsUtilsTest extends InputParametersClassesTest
{
	public static final String PARAM_NAME = "ConnectionName", INCORRECT_PARAM = "ConnName",
								CONN_DIR = "InputParamsUtilsTest";
	
	@Override
	@BeforeClass
	public void init() throws Exception
	{
		Path conTestDir = getConnectionsPath(CONN_DIR);
		FileUtils.deleteDirectory(conTestDir.toFile());
		Files.createDirectories(conTestDir);
		super.init();
		initConnections(conTestDir);
	}
	
	@Test
	public void testGetDBConnection()
	{
		DbConnection dbConnection = InputParamsUtils.getDbConnection(map(PARAM_NAME, DB_CON), INCORRECT_PARAM, DB_CON);
		DbConnection requiredDbConnection = InputParamsUtils.getRequiredDbConnection(map(PARAM_NAME, DB_CON), PARAM_NAME);
		Assert.assertEquals(dbConnection.getName(), DB_CON);
		Assert.assertEquals(requiredDbConnection.getName(), DB_CON);
	}

	@Test
	public void testGetMessageConnection()
	{
		DummyMessageConnection messageConnection = (DummyMessageConnection) InputParamsUtils
				.getClearThMessageConnection(map(PARAM_NAME, MESS_CON), INCORRECT_PARAM, MESS_CON, DummyMessageConnection.TYPE);
		DummyMessageConnection requiredMessageConnection = (DummyMessageConnection) InputParamsUtils
				.getRequiredClearThMessageConnection(map(PARAM_NAME, MESS_CON), PARAM_NAME, DummyMessageConnection.TYPE);

		Assert.assertEquals(messageConnection.getName(), MESS_CON);
		Assert.assertEquals(requiredMessageConnection.getName(), MESS_CON);
	}

	@Test
	public void testGetRunnableConnection()
	{
		DummyRunnableConnection runnableConnection = (DummyRunnableConnection) InputParamsUtils
				.getClearThRunnableConnection(map(PARAM_NAME, RUNNABLE_CON), INCORRECT_PARAM, RUNNABLE_CON, DummyRunnableConnection.TYPE);
		DummyRunnableConnection requiredRunnableConnection = (DummyRunnableConnection) InputParamsUtils
				.getRequiredClearThRunnableConnection(map(PARAM_NAME, RUNNABLE_CON), PARAM_NAME, DummyRunnableConnection.TYPE);

		Assert.assertEquals(runnableConnection.getName(), RUNNABLE_CON);
		Assert.assertEquals(requiredRunnableConnection.getName(), RUNNABLE_CON);
	}

	@Test(expectedExceptions = ResultException.class,
		expectedExceptionsMessageRegExp = "Connection with name '" + INCORR_CON + "' does not exist")
	public void testGetDBConnectionWithException()
	{
		InputParamsUtils.getRequiredDbConnection(map(PARAM_NAME, INCORR_CON), PARAM_NAME);
	}

	@Test(expectedExceptions = ResultException.class,
		expectedExceptionsMessageRegExp = "Connection '" + RUNNABLE_CON + "' does not support messages")
	public void testGetMessageConnectionWithException()
	{
		InputParamsUtils.getRequiredClearThMessageConnection(map(PARAM_NAME, RUNNABLE_CON), PARAM_NAME);
	}

	@Test(expectedExceptions = ResultException.class,
		expectedExceptionsMessageRegExp = "Connection '" + DB_CON + "' is not runnable")
	public void testGetRunnableConnectionWithException()
	{
		InputParamsUtils.getRequiredClearThRunnableConnection(map(PARAM_NAME, DB_CON), PARAM_NAME);
	}
	
	
	@Test(expectedExceptions = ResultException.class,
			expectedExceptionsMessageRegExp = "Connection '" + MESS_CON + "' has type " +
					DummyMessageConnection.TYPE + " while expected type " + DummyRunnableConnection.TYPE)
	public void testGetConnectionWithUnexpectedType()
	{
		InputParamsUtils.getClearThConnection(map(PARAM_NAME, MESS_CON), PARAM_NAME, "", DummyRunnableConnection.TYPE);
	}
}