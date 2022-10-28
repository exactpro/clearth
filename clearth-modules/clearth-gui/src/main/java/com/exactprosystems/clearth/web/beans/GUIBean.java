/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.beans;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIBean
{
	private static final Logger logger = LoggerFactory.getLogger(GUIBean.class);
	private static final String MENU_EXPANDED = "menuExpanded";
	
	private boolean menuExpanded;
	private String configFileName;

	public GUIBean()
	{
		configFileName = ClearThCore.userSettingsPath() + UserInfoUtils.getUserName() + "/" +
				ClearThCore.configFiles().getGUIConfigFileName();
		loadState();
	}

	public void loadState()
	{
		menuExpanded = true;
		
		if (!Files.exists(Paths.get(configFileName)))
			return;
		
		Map<String, String> config = null;
		try
		{
			config = KeyValueUtils.loadKeyValueFile(configFileName, false);
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not load GUI config", e, logger);
			return;
		}
		
		String exp = config.get(MENU_EXPANDED);
		if (exp != null)
			menuExpanded = Boolean.valueOf(exp);
	}

	public void saveState()
	{
		Map<String, String> config = new HashMap<String, String>();
		config.put(MENU_EXPANDED, Boolean.toString(menuExpanded));
		
		File configFile = new File(configFileName);
		if (!configFile.isFile())
			configFile.getParentFile().mkdirs();
		try 
		{
			KeyValueUtils.saveKeyValueFile(config, configFileName);
		} 
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not save GUI config", e, logger);
		}
	}

	public boolean isMenuExpanded()
	{
		return menuExpanded;
	}

	public void setMenuExpanded(boolean menuExpanded)
	{
		this.menuExpanded = menuExpanded;
		saveState();
	}

	public String getPageName()
	{
		String url = FacesContext.getCurrentInstance().getViewRoot().getViewId();
		return url.substring(url.lastIndexOf('/') + 1);
	}
}
