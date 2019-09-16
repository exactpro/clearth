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

package com.exactprosystems.clearth;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;

/**
 * Created by alexey.karpukhin on 8/24/16.
 */
public abstract class DeploymentConfig {

	protected final SimpleDateFormat buildDateFormat = new SimpleDateFormat("dd/MM/yyyy");

	protected ConfigFiles configFiles;
	protected File workingDir;

	protected String logsDir;
	protected boolean logsDirOverride;

	protected String appRoot;

	protected String root;
	protected boolean rootOverride;
	protected String configFileName;


	public String getLogsDir() {
		return logsDir;
	}
	public boolean isLogsDirOverride() {
		return logsDirOverride;
	}

	public String getAppRoot() {
		return appRoot;
	}

	public String getRoot() {
		return root;
	}
	public boolean isRootOverride() {
		return rootOverride;
	}
	public String getConfigFileName(){
		return configFileName;
	}


	protected void configureAppRoot() {
		//Getting application root path
		try
		{
			URL url = Thread.currentThread().getContextClassLoader().getResource("/");
			if (url == null)
				url = ClearThCore.class.getResource("/");
			appRoot = URLDecoder.decode(url.getFile(), "UTF-8") + "../";
		}
		catch (Exception e)
		{
			System.err.println("Error occurred while getting classes root to read settings");
			appRoot = "";
		}
	}

	protected void configureRoot() {
		rootOverride = false;
		try  {
			if (!trySelectRoot(workingDir.getParentFile())) {
				if (!trySelectRoot(workingDir)) {
					configFileName = null;
					root = null;
				}
			}
		}
		catch (IOException e) {
			configFileName = null;
			root = null;
		}
	}
	
	protected void configureLogsDir()
	{
		logsDirOverride = false;
		logsDir = configFiles.getLogsDir();
	}
	
	private boolean trySelectRoot(File folder) throws IOException {
		root = folder.getCanonicalPath();
		if (root.charAt(root.length() - 1) != File.separatorChar) {
			root += File.separatorChar;
		}
		File expConf = new File (folder, configFiles.getConfigFilePath());
		configFileName = expConf.getCanonicalPath();
		return expConf.exists();
	}



	public void init(ConfigFiles configFiles) {
		this.workingDir = new File(System.getProperty("user.dir"));
		this.configFiles = configFiles;

		configureAppRoot();
		configureRoot();

		if (configFileName == null || root == null) {
			System.err.println("Could not find '"+configFiles.getCfgDir()+"' directory, using default one");
			configFileName = configFiles.getConfigFilePath();
			if (!new File(configFileName).isAbsolute()) {
				configFileName = appRoot + configFileName;
			}
			root = appRoot;
		}
		
		configureLogsDir();

		additionalInit();
	}

	protected abstract void additionalInit();

	//TBD later for simple jar manifest
	protected abstract ClearThVersion getCTHVersion();
}
