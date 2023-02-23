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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.connections.storage.DefaultClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.ibmmq.IbmMqConnection;
import com.exactprosystems.clearth.connectivity.ibmmq.IbmMqConnectionSettings;
import com.exactprosystems.clearth.data.DefaultDataHandlersFactory;
import com.exactprosystems.clearth.utils.SettingsException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.*;

public abstract class BasicIbmMqTest
{
	protected static final String IBM_MQ_CON_NAME = "MQConn";
	protected ConnectionTypeInfo ibmTypeInfo;
	protected ClearThConnectionStorage connectionStorage;
	protected Path testDirPath;

	@BeforeClass
	public void beforeClass() throws Exception
	{
		testDirPath = createTestDirPath();
		ibmTypeInfo = createIbmTypeInfo();
		connectionStorage = createStorageWithRunningConnection();
		mockApplication();
	}
	
	@AfterClass
	public void afterClass() throws NoSuchFieldException, IllegalAccessException
	{
		setStaticField(ClearThCore.class, "instance", null);
	}
	
	protected void mockApplication() throws NoSuchFieldException, IllegalAccessException
	{
		ClearThCore application = mock(ClearThCore.class, CALLS_REAL_METHODS);
		when(application.getConnectionStorage()).thenReturn(connectionStorage); // needed for rules
		setStaticField(ClearThCore.class, "instance", application);
	}

	private <T> void setStaticField(Class<T> clazz, String fieldName, T fieldValue)
			throws NoSuchFieldException, IllegalAccessException
	{
		Field field = clazz.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(clazz, fieldValue);
	}

	protected ClearThConnectionStorage createStorageWithRunningConnection()
			throws ConnectivityException, SettingsException
	{
		IbmMqConnection connection = spy(createConnection(IBM_MQ_CON_NAME, "1.2.3.4",
				1234, "MI_MANAGER", "MI_QUEUE", true));
		when(connection.isRunning()).thenReturn(true);
		
		ClearThConnectionStorage storage = new DefaultClearThConnectionStorage(new DefaultDataHandlersFactory());
		storage.registerType(ibmTypeInfo);
		storage.addConnection(connection);
		return storage;
	}

	protected ConnectionTypeInfo createIbmTypeInfo()
	{
		return new ConnectionTypeInfo("IBM", IbmMqConnection.class, testDirPath);
	}

	protected Path createTestDirPath()
	{
		return Paths.get("testOutput").resolve(getClass().getSimpleName());
	}
	
	protected IbmMqConnection createConnection(String connectionName,
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
		connection.setTypeInfo(ibmTypeInfo);
		return connection;
	}
}
