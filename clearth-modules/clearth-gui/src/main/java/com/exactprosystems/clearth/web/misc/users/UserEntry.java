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

package com.exactprosystems.clearth.web.misc.users;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.Digester;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.xmldata.XmlUser;

/**
 * @author andrey.panarin
 *
 */
public class UserEntry implements Cloneable
{
	protected XmlUser xmlUser;
	protected String passwordText = "";
	
	public UserEntry(XmlUser xmlUser)
	{
		this.xmlUser = xmlUser;
	}
	
	public UserEntry(String name, String password, String role)
	{
		this.xmlUser = new XmlUser();
		this.xmlUser.setName(name);
		this.xmlUser.setPassword(password);
		this.xmlUser.setRole(role);
	}
	
	public static List<UserEntry> createList(List<XmlUser> xmlUsers)
	{
		List<UserEntry> list = new ArrayList<UserEntry>();
		for (XmlUser u : xmlUsers)
			list.add(new UserEntry(u));
		
		return list;
	}
	
	public boolean isActive()
	{
		return !UserSessionCollector.getSessionsByLogin(getName()).isEmpty();
	}
	
	public int getSessionCount()
	{
		return UserSessionCollector.getSessionsByLogin(getName()).size();
	}
	
	public Map<String, String> getUserSchedulersStatuses()
	{
		Map<String, String> result = new HashMap<String, String>();
		List<Scheduler> userSchedulers = ClearThCore.getInstance().getSchedulersManager().getUserSchedulers(getName());
		if (userSchedulers != null)
		{
			for (Scheduler currentScheduler : userSchedulers)
			{
				if (currentScheduler.isSuspended())
					result.put(currentScheduler.getName(), "Suspended");
				else if (currentScheduler.isRunning())
					result.put(currentScheduler.getName(), "Running");
				else
					result.put(null, "");
			}
		}
		return result;
	}
	
	public boolean checkUserSchedulersStatuses()
	{		
		for (String currentSchedulerStatus : getUserSchedulersStatuses().values())
		{
			if (!currentSchedulerStatus.isEmpty())
				return true;
		}
		return false;
	}
	
	public String getDefaultUserSchedulerStatus()
	{
		Scheduler userScheduler = ClearThCore.getInstance().getSchedulersManager().getDefaultUserScheduler(getName());
		if (userScheduler != null)
		{
			if (userScheduler.isSuspended())
				return "Suspended";
			if (userScheduler.isRunning())
				return "Running";
		}
		return "";
	}
	
	public String getName()
	{
		return xmlUser.getName();
	}

	public void setName(String name)
	{
		this.xmlUser.setName(name);
	}

	public String getPasswordText()
	{
		return passwordText;
	}
	
	public void setPasswordText(String text)
	{
		passwordText = text;
	}
	
	public boolean isNewPasswordDefined()
	{
		return passwordText != null && !passwordText.isEmpty();
	}
	
	public void applyPasswordText() throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		if (passwordText != null && !passwordText.isEmpty())
		{
			String md5 = Digester.stringToMD5(ClearThCore.getInstance().getSaltedText(passwordText));
			xmlUser.setPassword(md5);
		}
	}
	
	public String getPassword()
	{
		return xmlUser.getPassword();
	}

	public String getRole()
	{
		return xmlUser.getRole();
	}

	public void setRole(String role)
	{
		xmlUser.setRole(role);
	}
	
	public XmlUser getXmlUser()
	{
		return xmlUser;
	}
	
	public void killActiveSessions()
	{
		List<HttpSession> sessions = UserSessionCollector.getSessionsByLogin(getName());
		for (HttpSession s : sessions)
		{
			s.invalidate();
		}
	}
	
	public void tryStopUserSchedulers() throws AutomationException
	{
		if (ClearThCore.getInstance().isUserSchedulersAllowed())
		{
			List<Scheduler> uss = ClearThCore.getInstance().getSchedulersManager().getUserSchedulers(getName());
			if (uss == null)
				return;
			
			for (Scheduler currentScheduler : uss)
			{
				if (currentScheduler.isRunning() || currentScheduler.isSuspended())
					currentScheduler.stop();
			}
		}
	}
	
	public UserEntry clone()
	{
		UserEntry copy = new UserEntry(getName(), getPassword(), getRole());
		return copy;
	}
	
	public String toString()
	{
		return String.format("%s (%s)", getName(), getRole());
	}
}
