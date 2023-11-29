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

package com.exactprosystems.clearth.web;

import com.exactprosystems.clearth.ClearThVersion;
import com.exactprosystems.clearth.DeploymentConfig;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import java.io.File;
import java.util.Date;
import java.util.jar.Manifest;

public class WebDeploymentConfig extends DeploymentConfig {

	protected String appContextPath;
	protected boolean appContextPathOverride;

	public String getAppContextPath() {
		return appContextPath;
	}
	public boolean isAppContextPathOverride(){
		return appContextPathOverride;
	}

	protected void configureAppContextPath() {
		appContextPath = FacesContext.getCurrentInstance().getExternalContext().getInitParameter("AppContextPath");
		if (appContextPath != null)
		{
			appContextPathOverride = true;
			System.out.println("Application context path overridden in web.xml to '"+appContextPath+"'");
		}
		else
		{
			appContextPathOverride = false;
			String currentContext = FacesContext.getCurrentInstance().getExternalContext().getContextName();
			if (currentContext == null)
				currentContext = "/clearth";
			if (!currentContext.startsWith("/"))
				currentContext = "/" + currentContext;
			if (currentContext.equals("/root"))  //When ClearTH is deployed as root application of server
				currentContext = "";
			
			appContextPath = currentContext;
		}
	}

	@Override
	protected void configureRoot() {
		//Getting files root path
		String homeWebXml = FacesContext.getCurrentInstance().getExternalContext().getInitParameter("ClearThHome");
		if (homeWebXml != null)
		{
			rootOverride = true;
			File home = new File(homeWebXml);
			File c = new File(home, configFiles.getConfigFilePath());
			configFileName = c.getAbsolutePath();
			root = home.getAbsolutePath()+"/";
			System.out.println("Home directory overridden in web.xml to '"+root+"'");
		}
		else  //Searching for main config file to determine files root by its location
		{
			super.configureRoot();
		}
	}
	
	@Override
	protected void configureLogsDir()
	{
		logsDir = FacesContext.getCurrentInstance().getExternalContext().getInitParameter("LogsDirectory");
		if (logsDir != null)
		{
			logsDirOverride = true;
			System.out.println("Logs directory overridden in web.xml to '"+logsDir+"'");
			return;
		}
		
		logsDirOverride = false;
		File f = new File("logs/");  //If ClearTH is started within separate Jetty server, this will point to jetty/logs/ because "jetty" is web-application home directory
		logsDir = f.getAbsolutePath();
		if (!f.exists())  //In case of problems, using default logs path
			logsDir = configFiles.getLogsDir();
		System.out.println("Logs directory: "+logsDir);
	}

	@Override
	protected void additionalInit() {
		configureAppContextPath();
	}

	@Override
	protected ClearThVersion getCTHVersion() {
		String buildNum = null;
		String buildDate = null;
		Manifest mf = null;
		String raw = null;
		try
		{
			mf = new Manifest(((ServletContext) FacesContext
					.getCurrentInstance().getExternalContext()
					.getContext()).getResourceAsStream("/META-INF/MANIFEST.MF"));
			raw = mf.getMainAttributes().getValue("Implementation-Version");
		}
		catch (Exception e)
		{
			System.err.println("Error while reading MANIFEST.MF in war file. Using 'local_build' as version value:");
		}

		if (raw == null)
		{
			buildNum = "local_build";
			buildDate = buildDateFormat.format(new Date());
		}
		else
		{
			try
			{
				String[] versionFormat = raw.split("\\|");
				buildNum = versionFormat[0];
				buildDate = versionFormat[1];
			}
			catch (Exception e)
			{
				throw new RuntimeException("Incorrect version format");
			}
		}
		return new ClearThVersion(buildNum, buildDate);
	}

	@Override
	protected void configureAppRoot() {
		
		this.appRoot = ((ServletContext) FacesContext
				.getCurrentInstance().getExternalContext()
				.getContext()).getRealPath("/WEB-INF");
		
		if (!appRoot.endsWith("/")) {
			this.appRoot += "/";
		}
	}
}
