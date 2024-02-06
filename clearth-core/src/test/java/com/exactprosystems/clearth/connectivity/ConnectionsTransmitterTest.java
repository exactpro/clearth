/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.connections.storage.DefaultClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.dummy.DummyMessageConnection;
import com.exactprosystems.clearth.data.DefaultDataHandlersFactory;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class ConnectionsTransmitterTest
{
	private static final String TYPE = "Dummy", CLASS_NAME = ConnectionsTransmitterTest.class.getSimpleName();
	private static final Path TEMP_PATH = Paths.get("testOutput").resolve(CLASS_NAME);
	private static File tempDir;
	private static Path resDir;

	@BeforeClass
	public static void init() throws ClearThException, IOException
	{
		FileUtils.deleteDirectory(TEMP_PATH.toFile());
		Files.createDirectories(TEMP_PATH);

		tempDir = TEMP_PATH.toFile();
		resDir = Paths.get(FileOperationUtils.resourceToAbsoluteFilePath(CLASS_NAME));
	}

	@Test
	public void testExportConnectionsFromConnectionStorage() throws ConnectivityException, IOException, SettingsException
	{
		ConnectionsTransmitter transmitter = loadConnectionAndGetTransmitter(TEMP_PATH);
		File resultZipFile = transmitter.exportConnections(TYPE);
		Assert.assertTrue(resultZipFile.isFile());
	}

	@Test
	public void testExportConnectionsToZipFile() throws ConnectivityException, IOException, SettingsException
	{
		ConnectionsTransmitter transmitter = loadConnectionAndGetTransmitter(TEMP_PATH);
		String fileName = "testExportConnectionsToZipFile.xml";

		File trashFile = new File(tempDir, fileName);
		try
		{
			trashFile.createNewFile();
			File resultZipFile = transmitter.exportConnections(TYPE);
			List<File> unzipFiles = FileOperationUtils.unzipFile(resultZipFile, tempDir);

			for (File file : unzipFiles)
				assertNotEquals(fileName, file.getName());

			assertEquals(1, unzipFiles.size());
		}
		finally
		{
			Files.deleteIfExists(trashFile.toPath());
		}
	}

	@Test
	public void testUploadCopyOfAnExistingConnection() throws ConnectivityException, SettingsException, IOException
	{
		Path uploadCopyPath = TEMP_PATH.resolve("testUploadCopyOfAnExistingConnection");
		Files.createDirectories(uploadCopyPath);

		ConnectionsTransmitter transmitter = loadConnectionAndGetTransmitter(uploadCopyPath);
		File actualFile = uploadCopyPath.resolve("DummyConnection.xml").toFile();
		String actualContent = readFile(actualFile),
				expectedContent = readFile(resDir.resolve("DummyConnection.xml").toFile());
		assertNotEquals(actualContent, expectedContent);

		transmitter.deployConnections(TYPE, resDir.resolve("Dummy_connection_for_update.zip").toFile());
		String updatedFile = readFile(actualFile);
		assertEquals(updatedFile, expectedContent);
	}

	@Test(expectedExceptions = ConnectivityException.class)
	public void testUploadWrongZippedFilesToConnections() throws ConnectivityException, SettingsException, IOException
	{
		ConnectionsTransmitter transmitter = loadConnectionAndGetTransmitter(TEMP_PATH);
		transmitter.deployConnections(TYPE, resDir.resolve("DB_connection.zip").toFile());
	}

	@Test(expectedExceptions = ConnectivityException.class,
			expectedExceptionsMessageRegExp = "Could not load connection settings from file 'DBCon.xml'.")
	public void testLoadConnectionsFromDirWithUnexpectedFiles() throws ConnectivityException, SettingsException
	{
		ConnectionTypeInfo info =  new ConnectionTypeInfo(TYPE, DummyMessageConnection.class, resDir.resolve("unexpectedConnections"));
		ClearThConnectionStorage connectionStorage = new DefaultClearThConnectionStorage(new DefaultDataHandlersFactory());
		connectionStorage.registerType(info);
		connectionStorage.loadConnections();
	}

	private ConnectionsTransmitter loadConnectionAndGetTransmitter(Path path)
			throws ConnectivityException, SettingsException
	{
		ConnectionTypeInfo info =  new ConnectionTypeInfo(TYPE, DummyMessageConnection.class, path);
		ClearThConnectionStorage connectionStorage = new DefaultClearThConnectionStorage(new DefaultDataHandlersFactory());
		connectionStorage.registerType(info);

		ClearThConnection connection = connectionStorage.createConnection(info.getName());
		connection.setName("DummyConnection");

		connectionStorage.addConnection(connection);

		return new ConnectionsTransmitter(path.toFile(), connectionStorage);
	}
	
	private String readFile(File file) throws IOException
	{
		return FileUtils.readFileToString(file, Utils.UTF8)
				.replace("\r\n", "\n");
	}
}