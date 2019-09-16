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

package com.exactprosystems.clearth.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import com.exactprosystems.clearth.utils.Utils;

public class FavoriteToolsInfo
{
	protected final ToolsInfo tools;
	private final Set<Integer> favoriteTools;
	private final File file;
	
	public FavoriteToolsInfo(File infoFile, ToolsInfo availableTools) throws IOException
	{
		tools = availableTools;
		favoriteTools = readFavoriteTools(infoFile);
		file = infoFile;
	}
	
	public FavoriteToolsInfo(Set<Integer> favoriteTools, File infoFile, ToolsInfo availableTools)
	{
		tools = availableTools;
		this.favoriteTools = favoriteTools;
		file = infoFile;
	}

	
	protected Set<Integer> readFavoriteTools(File f) throws IOException
	{
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new FileReader(f));
			
			Set<Integer> favTools = new LinkedHashSet<Integer>();
			String line = null;
			while ((line = br.readLine()) != null)
			{
				ToolInfo ti = tools.getToolInfo(line);  //File stores names of tools to be easily edited by user, if needed
				if (ti != null)
					favTools.add(ti.getId());
			}
			
			return favTools;
		}
		finally
		{
			Utils.closeResource(br);
		}
	}
	
	
	public Set<Integer> getFavoriteTools()
	{
		return favoriteTools;
	}
	
	public File getFile()
	{
		return file;
	}
	
	public void save() throws IOException
	{
		PrintWriter writer = null;
		try
		{
			writer = new PrintWriter(file);
			for (Integer id : favoriteTools)
				writer.println(tools.getToolInfo(id).getName());
		}
		finally
		{
			Utils.closeResource(writer);
		}
	}
}
