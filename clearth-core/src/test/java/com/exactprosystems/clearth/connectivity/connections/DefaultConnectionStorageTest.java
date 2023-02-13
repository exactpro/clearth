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

package com.exactprosystems.clearth.connectivity.connections;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.db.DbConnection;
import com.exactprosystems.clearth.connectivity.connections.exceptions.ClassSetupException;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.connections.storage.DefaultClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.dummy.DummyMessageConnection;
import com.exactprosystems.clearth.connectivity.dummy.DummyPlainConnection;
import com.exactprosystems.clearth.connectivity.dummy.DummyRunnableConnection;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class DefaultConnectionStorageTest
{
	private static ApplicationManager applicationManager;
	private static DefaultClearThConnectionStorage storage;
	private static Path connectionDir;
	private static ConnectionTypeInfo dummyTypeInfo;
	private static ConnectionTypeInfo dbTypeInfo;

	@BeforeClass
	public static void beforeClass() throws ClearThException, SettingsException
	{
		applicationManager = new ApplicationManager();
		connectionDir = Paths.get("testOutput")
				.resolve(DefaultConnectionStorageTest.class.getSimpleName())
				.resolve("connections");
		prepareDirectory(connectionDir);
		storage = new DefaultClearThConnectionStorage();
		dummyTypeInfo = createDummyMessageTypeInfo();
		storage.registerType(dummyTypeInfo);
		dbTypeInfo = createDbTypeInfo();
		
		// ensure storage is working good; if this fails, then this class cannot be tested for other operations
		storage.createConnection(dummyTypeInfo.getName());
	}
	
	@AfterClass
	public static void afterClass() throws IOException
	{
		if (applicationManager != null)
			applicationManager.dispose();
	}

	private static void prepareDirectory(Path connectionDir)
	{
		connectionDir.toFile().mkdirs();
		try
		{
			FileUtils.cleanDirectory(connectionDir.toFile());
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error occurred during preparing directory for tests: " + connectionDir.toAbsolutePath(), e);
		}
	}

	private static ConnectionTypeInfo createDummyPlainTypeInfo()
	{
		return new ConnectionTypeInfo(
				DummyPlainConnection.TYPE,
				DummyPlainConnection.class,
				connectionDir.resolve("plain"));
	}
	
	private static ConnectionTypeInfo createDummyRunnableTypeInfo()
	{
		return new ConnectionTypeInfo(
				DummyRunnableConnection.TYPE,
				DummyRunnableConnection.class,
				connectionDir.resolve("runnable"));
	}
	
	private static ConnectionTypeInfo createDummyMessageTypeInfo()
	{
		return new ConnectionTypeInfo(
				DummyMessageConnection.TYPE,
				DummyMessageConnection.class,
				connectionDir);
	}

	private static ConnectionTypeInfo createDbTypeInfo()
	{
		return new ConnectionTypeInfo("DB",
				DbConnection.class,
				connectionDir.resolve("DB"));
	}

	private boolean hasFile(String conName)
	{
		return Files.exists(connectionDir.resolve(conName + ".xml"));
	}
	
	@Test(expected = SettingsException.class)
	public void testAddConnectionsWithSameName() throws ConnectivityException, SettingsException
	{
		String conName = "addConnectionWithSameNameTest";
		ClearThConnection con = storage.createConnection(dummyTypeInfo.getName());
		con.setName(conName);
		storage.addConnection(con);
		storage.addConnection(con.copy());
	}

	@Test
	public void testConnectionWithoutSettingsClassAnnotation()
	{
		assertThrows(ClassSetupException.class, () -> new BasicClearThConnection() {});
		assertThrows(ClassSetupException.class, () -> storage.registerType(new ConnectionTypeInfo("wrongType",
				BasicClearThConnection.class,
				connectionDir.resolve("shouldNotExist")
		)));
	}
	
	@Test(expected = SettingsException.class)
	public void testDifferentInfoTypesWithSameDir() throws SettingsException, ConnectivityException
	{
		ConnectionTypeInfo dbConType = new ConnectionTypeInfo("DB", 
				DbConnection.class, dummyTypeInfo.getDirectory());
		DefaultClearThConnectionStorage storageToBreak = new DefaultClearThConnectionStorage();
		storageToBreak.registerType(dummyTypeInfo);
		storageToBreak.registerType(dbConType);
	}
	
	@Test(expected = ConnectivityException.class)
	public void testTypeNotExists() throws ConnectivityException
	{
		storage.createConnection("NotExistType");
	}
	
	@Test(expected = SettingsException.class)
	public void testDoubleRegister() throws SettingsException, ConnectivityException
	{
		storage.registerType(dummyTypeInfo);
	}
	
	@Test
	public void testAddCustomCreatedConnectionType() throws ConnectivityException, SettingsException
	{
		String conName = "testAddCustomCreatedCon";

		DummyMessageConnection con = new DummyMessageConnection();
		con.setName(conName);
		ClearThConnectionStorage tmpStorage = new DefaultClearThConnectionStorage();
		tmpStorage.registerType(dummyTypeInfo);
		tmpStorage.registerType(dbTypeInfo);
		assertThrows(ConnectivityException.class, () -> tmpStorage.addConnection(con));

		// Setting type that is invalid for this connection class
		con.setTypeInfo(dbTypeInfo);
		assertThrows(ConnectivityException.class, () -> tmpStorage.addConnection(con));
		
		con.setTypeInfo(dummyTypeInfo);
		storage.addConnection(con);
	}

	@Test(expected = SettingsException.class)
	public void testAddConnectionWithoutName() throws ConnectivityException, SettingsException
	{
		ClearThConnection con = storage.createConnection(dummyTypeInfo.getName());
		storage.addConnection(con);
	}

	@Test
	public void testAddConnection() throws ConnectivityException, SettingsException
	{
		String conName = "addConTest";
		ClearThConnection con = storage.createConnection(dummyTypeInfo.getName());
		con.setName(conName);
		assertFalse(storage.containsConnection(conName));
		assertNull(storage.getConnection(conName));
		storage.addConnection(con);
		assertSame(con, storage.getConnection(conName));
		assertTrue(storage.containsConnection(conName));
		assertTrue(hasFile(conName));
	}
	
	@Test
	public void testRenameConnection() throws ConnectivityException, SettingsException
	{
		String conName = "renameConTest";
		String conAfterRenameName = "renameConTestModified";
		ClearThConnection con = storage.createConnection(dummyTypeInfo.getName());
		con.setName(conName);
		storage.addConnection(con);
		storage.renameConnection(con, conAfterRenameName);
		assertFalse(hasFile(conName));
		assertFalse(storage.containsConnection(conName));
		assertNull(storage.getConnection(conName));
		
		assertTrue(hasFile(conAfterRenameName));
		assertTrue(storage.containsConnection(conAfterRenameName));
		assertSame(con, storage.getConnection(conAfterRenameName));
	}
	
	@Test
	public void testModifyConnection() throws ConnectivityException, SettingsException
	{
		String conName = "modifyConTest";
		String conModifiedName = "modifyConTestModified";
		String testFieldValue = "Test Field Value";
		DummyMessageConnection con = (DummyMessageConnection) storage.createConnection(dummyTypeInfo.getName());
		con.setName(conName);
		storage.addConnection(con);
		
		DummyMessageConnection conCopy = (DummyMessageConnection) con.copy();
		conCopy.setName(conModifiedName);
		conCopy.getSettings().setTestField(testFieldValue);
		storage.modifyConnection(con, conCopy);
		
		con = (DummyMessageConnection) storage.getConnection(conName);
		assertNull(con);
		
		con = (DummyMessageConnection) storage.getConnection(conModifiedName);
		assertNotNull(con);
		assertEquals(testFieldValue, con.getSettings().getTestField());
		assertFalse(hasFile(conName));
		assertTrue(hasFile(conModifiedName));
	}
	
	@Test(expected = ConnectivityException.class)
	public void testModifyNotAddedConnection() throws ConnectivityException, SettingsException
	{
		String conName = "modifyConTest";
		String conModifiedName = "modifyConTestModified";
		String testFieldValue = "Test Field Value";
		DummyMessageConnection con = (DummyMessageConnection) storage.createConnection(dummyTypeInfo.getName());
		con.setName(conName);
		storage.addConnection(con);

		DummyMessageConnection conCopyNotInStorage = (DummyMessageConnection) con.copy();
		conCopyNotInStorage.setName(conModifiedName);
		conCopyNotInStorage.getSettings().setTestField(testFieldValue);
		storage.modifyConnection(conCopyNotInStorage, con);
	}
	
	@Test
	public void testRemoveConnection() throws ConnectivityException, SettingsException
	{
		String conName = "removeConTest";
		DummyMessageConnection con = (DummyMessageConnection) storage.createConnection(dummyTypeInfo.getName());
		con.setName(conName);
		storage.addConnection(con);
		storage.removeConnection(con);
		assertNull(storage.getConnection(conName));
		assertFalse(storage.containsConnection(conName));
		assertFalse(hasFile(conName));
		storage.removeConnection(con); // attempt to remove what is not contained
		
		storage.addConnection(con);
		con.start();
		storage.removeConnection(con);
		assertNull(storage.getConnection(conName));
		assertFalse(storage.containsConnection(conName));
		assertFalse(hasFile(conName));
		assertFalse(con.isRunning());
	}
	
	@Test
	public void testRemoveNonMessageConnection() throws ConnectivityException, SettingsException
	{
		ClearThConnectionStorage customStorage = new DefaultClearThConnectionStorage();
		customStorage.registerType(createDummyPlainTypeInfo());
		customStorage.registerType(createDummyRunnableTypeInfo());
		customStorage.registerType(dummyTypeInfo);
		
		ClearThConnection plainCon = createAndAddConnection(DummyPlainConnection.TYPE, "PlainCon1", customStorage),
				runnableCon = createAndAddConnection(DummyRunnableConnection.TYPE, "RunnableCon1", customStorage),
				msgCon = createAndAddConnection(DummyMessageConnection.TYPE, "MsgCon1", customStorage);
		
		customStorage.removeConnection(plainCon);
		customStorage.removeConnection(runnableCon);
		customStorage.removeConnection(msgCon);
		
		assertNull("Plain connection after removal", customStorage.getConnection(plainCon.getName()));
		assertNull("Runnable connection after removal", customStorage.getConnection(runnableCon.getName()));
		assertNull("Message connection after removal", customStorage.getConnection(msgCon.getName()));
	}
	
	@Test
	public void testLoadConnection() throws ConnectivityException, SettingsException
	{
		String conName = "loadConTest";
		String testFileValue = "LoadCon Test";
		DummyMessageConnection con = (DummyMessageConnection) storage.createConnection(dummyTypeInfo.getName());
		con.setName(conName);
		con.getSettings().setTestField(testFileValue);
		storage.addConnection(con);
		storage.reloadConnections();
		DummyMessageConnection conAfterReload = (DummyMessageConnection) storage.getConnection(conName);
		assertNotNull(conAfterReload);
		assertEquals(testFileValue, conAfterReload.getSettings().getTestField());
	}
	
	@Test
	public void testAutostartConnection() throws ConnectivityException, SettingsException
	{
		String conName = "autoStartTest";
		String conName2 = "autoStartTest2";
		String conWithoutAutostartName = "autostartTestNegative";

		DummyMessageConnection con = (DummyMessageConnection) storage.createConnection(dummyTypeInfo.getName());
		DummyMessageConnection conWithoutAutostart = (DummyMessageConnection) storage.createConnection(dummyTypeInfo.getName());
		con.setName(conName);
		conWithoutAutostart.setName(conWithoutAutostartName);
		conWithoutAutostart.setAutoconnect(false);
		storage.addConnection(con);
		storage.addConnection(conWithoutAutostart);

		assertFalse(con.isRunning());
		storage.autoStartConnections();
		assertTrue(con.isRunning());
		assertFalse(conWithoutAutostart.isRunning());
		
		DummyMessageConnection con2 = (DummyMessageConnection) storage.createConnection(dummyTypeInfo.getName());
		con2.setName(conName2);
		storage.addConnection(con2);
		storage.reloadConnections();
		
		assertFalse(con.isRunning());
		assertFalse(con2.isRunning());
		assertTrue(((DummyMessageConnection) storage.getConnection(conName)).isRunning());
		assertTrue(((DummyMessageConnection) storage.getConnection(conName2)).isRunning());
		assertFalse(((DummyMessageConnection) storage.getConnection(conWithoutAutostartName)).isRunning());
	}
	
	
	private ClearThConnection createAndAddConnection(String type, String name, ClearThConnectionStorage storage) throws ConnectivityException, SettingsException
	{
		ClearThConnection result = storage.createConnection(type);
		result.setName(name);
		storage.addConnection(result);
		return result;
	}
}