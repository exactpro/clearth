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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;

import static com.exactprosystems.clearth.ClearThCore.connectionStorage;

public class FavoriteConnectionManager
{
	private Map<String, Set<String>> favoriteConnectionList;
	private Map<String, File> fileList;
	
	private final static Logger logger = LoggerFactory.getLogger(FavoriteConnectionManager.class);
	
	private final String FAVORITE_CONNECTION_FILE_NAME = "favorite_connections.txt";
	
	private File userSettingsDir;
	
	boolean containsMissingReferences = false;
	
	public FavoriteConnectionManager(File userSettingsDir)
	{
		favoriteConnectionList = new HashMap<String, Set<String>>();
		fileList = new HashMap<String, File>();
		this.userSettingsDir = userSettingsDir;
		
		File[] dirs = userSettingsDir.listFiles(File::isDirectory);
		
		for (File dir : dirs)
		{
			File favoriteConnectionFile = new File(dir, FAVORITE_CONNECTION_FILE_NAME);
			if (!favoriteConnectionFile.exists() || favoriteConnectionFile.isDirectory())
			{
				continue;
			}
			else
			{
				String dirName = dir.getName();
				try
				{
					fileList.put(dirName, favoriteConnectionFile);
					favoriteConnectionList.put(dirName, this.readFavoriteConnectionList(favoriteConnectionFile));
					if (containsMissingReferences)
					{
						flushFile(dirName);
					}
				}
				catch (IOException ex)
				{
					logger.error("Error with reading file", ex);
					fileList.remove(dirName);
					favoriteConnectionList.remove(dirName);
				}
			}
		}
	}
	
	private boolean containsConnection(List<ClearThConnection<?,?>> conn, String name)
	{
		for (ClearThConnection<?,?> connection : conn)
		{
			if (connection.getName().equals(name))
			{
				return true;
			}
		}
		return false;
	}

	private Set<String> readFavoriteConnectionList(File f) throws IOException
	{
		Set<String> favoriteConnections = new HashSet<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(f)))
		{
			containsMissingReferences = false;
			String connectionName;
			while ((connectionName = br.readLine()) != null)
			{
				if (connectionStorage().containsConnection(connectionName))
					favoriteConnections.add(connectionName);
				else
					containsMissingReferences = true;
			}
		}
		return favoriteConnections;
	}
	
	private void flushFile(String username) throws IOException
	{
		File file = this.fileList.get(username);
		file.delete();
		file.createNewFile();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file)))
		{
			for (String s : this.favoriteConnectionList.get(username))
			{
				bw.write(s);
				bw.newLine();
			}
			bw.flush();
		}
	}
	
	public void favoriteStateChanged(String username, String connectionName, boolean isFavorite)
	{
		if (isFavorite)
		{
			this.favoriteConnectionList.get(username).add(connectionName);
		}
		else
		{
			this.favoriteConnectionList.get(username).remove(connectionName);
		}
		
		try
		{
			flushFile(username);
		}
		catch (IOException e)
		{
			logger.error("Cannot write to file", e);
		}
	}
	
	public void changeName(String oldName, String newName)
	{
		if (oldName.equals(newName))
		{
			return;
		}
		
		for (Entry<String, Set<String>> ent : this.favoriteConnectionList.entrySet())
		{
			if (ent.getValue().contains(oldName))
			{
				ent.getValue().remove(oldName);
				if (newName != null)  //newName == null when connection was removed
				{
					ent.getValue().add(newName);
				}
				try
				{
					flushFile(ent.getKey());
				}
				catch (IOException e)
				{
					logger.error("Cannot write to file", e);
				}
			}
		}
	}
	
	public Set<String> getUserFavoriteConnectionList(String username)
	{
		Set<String> list = this.favoriteConnectionList.get(username);
		if (list == null)
		{
			list = new HashSet<String>();
			this.favoriteConnectionList.put(username, list);
			File fdir = new File(userSettingsDir,username);
			fdir.mkdir();
			this.fileList.put(username, new File(fdir, FAVORITE_CONNECTION_FILE_NAME));

		}
		return list;
	}
	
}
