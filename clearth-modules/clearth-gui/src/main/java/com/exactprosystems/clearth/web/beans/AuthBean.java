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

import java.io.IOException;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.event.ComponentSystemEvent;
import javax.servlet.http.HttpSession;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.SchedulersManager;
import com.exactprosystems.clearth.web.filters.AuthenticationFilter;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.WebUtils;
import com.exactprosystems.clearth.web.misc.users.UserSessionCollector;
import com.exactprosystems.clearth.xmldata.XmlUser;

public class AuthBean extends ClearThBean
{
	public static final String AUTH_KEY = "clearth.user.data", 
			ADMIN_KEY = "clearth.user.admin", 
			ROLE_KEY = "clearth.user.role", 
			
			HOME_PAGE = "/ui/restricted/home.jsf", 
			LOGIN_PAGE = "/ui/login.jsf",
			
			USERNAME = "userName", 
			PASSWORD = "password";
	
	private static final String LOGIN_ERROR = "loginError";

	private String userName, password;
	private String requestUrl = null;

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getPassword()
	{
		return ""; // Returning password shows it on login page; that's not safe
	}

	public void setPassword(String password)
	{
		this.password = password;
	}


	protected String getHomePage()
	{
		return HOME_PAGE;
	}

	protected String getLoginPage()
	{
		return LOGIN_PAGE;
	}

	protected boolean initUserSession()
	{
		return true;
	}

	protected void invalidateUserSession()
	{
	}

	public void pullValuesFromFlash(ComponentSystemEvent e) {
		Flash flash = FacesContext.getCurrentInstance().getExternalContext().getFlash();
		userName = (String)flash.get(USERNAME);
		password = (String)flash.get(PASSWORD);
	}
	
	public void saveInput()
	{
		FacesContext facesContext = FacesContext.getCurrentInstance();
		Flash flash = facesContext.getExternalContext().getFlash();
		flash.setKeepMessages(true);
		flash.put(USERNAME, userName);
		flash.put(PASSWORD, password);
	}
	
	public String getErrorOutcome()
	{
		String result = "login?faces-redirect=true";
		return requestUrl != null ? result+"&"+AuthenticationFilter.PARAMETER+"="+requestUrl : result;
	}

	public String login()
	{
		ClearThCore app = ClearThCore.getInstance();
		XmlUser allowed = app.getUsersManager().isUserAllowed(userName, password);
		
		if (allowed == null)
		{
			MessageUtils.addMessage(FacesMessage.SEVERITY_ERROR, LOGIN_ERROR, "Authentication failed", "Incorrect username or password");
			getLogger().error("Incorrect password or unknown user '{}'", userName);
			saveInput();
			return getErrorOutcome();
		}
		
		if (allowed.getRole() == null) {
			MessageUtils.addMessage(FacesMessage.SEVERITY_ERROR, LOGIN_ERROR, "Authentication failed", "User role undefined");
			getLogger().error("User role for '{}' undefined in users.xml", allowed.getName());
			saveInput();
			return getErrorOutcome();
		}
		
		UserSessionCollector.registerCurrentSession(userName);
		
		Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
		sessionMap.remove(AUTH_KEY);
		sessionMap.remove(ADMIN_KEY);
		sessionMap.remove(ROLE_KEY);
		
		if (app.isUserSchedulersAllowed())
		{
			SchedulersManager schedulersManager = app.getSchedulersManager();
			Scheduler defaultUserScheduler = schedulersManager.getDefaultUserScheduler(userName);
			if (defaultUserScheduler == null)
			{
				try
				{
					schedulersManager.addDefaultUserScheduler(userName);
				}
				catch (Exception e)
				{
					String msg = "Could not init user scheduler";
					getLogger().error(msg, e);
					MessageUtils.addMessage(FacesMessage.SEVERITY_ERROR, LOGIN_ERROR, "Initialization failed", msg);
					return getErrorOutcome();
				}
			}
		}
		
		if (!initUserSession())
			return getErrorOutcome();
		
		sessionMap.put(AUTH_KEY, allowed.getName());
		sessionMap.put(ROLE_KEY, allowed.getRole());
		
		
		if (allowed.getRole().equals("admin"))
			sessionMap.put(ADMIN_KEY, true);
		
		initLogger();
		getLogger().info("logged in");
		
		try
		{
			String context = WebUtils.getContext();
			getLogger().trace("Context: {}", context);
			
			// If user tried to access resources when he wasn't authorized, 
			// use requestUrl from AuthenticationFilter to redirect user to that resource
			if (requestUrl != null)
			{
				FacesContext.getCurrentInstance().getExternalContext().redirect(requestUrl);
				requestUrl = null;
			}
			else
				FacesContext.getCurrentInstance().getExternalContext().redirect(context + getHomePage());
			return null;
		}
		catch (IOException e)
		{
			getLogger().error("Error on attempt to redirect after login", e);
			return getErrorOutcome();
		}
	}

	public void logout()
	{
		((HttpSession)FacesContext.getCurrentInstance().getExternalContext().getSession(false)).invalidate();

		invalidateUserSession();
		getLogger().info("logged out");

		try
		{
			String context = WebUtils.getContext();
			getLogger().trace("Context: {}", context);
			FacesContext.getCurrentInstance().getExternalContext().redirect(context + getLoginPage());
		}
		catch (IOException e)
		{
			getLogger().error("Error on attempt to redirect after logout", e);
		}
	}

	public String getRequestUrl()
	{
		return requestUrl;
	}

	public void setRequestUrl(String requestUrl)
	{
		this.requestUrl = requestUrl;
	}
}