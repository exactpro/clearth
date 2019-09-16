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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author andrey.panarin
 *
 */
public class UserSessionCollector
{
	private static final Logger logger = LoggerFactory.getLogger(UserSessionCollector.class);
	
	private static final Map<String, List<HttpSession>> sessionsByLogin = new HashMap<String, List<HttpSession>>();
	private static final Map<String, String> loginBySessionId = new HashMap<String, String>();
	
	private UserSessionCollector() {}
	
	public static List<HttpSession> getSessionsByLogin(String login)
	{
		List<HttpSession> sessions;
		synchronized (UserSessionCollector.class)
		{
			sessions = sessionsByLogin.get(login);
		}
		if (sessions == null)
			return Collections.emptyList();
		
		return new ArrayList<HttpSession>(sessions);
	}
	
	public static void registerCurrentSession(String login)
	{
		HttpSession currentSession = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
		String currentSessionId = currentSession.getId();
		
		synchronized (UserSessionCollector.class)
		{
			loginBySessionId.put(currentSessionId, login);
			
			List<HttpSession> sessions = sessionsByLogin.get(login);
			if (sessions == null)
			{
				sessions = new ArrayList<HttpSession>();
				sessionsByLogin.put(login, sessions);
			}
			sessions.add(currentSession);
		}
		
		logger.debug("Session '{}' has been registered as '{}'", currentSessionId, login);
	}
	
	public static void removeSession(HttpSession session)
	{
		synchronized (UserSessionCollector.class)
		{
			String sessionId = session.getId();
			String sessionLogin = loginBySessionId.remove(sessionId);
			
			List<HttpSession> sessionsForThisLogin = sessionsByLogin.get(sessionLogin);
			if (sessionsForThisLogin != null)
			{
				sessionsForThisLogin.remove(session);
			}
		}
	}
}
