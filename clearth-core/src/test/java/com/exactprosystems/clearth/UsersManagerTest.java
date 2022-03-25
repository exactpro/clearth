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

package com.exactprosystems.clearth;

import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.xmldata.XmlUser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Collections;

public class UsersManagerTest
{
	private static ApplicationManager clearThManager;
	private static UsersManager manager;

	@BeforeClass
	public static void startApp() throws ClearThException
	{
		clearThManager = new ApplicationManager();
		manager = ClearThCore.getInstance().getUsersManager();
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

	@Test(dataProvider = "valid-names")
	public void testAddingUserWithValidName(String name)
			throws Exception
	{
		XmlUser user = new XmlUser();
		user.setName(name);
		manager.addUser(user);
		try
		{
			Assert.assertTrue(manager.userList.stream().anyMatch(u -> u.getName().equals(name)));
		}
		finally
		{
			manager.removeUsers(Collections.singletonList(user));
		}
	}

	@Test(expectedExceptions = SettingsException.class, dataProvider = "invalid-names")
	public void testAddingUserWithInvalidName(String name)
			throws JAXBException, ClearThException, SettingsException
	{
		XmlUser user = new XmlUser();
		user.setName(name);
		manager.addUser(user);
	}

	@Test(dataProvider = "valid-names")
	public void testEditingUserWithValidName(String name)
			throws JAXBException, ClearThException, SettingsException
	{
		XmlUser originalUser = new XmlUser(), 
				newUser = new XmlUser();
		
		originalUser.setName("admin");
		newUser.setName(name);
		
		manager.addUser(originalUser);
		manager.modifyUsers(Collections.singletonList(originalUser), Collections.singletonList(newUser));
		try
		{
			Assert.assertTrue(manager.userList.stream().anyMatch(u -> u.getName().equals(name)));
		}
		finally
		{
			manager.removeUsers(Collections.singletonList(newUser));
		}
	}

	@Test(expectedExceptions = SettingsException.class, dataProvider = "invalid-names")
	public void testEditingUserWithInvalidName(String name)
			throws JAXBException, ClearThException, SettingsException
	{
		XmlUser originalUser = new XmlUser(), 
				newUser = new XmlUser();
		
		originalUser.setName("admin");
		newUser.setName(name);
		
		manager.addUser(originalUser);
		try
		{
			manager.modifyUsers(Collections.singletonList(originalUser), Collections.singletonList(newUser));
		}
		finally
		{
			manager.removeUsers(Collections.singletonList(originalUser));
		}
	}
}