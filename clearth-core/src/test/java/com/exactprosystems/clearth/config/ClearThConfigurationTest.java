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

package com.exactprosystems.clearth.config;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ClearThException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

public class ClearThConfigurationTest
{
	public static ClearThConfiguration configuration;

	public ClearThConfigurationTest() {}

	@BeforeClass
	public static void init() throws ConfigurationException, FileNotFoundException
	{
		File configFile = new File(resourceToAbsoluteFilePath("ConfigurationsClearTh/clearth.cfg"));

		if(!configFile.exists() || !configFile.isFile())
			throw new FileNotFoundException("File '" + configFile.getName() + "' not found");

		configuration = ClearThConfiguration.create(configFile);
	}

	@Test
	public void unmarshalConfig()
	{
		boolean userSchedulersAllowed = configuration.getAutomation().isUserSchedulersAllowed();
		Assert.assertTrue(userSchedulersAllowed);
	}

	@Test
	public void unmarshalCfgFromCore() throws ClearThException, IOException
	{
		ApplicationManager manager = new ApplicationManager();
		try
		{
			ClearThCore core = ClearThCore.getInstance();
			Assert.assertTrue(core.isUserSchedulersAllowed());
		}
		finally
		{
			manager.dispose();
		}
	}
}