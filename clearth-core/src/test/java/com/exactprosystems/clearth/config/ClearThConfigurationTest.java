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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.HashMap;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

public class ClearThConfigurationTest
{
	public static File configFile, incompleteCfgFile;
	public static final String cfgResourcePath = "ConfigurationsClearTh/clearth.cfg",
								incompleteCfg = "ConfigurationsClearTh/incompleteConfig.cfg";

	private static Map<String, String> SAMPLE_MAP;

	static {
		SAMPLE_MAP = new HashMap<>();
		SAMPLE_MAP.put("arg1", "val1");
		SAMPLE_MAP.put("arg2", "val2");
	}

	public ClearThConfigurationTest() {}

	@BeforeClass
	public static void init() throws FileNotFoundException
	{
		configFile = new File(resourceToAbsoluteFilePath(cfgResourcePath));
		if(!configFile.exists() || !configFile.isFile())
			throw new FileNotFoundException("File '" + configFile.getName() + "' not found");

		incompleteCfgFile = new File(resourceToAbsoluteFilePath(incompleteCfg));
		if(!incompleteCfgFile.exists() || !incompleteCfgFile.isFile())
			throw new FileNotFoundException("File '" + incompleteCfgFile.getName() + "' not found");
	}

	@DataProvider(name = "automationConfig")
	Object[][] getAutomationConfig()
	{
		return new Object[][]
		{
			{
				configFile, true, true
			},
			{
				incompleteCfgFile, true, true
			}
		};
	}

	@Test(dataProvider = "automationConfig")
	public void testAutomationConfig(File file, boolean expectedIsUserSchedulersAllowed, boolean duplicateActionId) 
		throws ConfigurationException
	{
		ClearThConfiguration configuration = ClearThConfiguration.create(file);
		Automation automation = configuration.getAutomation();
		Assert.assertEquals(automation.isUserSchedulersAllowed(), expectedIsUserSchedulersAllowed);

		MatrixFatalErrors matrixFatalErrors = automation.getMatrixFatalErrors();
		Assert.assertEquals(matrixFatalErrors.isDuplicateActionId(), duplicateActionId);
	}

	@DataProvider(name = "memoryMonitorConfig")
	Object[][] getMemoryMonitorConfig()
	{
		return new Object[][]
		{
			{
				configFile, 10001, 50000001, 100000001
			},
			{
				incompleteCfgFile, 10003, 50000000, 100000000
			}
		};
	}
	@Test(dataProvider = "memoryMonitorConfig")
	public void testMemoryMonitorConfig(File file, long expectedSleep, long expectedLargeDiff,
	                                    long expectedLowMemory) throws ConfigurationException
	{
		ClearThConfiguration configuration = ClearThConfiguration.create(file);
		MemoryMonitorCfg cfgMonitor = configuration.getMemory().getMonitor();
		Assert.assertEquals(cfgMonitor.getSleep(), expectedSleep);
		Assert.assertEquals(cfgMonitor.getLargeDiff(), expectedLargeDiff);
		Assert.assertEquals(cfgMonitor.getLowMemory(), expectedLowMemory);
	}

	@DataProvider(name = "locationsConfig")
	Object[][] getLocationsConfig()
	{
		return new Object[][]
		{
			{
				configFile, SAMPLE_MAP
			},
			{
				incompleteCfgFile,  null
			}
		};
	}

	@Test(dataProvider = "locationsConfig")
	public void testLocationsConfig(File file, Map<String, String> expectedLocations) throws ConfigurationException
	{
		ClearThConfiguration configuration = ClearThConfiguration.create(file);
		
		Assert.assertEquals(configuration.getLocationsMapping(), expectedLocations);
	}
}