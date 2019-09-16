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

package com.exactprosystems.clearth.web.beans;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.KeyValueUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;

/**
 * @author victor.klochkov
 *
 */
public class GUIBean
{
	private Boolean menuExpanded;
	private String configFileName;

	public GUIBean()
	{
		configFileName = ClearThCore.userSettingsPath() + UserInfoUtils.getUserName() + "/" +
				ClearThCore.configFiles().getGUIConfigFileName();
		loadState();
	}

	public void loadState()
	{
		Map<String, String> config = KeyValueUtils.loadKeyValueFile(configFileName, false);
		menuExpanded = config.get("menuExpanded") != null ? Boolean.valueOf(config.get("menuExpanded")) : true;
	}

	public void saveState()
	{
		Map<String, String> config = new HashMap<String, String>();
		config.put("menuExpanded", menuExpanded.toString());
		
		File configFile = new File(configFileName);
		if (!configFile.isFile())
			configFile.getParentFile().mkdirs();
		
		KeyValueUtils.saveKeyValueFile(config, configFileName);
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
