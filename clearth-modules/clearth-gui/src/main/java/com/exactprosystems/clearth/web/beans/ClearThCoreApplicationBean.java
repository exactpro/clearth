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
import com.exactprosystems.clearth.ConfigFiles;
import com.exactprosystems.clearth.DeploymentConfig;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.MemoryAndSpaceMonitor;
import com.exactprosystems.clearth.web.WebDeploymentConfig;
import com.exactprosystems.clearth.web.jetty.JettyXmlUpdater;

/**
 * Created by alexey.karpukhin on 8/25/16.
 */
public abstract class ClearThCoreApplicationBean extends ClearThBean {

	public static ClearThCoreApplicationBean appInstance = null;

	protected DeploymentConfig deploymentConfig = null;
	protected ConfigFiles configFiles = null;

	private String appContextPath;
	private boolean appContextPathOverride;

	public String getAppContextPath()
	{
		return appContextPath;
	}
	public boolean isAppContextPathOverride()
	{
		return appContextPathOverride;
	}

	public ClearThCoreApplicationBean() throws ClearThException
	{
		synchronized (ClearThCoreApplicationBean.class)
		{
			if (appInstance != null)
			{
				throw new ClearThException("ApplicationBean is already created");
			}
			appInstance = this;
		}

		configFiles = createConfigFiles();
		deploymentConfig = createDeploymentConfig();
		deploymentConfig.init(configFiles);
		initApplication();
		initWebApplication();
	}

	protected abstract void initApplication() throws ClearThException;

	protected void initWebApplication() throws ClearThException
	{

		WebDeploymentConfig webDeploymentConfig = (WebDeploymentConfig) deploymentConfig;
		this.appContextPath  = webDeploymentConfig.getAppContextPath();
		this.appContextPathOverride = webDeploymentConfig.isAppContextPathOverride();

		updateJettyXml(ClearThCore.rootRelative("jetty/etc/jetty.xml"));
	}
	
	protected void updateJettyXml(String jettyXmlPath)
	{
		JettyXmlUpdater xmlUpdater = initJettyXmlUpdater();
		xmlUpdater.updateJettyXml(jettyXmlPath);
	}
	
	protected JettyXmlUpdater initJettyXmlUpdater()
	{
		return new JettyXmlUpdater();
	}

	protected DeploymentConfig createDeploymentConfig()
	{
		return new WebDeploymentConfig();
	}

	protected abstract ConfigFiles createConfigFiles();

	public static ClearThCoreApplicationBean getInstance()
	{
		return appInstance;
	}

	public String getUsedMemoryInfo()
	{
		return String.valueOf(MemoryAndSpaceMonitor.getUsedMemoryMb());
	}

	public String getMaxMemoryInfo()
	{
		return String.valueOf(MemoryAndSpaceMonitor.getMaxMemoryMb());
	}

	public String getUsedSpaceInfo()
	{
		return String.valueOf(MemoryAndSpaceMonitor.getUsedSpace(MemoryAndSpaceMonitor.GB_FACTOR));
	}

	public String getTotalSpaceInfo()
	{
		return String.valueOf(MemoryAndSpaceMonitor.getTotalSpace(MemoryAndSpaceMonitor.GB_FACTOR));
	}

	public boolean isMemoryBreachesLimit()
	{
		return MemoryAndSpaceMonitor.isMemoryBreachLimit(90);
	}
}
