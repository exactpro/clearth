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

package com.exactprosystems.clearth.web.beans.tools;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.tools.ToolsInfo;
import com.exactprosystems.clearth.tools.ToolsManager;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;

public class ToolsBean extends ClearThBean
{
	protected int activeTab = 0;
	protected int favoriteActiveTab = 0;
	protected int collectorScannerTab = 0;
	
	protected ToolsManager toolsManager;
	protected ToolsInfo allTools;
	protected String username;
	protected Set<Integer> favoriteTools;  //No need to update this set because it is obtained by reference and is updated by toolsManager 

	public ToolsBean() {

	}

	@PostConstruct
	private void init()
	{
		toolsManager = ClearThCore.getInstance().getToolsManager();
		allTools = toolsManager.getToolsInfo();
		username = UserInfoUtils.getUserName();
		favoriteTools = toolsManager.getUserFavoriteTools(username);

		if (!favoriteTools.isEmpty())
		{
			activeTab = favoriteActiveTab = favoriteTools.iterator().next();
		}
	}

	
	public ToolsInfo getAllTools()
	{
		return allTools;
	}

	public boolean isFavorite(int toolId)
	{
		return favoriteTools.contains(toolId);
	}

	public void favorite(int toolId)
	{
		toolsManager.favoriteStateChanged(username, toolId, true);
		setActiveTab(toolId);
	}

	public void unfavorite(int toolId)
	{
		toolsManager.favoriteStateChanged(username, toolId, false);
		setActiveTab(toolId);
	}

	
	public int getActiveTab()
	{
		return this.activeTab;
	}
	
	public void setActiveTab(int activeTab)
	{
		this.activeTab = activeTab;
		if (favoriteTools.contains(activeTab))
			favoriteActiveTab = activeTab;
		else
			favoriteActiveTab = -1;
	}


	public int getFavoriteActiveTab()
	{
		return favoriteActiveTab;
	}

	public void setFavoriteActiveTab(int favoriteActiveTab)
	{
		this.favoriteActiveTab = favoriteActiveTab;
		this.activeTab = favoriteActiveTab;
	}

	
	public String getToolName(int toolId)
	{
		return allTools.getToolInfo(toolId).getName();
	}

	public Set<Integer> getFavoriteTools()
	{
		return favoriteTools;
	}

	
	public int getCollectorScannerTab()
	{
		return this.collectorScannerTab;
	}
	
	public void setCollectorScannerTab(int collectorScannerTab)
	{
		this.collectorScannerTab = collectorScannerTab;
	}

	public List<String> getSchedulers()
	{
		return ClearThCore.getInstance().getSchedulersManager().getAvailableSchedulerNames(UserInfoUtils.getUserName());
	}	
}
