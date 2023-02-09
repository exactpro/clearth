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
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;

public class ClearThConfigurationTest
{
	public static File configFile, incompleteCfgFile;
	public static final String cfgResourcePath = "ConfigurationsClearTh/clearth.cfg",
								incompleteCfg = "ConfigurationsClearTh/incompleteConfig.cfg";

	public static Set<String> parameters;
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

		parameters = new HashSet<>();
		parameters.add("testcase");
		parameters.add("keyfield");
	}

	@DataProvider(name = "automationConfig")
	Object[][] getAutomationConfig()
	{
		return new Object[][]
		{
			{
				configFile, true, true, parameters
			},
			{
				incompleteCfgFile, true, true, new HashSet<>()
			}
		};
	}
	
	@DataProvider(name ="connectivityConfig")
	Object[][] connectivityConfig()
	{
		return new Object[][]
		{
			{
				configFile, "dummyType", "com.exactprosystems.clearth.connectivity.DummyConnection", "dummy_connections"
			},
			{
				incompleteCfgFile, null, null, null
			}
		};
	}

	@Test(dataProvider = "automationConfig")
	public void testAutomationConfig(File file, boolean expectedIsUserSchedulersAllowed, boolean duplicateActionId,
	                                 Set<String> expParams)
		throws ConfigurationException
	{
		ClearThConfiguration configuration = ClearThConfiguration.create(file);
		Automation automation = configuration.getAutomation();
		Assert.assertEquals(automation.isUserSchedulersAllowed(), expectedIsUserSchedulersAllowed);

		MatrixFatalErrors matrixFatalErrors = automation.getMatrixFatalErrors();
		Assert.assertEquals(matrixFatalErrors.isDuplicateActionId(), duplicateActionId);

		SpecialActionParameters specialActionParameters = automation.getSpecialActionParameters();
		Assert.assertEquals(specialActionParameters.getParameters(), expParams);
	}
	
	@Test(dataProvider = "connectivityConfig")
	public void testConnectivityConfig(File file, String typeName, String typeClass, String typeDirectory) throws ConfigurationException
	{
		ClearThConfiguration configuration = ClearThConfiguration.create(file);
		Connectivity connectivity = configuration.getConnectivity();
		
		ConnectionTypesConfig typesConfig = connectivity.getTypesConfig();
		if (typeName == null)
		{
			Assert.assertEquals(typesConfig.getTypes().size(), 0);
			return;
		}
		
		ConnectionType actualType = typesConfig.getTypes().get(0);
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(actualType.getName(), typeName);
		soft.assertEquals(actualType.getConnectionClass(), typeClass);
		soft.assertEquals(actualType.getDirectory(), typeDirectory);
		soft.assertAll();
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