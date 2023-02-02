/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.validation;

import com.exactprosystems.clearth.BasicTestNgTest;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.connections.storage.DefaultClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.ibmmq.IbmMqConnection;
import com.exactprosystems.clearth.connectivity.ibmmq.IbmMqConnectionSettings;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MQReadQNotReadByOthersRuleTest extends BasicTestNgTest
{
	private IbmMqReadQNotReadByOthersRule rule = new IbmMqReadQNotReadByOthersRule();
	private static ConnectionTypeInfo conType = new ConnectionTypeInfo("IbmMqTestCon",
			IbmMqConnection.class,
			Paths.get("testOutput").resolve(MQReadQNotReadByOthersRuleTest.class.getSimpleName()));

	@DataProvider(name = "invalidData")
	public Object[][] createInvalidData() {
		return new Object[][]
				{
						{
								createConnection("MQConnToCheck",
								                 "10.64.17.130",
								                 1234,
								                 "MI_MANAGER",
								                 "MI_QUEUE",
								                 true),
								"Can't start connection 'MQConnToCheck' that reads the same queue " +
										"as 'MQConn' (receiveQueue = 'MI_QUEUE')."
						}
				};
	}

	@DataProvider(name = "validData")
	public Object[][] createValidData() {
		return new Object[][]
				{
						{
								createConnection("MQConnToCheck",
								                 "10.64.17.130",
								                 1234,
								                 "MI_MANAGER",
								                 "SOME_QUEUE",
								                 true)
						},
						{
								createConnection("MQConnToCheck",
								                 "10.64.17.130",
								                 1234,
								                 "SOME_MANAGER",
								                 "MI_QUEUE",
								                 true)
						},
						{
								createConnection("MQConnToCheck",
								                 "10.64.17.131",
								                 1234,
								                 "MI_MANAGER",
								                 "MI_QUEUE",
								                 true)
						},
						{
								createConnection("MQConnToCheck",
								                 "10.64.17.130",
								                 12345,
								                 "MI_MANAGER",
								                 "MI_QUEUE",
								                 true)
						},
						{
								createConnection("MQConnToCheck",
								                 "10.64.17.130",
								                 1234,
								                 "MI_MANAGER",
								                 "MI_QUEUE",
								                 false)
						}
				};
	}

	@Test(dataProvider = "invalidData")
	public void checkConnectionWithConflicts(IbmMqConnection connection, String expectedErrorMessage)
	{
		assertTrue(rule.isConnectionSuitable(connection));
		assertEquals(expectedErrorMessage, rule.check(connection));
	}

	@Test(dataProvider = "validData")
	public void checkValidConnection(IbmMqConnection connection)
	{
		assertTrue(rule.isConnectionSuitable(connection));
		assertNull(rule.check(connection));
	}

	@Override
	protected void mockOtherApplicationFields(ClearThCore application)
	{
		mockRunningConnections(application);
	}

	private void mockRunningConnections(ClearThCore application)
	{
		IbmMqConnection anotherConnection = createConnection("MQConn", "10.64.17.130",
				1234, "MI_MANAGER", "MI_QUEUE", true);
		
		List<ClearThMessageConnection> anotherConnections = Collections.singletonList(anotherConnection);
		
		DefaultClearThConnectionStorage storage = mock(DefaultClearThConnectionStorage.class);
		//noinspection unchecked
		when(storage.getConnections(anyString(), any(Predicate.class), eq(ClearThMessageConnection.class)))
				.thenReturn(anotherConnections);

		when(application.getConnectionStorage()).thenReturn(storage);
	}

	private IbmMqConnection createConnection(String connectionName,
	                                      String hostname,
	                                      int port,
	                                      String queueManager,
	                                      String receiveQueue,
	                                      boolean useReceiveQueue)
	{
		IbmMqConnection connection = new IbmMqConnection();
		connection.setName(connectionName);
		IbmMqConnectionSettings connectionSettings = connection.getSettings();
		connectionSettings.setHostname(hostname);
		connectionSettings.setPort(port);
		connectionSettings.setQueueManager(queueManager);
		connectionSettings.setReceiveQueue(receiveQueue);
		connectionSettings.setUseReceiveQueue(useReceiveQueue);
		connection.setTypeInfo(conType);
		return connection;
	}
}
