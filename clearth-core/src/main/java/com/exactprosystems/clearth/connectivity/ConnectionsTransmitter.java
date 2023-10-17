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
import com.exactprosystems.clearth.connectivity.connections.ClearThRunnableConnection;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.connections.storage.ConnectionFileOperator;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConnectionsTransmitter
{
	private final File tempDir;
	private final ClearThConnectionStorage connectionStorage;

	public ConnectionsTransmitter(File tempDir, ClearThConnectionStorage connectionStorage){
		this.tempDir = tempDir;
		this.connectionStorage = connectionStorage;
	}

	protected final static Logger logger = LoggerFactory.getLogger(ConnectionsTransmitter.class);
	
	public File exportConnections(String type) throws IOException, ConnectivityException
	{
		String tempName = type + "_connections_";
		File tempTypePath = FileOperationUtils.createTempDirectory(tempName, tempDir);
		List<File> fileList = createConnectionFiles(type, tempTypePath);
		File resultFile = null;

		if(!fileList.isEmpty())
		{
			resultFile = File.createTempFile(tempName, ".zip", tempDir);
			FileOperationUtils.zipFiles(resultFile, fileList);

			logger.debug("Created ZIP file '{}' with {} connections to be exported", resultFile.getAbsolutePath(), type);
		}

		FileUtils.deleteDirectory(tempTypePath);
		return resultFile;
	}

	private List<File> createConnectionFiles(String type, File tempPath) throws ConnectivityException
	{
		List<ClearThConnection> connectionList = connectionStorage.getConnections(type);

		if(connectionList.isEmpty())
			return Collections.emptyList();

		List<File> fileList = new ArrayList<>();
		ConnectionFileOperator operator = new ConnectionFileOperator();
		for (ClearThConnection cthConn : connectionList)
		{
			String connName = cthConn.getName();
			File connectionFile = new File(tempPath, connName + ".xml");
			try
			{
				operator.save(cthConn, connectionFile);
				fileList.add(connectionFile);
			}
			catch (ConnectivityException e)
			{
				throw new ConnectivityException("Could not export connection '" + connName +"'", e);
			}
		}
		return fileList;
	}

	public synchronized void deployConnections(String type, File zipFile) throws IOException, ConnectivityException, SettingsException
	{
		File destDir = FileOperationUtils.createTempDirectory("imported_" + type + "_connections_", tempDir);
		try
		{
			FileOperationUtils.unzipFile(zipFile, destDir);
			File connectionsDir = getConnectionsPath(type).toFile();
			String connectionsDirPath = connectionsDir.getCanonicalPath();
			
			File[] files = destDir.listFiles();
			if (files == null)
				throw new IOException("Could not get files from '" + destDir + "'");

			ConnectionFileOperator operator = new ConnectionFileOperator();
			ConnectionTypeInfo info = connectionStorage.getConnectionTypeInfo(type);

			for (File conFile : files)
			{
				ClearThConnection connection = operator.load(conFile, info);
				connection.setTypeInfo(info);
				ClearThConnection oldConnection = connectionStorage.getConnection(connection.getName(), type);
				if (oldConnection != null)
				{
					stopRunnableConnection(oldConnection);
					connectionStorage.modifyConnection(oldConnection, connection);
				}
				else
					connectionStorage.addConnection(connection);
			}
			logger.debug("Imported {} connections are unzipped and copied to '{}'", type, connectionsDirPath);
		}
		finally
		{
			FileUtils.deleteDirectory(destDir);
		}
	}

	private void stopRunnableConnection(ClearThConnection connection)
	{
		if (connection instanceof ClearThRunnableConnection)
		{
			ClearThRunnableConnection runnableConnection = (ClearThRunnableConnection) connection;
			if (runnableConnection.isRunning())
			{
				try
				{
					runnableConnection.stop();
				}
				catch (Exception e)
				{
					logger.error("Could not stop connection '{}'", connection.getName());
				}
			}
		}
	}

	private Path getConnectionsPath(String type) throws ConnectivityException
	{
		return connectionStorage.getConnectionTypeInfo(type).getDirectory();
	}
}
