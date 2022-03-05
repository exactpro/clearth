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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;

import com.exactprosystems.clearth.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.xmldata.XmlUser;
import com.exactprosystems.clearth.xmldata.XmlUsers;

public class UsersManager
{
	private static final Logger logger = LoggerFactory.getLogger(UsersManager.class);
	
	protected volatile XmlUsers usersList;
	protected volatile Date usersListDate;
	protected final Object usersListMonitor = new Object(); // Required as usersList is reset on reload
	
	public void init() throws JAXBException, IOException
	{
		loadUsersList();
	}
	
	public void uploadUsersList(File uploadedFile, String originalFileName) throws JAXBException, IOException
	{
		synchronized (usersListMonitor)
		{
			usersList = loadUsers(uploadedFile.getAbsolutePath());
			saveUsersList();
			usersListDate = new Date();
		}
	}
	
	public void loadUsersList() throws JAXBException, IOException
	{
		synchronized (usersListMonitor)
		{
			usersList = loadUsers(ClearThCore.usersListPath());
			usersListDate = new Date();
		}
	}
	
	public void saveUsersList() throws JAXBException
	{
		saveUsersList(ClearThCore.usersListPath());
	}

	protected void validateName(String name) throws SettingsException
	{
		NameValidator.validate(name);
	}

	public void addUser(XmlUser user) throws JAXBException, IOException, SettingsException
	{
		validateName(user.getName());
		
		synchronized (usersListMonitor)
		{
			loadUsersList();
			usersList.getUsers().add(user);
			try
			{
				saveUsersList();
			}
			catch (Exception e)
			{
				String msg = "Error while saving new user";
				logger.error(msg, e);
				
				String detailedMsg = msg + ": " + e.getMessage();
				throw new IOException(detailedMsg, e);
			}
			loadUsersList();
		}
	}
	
	public void modifyUsers(List<XmlUser> originalUsers, List<XmlUser> newUsers)
			throws JAXBException, IOException, ClearThException, SettingsException
	{
		synchronized (usersListMonitor)
		{
			loadUsersList();
			for (int i = 0; i < newUsers.size(); i++)
				doModifyUser(originalUsers.get(i), newUsers.get(i));
			
			try
			{
				saveUsersList();
			}
			catch (Exception e)
			{
				String msg = "Error while saving users list after modifying it";
				logger.error(msg, e);
				throw new IOException(msg, e);
			}
			loadUsersList();
		}
	}
	
	public void removeUsers(List<XmlUser> users) throws ClearThException, JAXBException, IOException
	{
		synchronized (usersListMonitor)
		{
			loadUsersList();
			
			try
			{
				for (XmlUser u : users)
				{
					int userIndex = getUserIndexByName(u.getName());
					if (userIndex >= 0)
						usersList.getUsers().remove(userIndex);
					else
						throw new ClearThException("User '"+u.getName()+"' does not exist");
				}
			}
			finally
			{
				try
				{
					saveUsersList();
				}
				catch (Exception e)
				{
					String msg = "Error while saving users list after users removal";
					logger.error(msg, e);
					throw new IOException(msg, e);
				}
				loadUsersList();
			}
		}
	}

	public XmlUser isUserAllowed(String userName, String password)
	{
		if (usersList == null)
		{
			logger.error("No users defined");
			return null;
		}
		
		for (XmlUser user : usersList.getUsers())
		{
			if (userName.equals(user.getName()) && isValidPassword(password, user.getPassword()))
				return user;
		}
		return null;
	}
	
	
	public XmlUsers getUsers()
	{
		return usersList;
	}
	
	public Date getUsersListDate()
	{
		return usersListDate;
	}
	

	protected XmlUsers loadUsersFromFile(String usersFileName) throws JAXBException, IOException
	{
		return XmlUtils.unmarshalObject(XmlUsers.class, usersFileName);
	}
	
	protected UsersRepairer createUsersRepairer()
	{
		return new UsersRepairer();
	}

	protected XmlUsers loadUsers(String usersFileName) throws JAXBException, IOException
	{
		try
		{
			return loadUsersFromFile(usersFileName);
		}
		catch (UnmarshalException e)
		{
			logger.warn("Error occured while loading users list from '{}' file", usersFileName, e);
			return repairUsers(usersFileName);
		}
	}
	
	protected XmlUsers repairUsers(String usersFileName)
	{
		logger.info("Users list file might be broken. Trying to fix its contents...");
		UsersRepairer repairer = createUsersRepairer();
		if (!repairer.repairUsersFile(usersFileName))
			return new XmlUsers();
		
		logger.info("Config file has been fixed");
		//We have fixed file contents. Trying to load users data again
		try
		{
			return loadUsersFromFile(usersFileName);
		}
		catch (Exception e2)
		{
			logger.warn("Error occured while loading users list from fixed file", e2);
			logger.info("Resetting users list to empty state...");
			repairer.resetUsersFile(usersFileName);  //Resetting file to empty to avoid "fixing" it next time thus re-writing original file backup
		}
		return new XmlUsers();
	}

	protected void saveUsersList(String usersFileName) throws JAXBException
	{
		XmlUtils.marshalObject(usersList, usersFileName);
	}
	
		
	protected void doModifyUser(XmlUser originalUser, XmlUser newUser) throws ClearThException, SettingsException {
		int userIndex = getUserIndexByName(originalUser.getName());
		if (userIndex < 0)
		{
			String msg = "User '"+originalUser.getName()+"' doesn't exist, cannot modify";
			logger.error(msg);
			throw new ClearThException(msg);
		}
		
		validateName(newUser.getName());
		
		usersList.getUsers().set(userIndex, newUser);
	}

	
	private int getUserIndexByName(String userName)
	{
		for (int i = 0; i < usersList.getUsers().size(); i++)
		{
			if (userName.equals(usersList.getUsers().get(i).getName()))
				return i;
		}
		return -1;
	}
	
	private boolean isValidPassword(String input, String expected)
	{
		try
		{
			if (expected != null)
				return expected.equals(Digester.stringToMD5(ClearThCore.getInstance().getSaltedText(input)));
			logger.error("Password for this user is undefined");
		}
		catch (NoSuchAlgorithmException | UnsupportedEncodingException e)
		{
			logger.error("Password encryption failed", e);
		}

		return false;
	}
}
