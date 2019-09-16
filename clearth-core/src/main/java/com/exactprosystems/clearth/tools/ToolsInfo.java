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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.collection.UnmodifiableCollection;

public class ToolsInfo
{
	protected final Map<Integer, ToolInfo> tools;
	
	public ToolsInfo(List<ToolInfo> tools)
	{
		this.tools = createToolsMap(tools);
	}
	
	
	protected Map<Integer, ToolInfo> createToolsMap(List<ToolInfo> tools)
	{
		Map<Integer, ToolInfo> result = new LinkedHashMap<Integer, ToolInfo>();
		for (ToolInfo ti : tools)
			result.put(ti.getId(), ti);
		return result;
	}
	
	
	public Collection<ToolInfo> getTools()
	{
		return UnmodifiableCollection.unmodifiableCollection(tools.values());
	}
	
	public ToolInfo getToolInfo(int id)
	{
		return tools.get(id);
	}
	
	public ToolInfo getToolInfo(String name)
	{
		for (ToolInfo ti : tools.values())
		{
			if (ti.getName().equals(name))
				return ti;
		}
		return null;
	}
}
