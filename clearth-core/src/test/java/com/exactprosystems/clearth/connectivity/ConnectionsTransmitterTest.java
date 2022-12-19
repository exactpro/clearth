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
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ConnectionsTransmitterTest
{
	private static final String type = "Dummy", fileName = "file.xml";
	private static final Path tempPath = Paths.get("testOutput", "exportConnections"),
							connectionPath = tempPath.resolve("ConnectionsXML");
	private static File tempDir, connDir;

	@BeforeClass
	public static void init() throws ClearThException, IOException
	{
		if (!Files.isDirectory(connectionPath))
			Files.createDirectories(connectionPath);

		connDir = connectionPath.toFile();
		tempDir = tempPath.toFile();
	}

	@After
	public void deleteTempFiles() throws IOException {
		FileUtils.cleanDirectory(tempDir);
	}

	@Test
	public void testExportConnectionsFromConnectionStorage() throws ConnectivityException, IOException, SettingsException
	{
		ConnectionsTransmitter transmitter = loadConnectionAndGetTransmitter();
		File resultZipFile = transmitter.exportConnections(type);
		Assert.assertTrue(resultZipFile.isFile());
	}

	@Test
	public void testExportConnectionsToZipFile() throws ConnectivityException, IOException, SettingsException
	{
		ConnectionsTransmitter transmitter = loadConnectionAndGetTransmitter();

		File trashFile = new File(connDir, fileName);
		trashFile.createNewFile();

		File resultZipFile = transmitter.exportConnections(type);
		List<File> unzipFiles = FileOperationUtils.unzipFile(resultZipFile, tempDir);

		for (File file: unzipFiles)
			Assert.assertNotEquals(fileName, file.getName());

		Assert.assertEquals(1,unzipFiles.size());
	}

	private ConnectionsTransmitter loadConnectionAndGetTransmitter()
			throws ConnectivityException, SettingsException
	{
		ConnectionTypeInfo info =  new ConnectionTypeInfo(type, DummyMessageConnection.class, connectionPath);
		ClearThConnectionStorage connectionStorage = new DefaultClearThConnectionStorage(new DefaultDataHandlersFactory());
		connectionStorage.registerType(info);

		ClearThConnection connection = connectionStorage.createConnection(info.getName());
		connection.setName("DummyConnection");

		connectionStorage.addConnection(connection);

		return new ConnectionsTransmitter(tempDir, connectionStorage);
	}
}