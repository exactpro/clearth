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
import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

public class ListenersNotWritingToBusyFilesRuleTest extends BasicTestNgTest
{
	private static final Path CTH_ROOT_PATH = Paths.get("/tools/CTH");

	private ListenersNotWritingToBusyFilesRule rule = new ListenersNotWritingToBusyFilesRule();


	@DataProvider(name = "invalidData")
	public Object[][] createInvalidData()
	{
		return new Object[][]
				{
						{
								createConnection("MQConnToCheck",
								                 new ListenerConfiguration("CollectorListener",
								                                           "Collector",
								                                           "contentsFileName=MyFile.txt", true),
								                 new ListenerConfiguration("FileListener1",
								                                           "File",
								                                           "MyFile.txt", true),
								                 new ListenerConfiguration("FileListener2",
								                                           "File",
								                                           "MyFile2.txt", true),
								                 new ListenerConfiguration("FileListener3",
								                                           "File",
								                                           "MyFile.txt", true),
								                 new ListenerConfiguration("FileListener4",
								                                           "File",
								                                           "MyFile2.txt", true)
								),
								"Can't start connection 'MQConnToCheck' - \n"
										+ "Connections' listeners are writing the same files:\n"
										+ "Listeners 'CollectorListener', 'FileListener1', 'FileListener3' to file '/tools/CTH/MyFile.txt' "
										+ "which is already written to by connection's listener 'CollectorListener'('MQConn');\n"
										+ "Listeners 'FileListener2', 'FileListener4' to file '/tools/CTH/MyFile2.txt'."
						},
						{
								createConnection("MQConnToCheck",
								                 new ListenerConfiguration("CollectorListener",
								                                           "Collector",
								                                           "contentsFileName=MyFile1.txt", true),
								                 new ListenerConfiguration("FileListener",
								                                           "File",
								                                           CTH_ROOT_PATH.resolve("MyFile1.txt")
								                                                        .toString(), true)
								),
								"Can't start connection 'MQConnToCheck' - \n"
										+ "Connections' listeners are writing the same files:\n"
										+ "Listeners 'CollectorListener', 'FileListener' to file '/tools/CTH/MyFile1.txt'."
						},
						{
								createConnection("MQConnToCheck",
								                 new ListenerConfiguration("CollectorListener",
								                                           "Collector",
								                                           "contentsFileName=MyFile1.txt", true),
								                 new ListenerConfiguration("FileListener",
								                                           "File",
								                                           CTH_ROOT_PATH.resolve("..")
								                                                        .resolve("CTH")
								                                                        .resolve("MyFile.txt")
								                                                        .toString(), true)
								),
								"Can't start connection 'MQConnToCheck' - \n"
										+ "Connections' listeners are writing the same files:\n"
										+ "Listeners 'FileListener' to file '/tools/CTH/MyFile.txt' "
										+ "which is already written to by connection's listener 'CollectorListener'('MQConn')."
						}
				};
	}
	

	@DataProvider(name = "validData")
	public Object[][] createValidData()
	{
		return new Object[][]
				{

						{
								createConnection("MQConnToCheck",
										new ListenerConfiguration("CollectorListener",
												"Collector",
												"contentsFileName=MyFile1.txt", true),
										new ListenerConfiguration("FileListener",
												"File",
												"MyFile2.txt", true))
						},
						{
								createConnection("MQConnToCheck",
										new ListenerConfiguration("CollectorListener",
												"Collector",
												"fileName=MyFile.txt", true))
						}
				};
	}

	@Test(dataProvider = "invalidData")
	public void checkConnectionWithConflicts(ClearThMessageConnection connection, String expectedErrorMessage)
	{
		assertTrue(rule.isConnectionSuitable(connection));
		assertEquals(rule.check(connection), expectedErrorMessage);
	}

	@Test(dataProvider = "validData")
	public void checkValidConnection(ClearThMessageConnection connection)
	{
		assertTrue(rule.isConnectionSuitable(connection));
		assertNull(rule.check(connection));
	}


	@Override
	protected void mockOtherApplicationFields(ClearThCore application)
	{
		mockRunningConnections(application);
		mockRootRelative(application);
	}
	
	private void mockRunningConnections(ClearThCore application)
	{
		MQConnection anotherConnection = createConnection("MQConn",
		                                                  new ListenerConfiguration("CollectorListener",
		                                                                            "Collector",
		                                                                            "contentsFileName=MyFile.txt", true));
		List<ClearThMessageConnection> anotherConnections = singletonList(anotherConnection);

		ClearThConnectionStorage storage = mock(ClearThConnectionStorage.class);
		//noinspection unchecked
		when(storage.getConnections(any(Predicate.class), eq(ClearThMessageConnection.class)))
				.thenReturn(anotherConnections);

		when(application.getConnectionStorage()).thenReturn(storage);
	}
	
	private void mockRootRelative(ClearThCore application)
	{
		when(application.getRootRelative(anyString()))
				.thenAnswer(i -> fakeRootRelative(String.valueOf(i.getArguments()[0])));
	}
	
	private String fakeRootRelative(String fileName)
	{
		Path path = Paths.get(fileName);
		if (path.isAbsolute())
			return fileName;
		else
			return CTH_ROOT_PATH.resolve(path).toString();
	}

	
	private MQConnection createConnection(String connectionName, ListenerConfiguration... listenerConfigurations)
	{
		MQConnection connection = new DefaultMQConnection();
		connection.setName(connectionName);
		connection.setListeners(Arrays.asList(listenerConfigurations));
		return connection;
	}
}
