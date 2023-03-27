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
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
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

public class ParametersHandlerTest extends InputParametersClassesTest
{
	private static final String CONN_DIR = ParametersHandlerTest.class.getSimpleName();
	private static final String PARAM_DB_CON = "DB", PARAM_MESS_CON = "message",
					PARAM_RUNNABLE_CON = "runnable", PARAM_INCORR_CON = "ConName";

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
	public void testGetRequiredCthConnections()
	{
		InputParamsHandler handler = new InputParamsHandler(map(PARAM_DB_CON, DB_CON, PARAM_MESS_CON, MESS_CON, PARAM_RUNNABLE_CON,
				RUNNABLE_CON));
		DbConnection dbConnection = handler.getRequiredDbConnection(PARAM_DB_CON);
		DummyMessageConnection msgConnection =
				(DummyMessageConnection) handler.getRequiredClearThMessageConnection(PARAM_MESS_CON);
		DummyRunnableConnection rnConnection =
				(DummyRunnableConnection) handler.getRequiredClearThRunnableConnection(PARAM_RUNNABLE_CON);
		checkSuccess(handler);
		
		Assert.assertEquals(dbConnection.getName(), DB_CON);
		Assert.assertEquals(msgConnection.getName(), MESS_CON);
		Assert.assertEquals(rnConnection.getName(), RUNNABLE_CON);
	}
	
	@Test
	public void testGetCthConnections()
	{
		InputParamsHandler handler = new InputParamsHandler(map(PARAM_INCORR_CON, INCORR_CON));
		DbConnection dbConnection = handler.getDbConnection(PARAM_DB_CON, DB_CON);
		DummyMessageConnection msgConnection =
				(DummyMessageConnection) handler.getClearThMessageConnection(PARAM_MESS_CON, MESS_CON);
		DummyRunnableConnection rnConnection =
				(DummyRunnableConnection) handler.getClearThRunnableConnection(PARAM_RUNNABLE_CON, RUNNABLE_CON);
		checkSuccess(handler);
		
		Assert.assertEquals(dbConnection.getName(), DB_CON);
		Assert.assertEquals(msgConnection.getName(), MESS_CON);
		Assert.assertEquals(rnConnection.getName(), RUNNABLE_CON);
	}
	
	@Test
	public void testGetDbConnectionsWithErrors()
	{
		InputParamsHandler handler = new InputParamsHandler(map(PARAM_DB_CON, DB_CON, PARAM_INCORR_CON, INCORR_CON));
		DbConnection dbConnection = handler.getRequiredDbConnection(PARAM_INCORR_CON);
		DummyMessageConnection msgConn = (DummyMessageConnection) handler.getRequiredClearThMessageConnection(PARAM_DB_CON);
		DummyRunnableConnection rnConn = (DummyRunnableConnection) handler.getRequiredClearThRunnableConnection(PARAM_DB_CON);
		
		Assert.assertNull(msgConn);
		Assert.assertNull(rnConn);
		
		checkFailed(handler, "Error\\(s\\) while getting connection\\(s\\): " +
				String.format("'%s' %s", INCORR_CON, "\\(does not exist\\)") + ", " +
				String.format("'%s' %s", DB_CON, "\\(does not support messages\\)") + ", " +
				String.format("'%s' %s", DB_CON, "\\(is not runnable\\)"));
	}
	
	@Test
	public void testGetConnectionWithUnexpectedType()
	{
		InputParamsHandler handler = new InputParamsHandler(map(PARAM_DB_CON, DB_CON));
		ClearThConnection connection = handler.getRequiredClearThConnection(PARAM_DB_CON, DummyMessageConnection.TYPE);
		
		Assert.assertNull(connection);
		checkFailed(handler,"Error\\(s\\) while getting connection\\(s\\): " +
				String.format("'%s' %s", DB_CON, "\\(has unexpected type DB while expected type "
						+ DummyMessageConnection.TYPE + "\\)"));
	}
	
	protected void checkSuccess(InputParamsHandler handler) throws ResultException
	{
		handler.check();
	}

	protected void checkFailed(InputParamsHandler handler, String messagePattern)
	{
		ResultException resultException = Assert.expectThrows(ResultException.class, handler::check);
		String msg = resultException.getMessage();
		Assert.assertNotNull(msg, "check() method is thrown exception with null message");
		Assert.assertTrue(msg.matches(messagePattern), "check() method is finished with unexpected message: " + msg);
	}

}