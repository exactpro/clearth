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

package com.exactprosystems.clearth.connectivity.connections.settings;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class SettingValuesTest
{
private Processor processor;
	
	@BeforeClass
	public void init() throws NoSuchMethodException, SecurityException
	{
		processor = new Processor();
	}
	
	@Test
	public void valuesAssignment() throws SettingDeclarationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		SettingsModel model = processor.process(CorrectSettings.class);
		
		DummyConnection con = new DummyConnection();
		
		SettingValues values = new SettingValues(model, con, false);
		values.getSetting("host").setStringValue("localhost");
		values.getSetting("port").setIntValue(8090);
		((EnumSettingAccessor)values.getSetting("mode")).setEnumValue("REST");
		values.getSetting("timeout").setLongValue(((long)Integer.MAX_VALUE+10));
		values.getSetting("autoReconnect").setBooleanValue(true);
		
		CorrectSettings conSettings = (CorrectSettings)con.getSettings();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(conSettings.getHost(), "localhost", "Host");
		soft.assertEquals(conSettings.getPort(), 8090, "Port");
		soft.assertEquals(conSettings.getMode(), ConnectionMode.REST, "Mode");
		soft.assertEquals(conSettings.getTimeout(), ((long)Integer.MAX_VALUE+10), "Timeout");
		soft.assertEquals(conSettings.isAutoReconnect(), true, "Auto-reconnect");
		soft.assertAll();
	}
	
	@Test
	public void supportedTypes() throws SettingDeclarationException
	{
		SettingsModel model = processor.process(CorrectSettings.class);
		SettingValues values = new SettingValues(model, new DummyConnection(), false);
		
		List<String> expectedAll = Arrays.asList("login", "password", "host", "port", "mode", "timeout", "autoReconnect", "multiline"),
				expectedSupported = new ArrayList<>(expectedAll);
		expectedSupported.remove("multiline");  //This field is List<String>, which is not supported by GUI and shouldn't be present among supported settings
		
		List<String> actualAll = collectNames(values.getAllSettings()),
				actualSupported = collectNames(values.getSupportedSettings());
		
		Assert.assertEquals(actualAll, expectedAll, "All settings");
		Assert.assertEquals(actualSupported, expectedSupported, "Supported settings");
	}
	
	
	private List<String> collectNames(Collection<SettingAccessor> accessors)
	{
		return accessors.stream()
				.map(a -> a.getProperties().getFieldName())
				.collect(Collectors.toList());
	}
}
