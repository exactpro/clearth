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

package com.exactprosystems.clearth.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager of all built-in tools. 
 * It provides access to ToolsFactory that creates instances of tools, to storage of all available tools, manages user favorite tools
 * @author vladimir.panarin
 */
public class ToolsManager
{
	private final static Logger logger = LoggerFactory.getLogger(ToolsManager.class);
	protected static final String FAVORITE_TOOL_FILE_NAME = "favorite_tools.txt";
	public static final String TOOL_MESSAGE_PARSER = "Message parser",
			TOOL_MESSAGE_TO_SCRIPT = "Message to script",
			TOOL_SCRIPT_TO_MESSAGE = "Script to message",
			TOOL_COLLECTOR_SCANNER = "Collector scanner",
			TOOL_CONFIG_MAKER = "Config maker",
			TOOL_MATRIX_FROM_REPORT = "Matrix from report",
			TOOL_MESSAGE_HELPER = "Message helper",
			TOOL_EXP_CALC = "Expression calculator",
			TOOL_DICT_VALIDATOR = "Dictionary validator",
			TOOL_MATRIX_UPDATER = "Matrix updater",
			TOOL_SQL_EXECUTOR = "SQL Executor";
	
	protected final ToolsInfo toolsInfo;
	protected final ToolsFactory toolsFactory;
	protected final File userSettingsDir;
	protected final Map<String, FavoriteToolsInfo> favoriteToolsInfo;  //Key - username
	
	public ToolsManager(File userSettingsDir)
	{
		toolsInfo = createToolsInfo();
		toolsFactory = createToolsFactory();
		this.userSettingsDir = userSettingsDir;
		favoriteToolsInfo = createFavoriteToolsMap();
		
		List<File> settingsFiles = getUserSettingsFiles(userSettingsDir);  //All files are cfg/userSettings/<userName>/favorite_tools.txt
		loadSettings(settingsFiles);
	}
	
	
	protected ToolInfo createToolInfo(int id, String name)
	{
		return new ToolInfo(id, name);
	}
	
	protected List<ToolInfo> createToolsList()
	{
		List<ToolInfo> result = new ArrayList<ToolInfo>();
		result.add(createToolInfo(0, TOOL_MESSAGE_PARSER));
		result.add(createToolInfo(3, TOOL_MESSAGE_TO_SCRIPT));
		result.add(createToolInfo(6, TOOL_SCRIPT_TO_MESSAGE));
		result.add(createToolInfo(10, TOOL_COLLECTOR_SCANNER));
		result.add(createToolInfo(20, TOOL_CONFIG_MAKER));
		result.add(createToolInfo(30, TOOL_MATRIX_FROM_REPORT));
		result.add(createToolInfo(40, TOOL_MESSAGE_HELPER));
		result.add(createToolInfo(50, TOOL_EXP_CALC));
		result.add(createToolInfo(70, TOOL_DICT_VALIDATOR));
		result.add(createToolInfo(80, TOOL_MATRIX_UPDATER));
		result.add(createToolInfo(90, TOOL_SQL_EXECUTOR));
		return result;
	}
	
	protected ToolsInfo createToolsInfo()
	{
		return new ToolsInfo(createToolsList());
	}
	
	protected ToolsFactory createToolsFactory()
	{
		return new ToolsFactory();
	}
	
	protected Map<String, FavoriteToolsInfo> createFavoriteToolsMap()
	{
		return new ConcurrentHashMap<String, FavoriteToolsInfo>();
	}
	
	protected FavoriteToolsInfo createFavoriteToolsInfo(File f) throws IOException
	{
		return new FavoriteToolsInfo(f, toolsInfo);
	}
	
	protected FavoriteToolsInfo createFavoriteToolsInfo(Set<Integer> favTools, File f)
	{
		return new FavoriteToolsInfo(favTools, f, toolsInfo);
	}
	
	
	protected List<File> getUserSettingsFiles(File dir)
	{
		File[] subDirs = dir.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				return pathname.isDirectory();
			}
		});
		
		List<File> result = new ArrayList<File>();
		for (File sd : subDirs)
		{
			File f = new File(sd, FAVORITE_TOOL_FILE_NAME);
			if (f.isFile())
				result.add(f);
		}
		return result;
	}
	
	protected void loadSettings(List<File> settingsFiles)
	{
		for (File f : settingsFiles)
		{
			String userName = f.getParentFile().getName();
			try
			{
				FavoriteToolsInfo fti = createFavoriteToolsInfo(f);
				favoriteToolsInfo.put(userName, fti);
			}
			catch (IOException ex)
			{
				logger.error("Error while reading tools info from file '"+f.getAbsolutePath()+"'", ex);
			}
		}
	}
	
	
	protected FavoriteToolsInfo initUserFavoriteToolsInfo(String username)
	{
		File dir = new File(userSettingsDir, username);
		dir.mkdirs();
		File infoFile = new File(dir, FAVORITE_TOOL_FILE_NAME);
		return createFavoriteToolsInfo(new LinkedHashSet<Integer>(), infoFile);
	}
	
	
	public FavoriteToolsInfo getUserFavoriteToolsInfo(String username)
	{
		FavoriteToolsInfo fti = favoriteToolsInfo.get(username);
		if (fti == null)
		{
			fti = initUserFavoriteToolsInfo(username);
			favoriteToolsInfo.put(username, fti);
		}
		return fti;
	}
	
	public Set<Integer> getUserFavoriteTools(String username)
	{
		FavoriteToolsInfo fti = getUserFavoriteToolsInfo(username);
		return fti.getFavoriteTools();
	}
	
	public void favoriteStateChanged(String username, int toolId, boolean isFavorite)
	{
		FavoriteToolsInfo fti = getUserFavoriteToolsInfo(username);
		Set<Integer> tools = fti.getFavoriteTools();
		if (isFavorite)
			tools.add(toolId);
		else
			tools.remove(toolId);
		
		try
		{
			fti.save();
		}
		catch (IOException e)
		{
			logger.error("Error while writing tools info to file '"+fti.getFile().getAbsolutePath()+"'", e);
		}
	}
	
	
	public ToolsInfo getToolsInfo()
	{
		return toolsInfo;
	}
	
	public ToolsFactory getToolsFactory()
	{
		return toolsFactory;
	}
}
