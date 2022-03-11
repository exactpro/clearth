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

package com.exactprosystems.clearth.web.beans;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;

import com.exactprosystems.clearth.web.misc.*;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.UsersManager;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.web.misc.users.UserEntry;
import com.exactprosystems.clearth.xmldata.XmlUser;

public class UserManagementBean extends ClearThBean
{
	private final UsersManager manager = ClearThCore.getInstance().getUsersManager();
	private final List<UserEntry> selectedUsers = new ArrayList<>();
	private List<UserEntry> originalSelectedUsers = null;
	private final UserPropsToEdit userProps;

	private List<UserEntry> users;
	private Instant lastUpdated;

	public UserManagementBean()
	{
		userProps = createUserPropsToEdit();
	}
	
	
	protected UserPropsToEdit createUserPropsToEdit()
	{
		return new UserPropsToEdit();
	}
	
	public UserPropsToEdit getUserProps()
	{
		return userProps;
	}
	
	
	public SelectItem[] getUsersList()
	{
		List<UserEntry> users = getUsers();
		SelectItem[] result = new SelectItem[users.size()];
		int i = -1;
		for (UserEntry u : users)
		{
			i++;
			result[i] = new SelectItem(u.getName());
		}
		return result;
	}

	public List<UserEntry> getUsers()
	{
		if(isUsersListOutdated())
		{
			lastUpdated = Instant.now();
			users = UserEntry.createList(manager.getUsers());
		}

		return users;
	}

	public boolean isUsersListOutdated()
	{
		if (lastUpdated == null)
			return true;
		Instant xmlListDate = manager.getUserListDate();
		return xmlListDate.isAfter(lastUpdated);
	}

	@PostConstruct
	private void init()
	{
		newUser();
	}

	public List<UserEntry> getSelectedUsers()
	{
		return selectedUsers;
	}

	public void setSelectedUsers(List<UserEntry> selectedUsers)
	{
		this.originalSelectedUsers = selectedUsers;
		this.selectedUsers.clear();
		for (UserEntry u : originalSelectedUsers)
			this.selectedUsers.add(u.clone());
	}
	
	public UserEntry getOneSelectedUser()
	{
		if (selectedUsers.isEmpty())
			return null;
		return selectedUsers.get(0);
	}
	
	public void setOneSelectedUser(UserEntry selectedUser)
	{
		setSelectedUsers(Collections.singletonList(selectedUser));
	}
	
	public boolean isOneUserSelected()
	{
		return selectedUsers.size() == 1;
	}
	
	
	protected void resetUsersSelection()
	{
		originalSelectedUsers = null;
		selectedUsers.clear();
	}
	

	private void warnUserExists(UserEntry user)
	{
		MessageUtils.addWarningMessage("Warning", "Name '" + user.getName() + "' is already used");
	}

	public void newUser()
	{
		resetUsersSelection();
		
		selectedUsers.add(new UserEntry("", "", "user"));
	}
	
	protected void editUserProps(UserEntry userToEdit, UserEntry changes, UserEntry original, UserPropsToEdit propsToEdit)
	{
		userToEdit.setPasswordText(propsToEdit.isPassword() ? changes.getPasswordText() : original.getPasswordText());
		userToEdit.setRole(propsToEdit.isRole() ? changes.getRole() : original.getRole());
	}

	protected List<XmlUser> userEntriesToXmlUsers(List<UserEntry> users)
	{
		List<XmlUser> result = new ArrayList<>();
		for (UserEntry u : users)
			result.add(u.getXmlUser());
		return result;
	}

	public void saveUsers()
	{
		if (!canInteractWithUserList())
			return;
		
		UserEntry firstUser = getOneSelectedUser();
		if (selectedUsers.size() > 1)  //If multiple users are edited, JSF changes only the first one. Need to apply changes to all other selected users. User name shouldn't be changed here!
		{
			//We get here only if multiple users are edited, thus originalSelectedUser can't be null
			UserEntry originalFirstUser = originalSelectedUsers.get(0);
			editUserProps(firstUser, firstUser, originalFirstUser, userProps);  //Restoring properties that shouldn't be edited, because JSF has changed all the properties of the first user
			for (int i = 1; i < selectedUsers.size(); i++)
			{
				UserEntry u = selectedUsers.get(i);
				editUserProps(u, firstUser, u, userProps);
			}
		}
			
		boolean isNewUser = originalSelectedUsers == null;
		boolean isNewPasswordDefined = firstUser.isNewPasswordDefined();
		boolean canClose = (firstUser.getName() != null) && (!firstUser.getName().isEmpty())
				&& (!isNewUser || isNewPasswordDefined);
		if (!canClose)
		{
			MessageUtils.addErrorMessage("Cannot save user", "Not all of the mandatory fields are filled in");
			WebUtils.addCanCloseCallback(canClose);
			return;
		}
		
		try
		{
			if (isNewUser)
			{
				if (manager.isUserExists(firstUser.getName()))
				{
					warnUserExists(firstUser);
					WebUtils.addCanCloseCallback(false);
					return;
				}

				firstUser.applyPasswordText();
				manager.addUser(firstUser.getXmlUser());
				getLogger().info("created user '" + firstUser.getName() + "'");
			}
			else
			{
				if (!originalSelectedUsers.get(0).getName().equals(firstUser.getName()) && manager.isUserExists(firstUser.getName()))
				{
					warnUserExists(firstUser);
					WebUtils.addCanCloseCallback(false);
					return;
				}

				if (isNewPasswordDefined)
					for (UserEntry u : selectedUsers)
						u.applyPasswordText();
				manager.modifyUsers(userEntriesToXmlUsers(originalSelectedUsers), userEntriesToXmlUsers(selectedUsers));
				
				if (selectedUsers.size() == 1)
					getLogger().info("modified user '"+firstUser.getName()+"'");
				else
				{
					CommaBuilder cb = new CommaBuilder();
					for (UserEntry u : selectedUsers)
						cb.append("'"+u.getName()+"'");
					getLogger().info("modified users {}", cb);
				}
			}
		}
		catch (Exception e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
			canClose = false;
		}
		WebUtils.addCanCloseCallback(canClose);
	}

	public void removeUsers()
	{
		if (!canInteractWithUserList())
			return;
		
		try
		{
			manager.removeUsers(userEntriesToXmlUsers(originalSelectedUsers));
			if (originalSelectedUsers.size() == 1)
				getLogger().info("removed user '"+originalSelectedUsers.get(0).getName()+"'");
			else
			{
				CommaBuilder cb = new CommaBuilder();
				for (UserEntry u : originalSelectedUsers)
					cb.append("'"+u.getName()+"'");
				getLogger().info("removed users {}", cb);
			}
		}
		catch (Exception e)
		{
			getLogger().error("Failed to remove selected users", e);
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		finally
		{
			resetUsersSelection();
		}
	}

	public void uploadConfiguration(FileUploadEvent event)
	{
		if (!canInteractWithUserList())
			return;
		
		UploadedFile file = event.getFile();
		if ((file == null) || (file.getContents().length == 0))
			return;

		try
		{
			File storedFile = WebUtils.storeUploadedFile(file, new File(ClearThCore.automationStoragePath()),
					"userconfig_", ".xml");

			manager.uploadUsersList(storedFile, new File(file.getFileName()).getName());
			getLogger().info("uploaded users configuration '" + file.getFileName() + "'");
		}
		catch (Exception e)
		{
			WebUtils.logAndGrowlException("Error while working with users configuration from file " + file.getFileName(), 
					e, getLogger());
		}
	}

	public boolean isActiveUsersSelected()
	{
		for (UserEntry user : selectedUsers)
		{
			if (user.getSessionCount() > 0 || user.checkUserSchedulersStatuses())
				return true;
		}
		return false;
	}

	public void killUsers()
	{
		if (!canInteractWithUserList())
			return;

		for (UserEntry user : selectedUsers)
		{
			try
			{
				user.tryStopUserSchedulers();
				user.killActiveSessions();
				getLogger().info("killed user '" + user.getName() + "'");
			}
			catch (AutomationException e)
			{
				getLogger().error("failed to kill user '"+user.getName()+"'", e);
				MessageUtils.addErrorMessage("Error", e.getMessage());
			}
		}
	}

	public boolean canInteractWithUserList()
	{
		if (UserInfoUtils.isAdmin())
			return true;
		MessageUtils.addErrorMessage("Error", "You have no access to User Management actions");
		return false;
	}
	
	
	public StreamedContent downloadConfig()
	{
		try
		{
			return WebUtils.downloadFile(new File(ClearThCore.usersListPath()));
		}
		catch (IOException e)
		{
			WebUtils.logAndGrowlException("Could not download user list", e, getLogger());
			return null;
		}
	}
}
