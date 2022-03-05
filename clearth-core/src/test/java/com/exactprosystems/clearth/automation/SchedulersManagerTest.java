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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.testng.annotations.*;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class SchedulersManagerTest
{
	private static ApplicationManager clearThManager;
	private static SchedulersManager manager;

	@BeforeClass
	public static void startApp() throws ClearThException
	{
		clearThManager = new ApplicationManager();
		manager = ClearThCore.getInstance().getSchedulersManager();
	}

	@AfterClass
	public static void disposeApp() throws IOException
	{
		if (clearThManager != null)
			clearThManager.dispose();
	}

	@DataProvider(name = "valid-names")
	public Object[][] createValidNamesData()
	{
		return new Object[][] {
				{ "admin" }, 
				{ "admin_" }, 
				{ "_admin_" }, 
				{ "+_ad_min_+" }, 
				{ ",admin." }, 
				{ "(a%d==#min$)" },
				{ "#3123$4%^&-@`~" }
			};
	}

	@DataProvider(name = "invalid-names")
	public Object[][] createInvalidNamesData()
	{
		return new Object[][] {
				{ "/admin" }, 
				{ "\\admin" }, 
				{ ":admin:" }, 
				{ "adm_<|>_in" }, 
				{ "|admin?" }, 
				{ "admin/" }, 
				{ "" }
			};
	}

	@Test(dataProvider = "valid-names", priority = 1)
	public void testAddingSchedulerWithValidName(String name) throws Exception
	{
		manager.addScheduler("admin", name);
		assertNotNull(manager.getSchedulerByName(name, "admin"));
	}

	@Test(expectedExceptions = SettingsException.class, dataProvider = "invalid-names", priority = 1)
	public void testAddingSchedulerWithInvalidName(String name) throws Exception
	{
		manager.addScheduler("admin", name);
	}
}