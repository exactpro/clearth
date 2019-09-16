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

package com.exactprosystems.clearth.connectivity;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.FileOperationUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConnectionsTransmitter
{
	protected final static Logger logger = LoggerFactory.getLogger(ConnectionsTransmitter.class);
	
	public File exportConnections(String type) throws IOException
	{
		File destDir = new File(ClearThCore.tempPath()),
				resultFile = File.createTempFile(type + "_connections_", ".zip", destDir),
				connectionsDir = new File(getConnectionsPath(type));
		
		FileOperationUtils.zipFiles(resultFile, connectionsDir.listFiles());
		logger.debug("Created zip file with {} connections from {} to be exported", type, connectionsDir);
		return resultFile;
	}
	
	public synchronized void deployConnections(String type, File zipFile) throws IOException
	{
		File destDir = FileOperationUtils.createTempDirectory("imported_" + type + "_connections_", new File(ClearThCore.uploadStoragePath()));
		try
		{
			FileOperationUtils.unzipFile(zipFile, destDir);
			File connectionsDir = new File(getConnectionsPath(type));
			String connectionsDirPath = connectionsDir.getCanonicalPath();
			
			File[] files = destDir.listFiles();
			if (files == null)
				throw new IOException("Could not get files from '" + destDir + "'");
			
			for (File conFile : files)
			{
				if (conFile != null)
					FileOperationUtils.copyFile(conFile.getCanonicalPath(), connectionsDirPath + "/" + conFile.getName());
			}
			logger.debug("Imported {} connections are unzipped and copied to '{}'", type, connectionsDirPath);
			ClearThCore.connectionStorage().reloadConnections();
		}
		finally
		{
			FileUtils.deleteDirectory(destDir);
		}
	}
	
	private String getConnectionsPath(String type)
	{
		return ClearThCore.connectionStorage().getConnectionsPath(type);
	}
}
