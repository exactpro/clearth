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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	private final Lock lock = new ReentrantLock();

	protected Map<String, XmlUser> userMap;
	protected List<XmlUser> userList;
	protected volatile Instant userListDate;

	public void init() throws JAXBException, IOException
	{
		loadUsers(getXmlUsers(ClearThCore.usersListPath()));
		userListDate = Instant.now();
	}
	
	public void uploadUsersList(File uploadedFile, String originalFileName)
			throws JAXBException, IOException
	{
		lock.lock();
		try
		{
			loadUsers(getXmlUsers(uploadedFile.getAbsolutePath()));
			saveUsersList();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	protected void loadUsers(XmlUsers xmlUsers)
	{
		lock.lock();
		try
		{
			userList = new CopyOnWriteArrayList<>(xmlUsers.getUsers());
			userList.sort((o1,o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
			userMap = new ConcurrentHashMap<>();

			fillUserMap();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	private void saveUsersList() throws JAXBException
	{
		lock.lock();
		try
		{
			userListDate = Instant.now();
			saveUsersList(ClearThCore.usersListPath());
		}
		finally
		{
			lock.unlock();
		}
	}

	public void addUser(XmlUser user) throws SettingsException, ClearThException, JAXBException
	{
		lock.lock();
		try
		{
			validateUser(user);
			
			if(isUserExists(user.getName()))
				throw new ClearThException("User already exists");

			userMap.put(user.getName(), user);
			addToList(user);

			saveUsersList();
		}
		catch (JAXBException e)
		{
			String msg = "Error while saving new user";
			logger.error(msg, e);
			
			String detailedMsg = msg + ": " + e.getMessage();
			throw new JAXBException(detailedMsg, e);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public void modifyUsers(List<XmlUser> originalUsers, List<XmlUser> newUsers)
			throws JAXBException, ClearThException, SettingsException
	{
		lock.lock();
		try
		{
			try
			{
				for(int i=0; i<newUsers.size(); i++)
					doModifyUser(originalUsers.get(i), newUsers.get(i));
			}
			finally
			{
				saveUsersList();
			}
		}
		catch (JAXBException e)
		{
			String msg = "Error while saving users list after modifying it";
			logger.error(msg, e);
			
			String detailedMsg = msg + ": " + e.getMessage();
			throw new JAXBException(detailedMsg, e);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	public void removeUsers(List<XmlUser> users) throws ClearThException, JAXBException
	{
		lock.lock();
		try
		{
			try
			{
				for(XmlUser user : users)
				{
					int index = getUserIndexByName(user.getName());

					userMap.remove(user.getName());
					userList.remove(index);
				}
			}
			finally
			{
				saveUsersList();
			}
		}
		catch (JAXBException e)
		{
			String msg = "Error while saving users list after users removal";
			logger.error(msg, e);
			
			String detailedMsg = msg + ": " + e.getMessage();
			throw new JAXBException(detailedMsg, e);
		}
		finally
		{
			lock.unlock();
		}
	}

	public XmlUser isUserAllowed(String userName, String password)
	{
		if (userMap.isEmpty())
		{
			logger.error("No users defined");
			return null;
		}

		XmlUser user = userMap.get(userName);

		if (user!=null && isValidPassword(password, user.getPassword()))
			return user;

		return null;
	}

	public boolean isUserExists(String userName)
	{
		return userMap.containsKey(userName);
	}

	public List<XmlUser> getUsers()
	{
		return Collections.unmodifiableList(userList);
	}

	public Instant getUserListDate()
	{
		return userListDate;
	}

	protected XmlUsers loadUsersFromFile(String usersFileName) throws JAXBException, IOException
	{
		return XmlUtils.unmarshalObject(XmlUsers.class, usersFileName);
	}
	
	protected UsersRepairer createUsersRepairer()
	{
		return new UsersRepairer();
	}

	protected XmlUsers getXmlUsers(String usersFileName) throws JAXBException, IOException
	{
		try
		{
			return loadUsersFromFile(usersFileName);
		}
		catch (UnmarshalException e)
		{
			logger.warn("Error occurred while loading users list from '{}' file", usersFileName, e);
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
			logger.warn("Error occurred while loading users list from fixed file", e2);
			logger.info("Resetting users list to empty state...");
			repairer.resetUsersFile(usersFileName);  //Resetting file to empty to avoid "fixing" it next time thus re-writing original file backup
		}
		return new XmlUsers();
	}

	protected void saveUsersList(String usersFileName) throws JAXBException
	{
		XmlUtils.marshalObject(createXmlUsers(), usersFileName);
	}

	protected void doModifyUser(XmlUser originalUser, XmlUser newUser)
			throws ClearThException, SettingsException
	{
		int index = getUserIndexByName(originalUser.getName());
		
		validateUser(newUser);
		
		userMap.remove(originalUser.getName(), originalUser);
		userMap.put(newUser.getName(), newUser);
		userList.set(index, newUser);
	}
	
	protected void validateUser(XmlUser user) throws SettingsException
	{
		NameValidator.validate(user.getName());
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

	private int getUserIndexByName(String userName) throws ClearThException
	{
		XmlUser user = userMap.get(userName);

		if(user == null)
			throw new ClearThException("User '" + userName + "' does not exist");

		return userList.indexOf(user);
	}

	private void fillUserMap()
	{
		userList.forEach(user -> userMap.put(user.getName(), user));
	}

	private XmlUsers createXmlUsers()
	{
		lock.lock();
		try
		{
			XmlUsers xmlUsers = new XmlUsers();

			List<XmlUser> temp = xmlUsers.getUsers();
			temp.addAll(userList);

			return xmlUsers;
		}
		finally
		{
			lock.unlock();
		}
	}

	private void addToList(XmlUser user)
	{
		Comparator<XmlUser> comp = (o1,o2) -> o1.getName().compareToIgnoreCase(o2.getName());
		int to = Math.abs(Collections.binarySearch(userList, user, comp))-1;
		userList.add(to, user);
	}
}
