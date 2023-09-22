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

package com.exactprosystems.clearth.web.misc.users;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSessionCollector
{
	private static final Logger logger = LoggerFactory.getLogger(UserSessionCollector.class);
	
	private static final ReadWriteLock lock = new ReentrantReadWriteLock();
	private static final Lock readLock = lock.readLock(),
			writeLock = lock.writeLock();
	
	private static final Map<String, Set<HttpSession>> sessionsByLogin = new LinkedHashMap<>();
	private static final Map<String, String> loginBySessionId = new HashMap<>();
	
	private UserSessionCollector() {}
	
	public static List<String> getLogins()
	{
		readLock.lock();
		try
		{
			return new ArrayList<>(sessionsByLogin.keySet());
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	public static List<HttpSession> getSessionsByLogin(String login)
	{
		readLock.lock();
		try
		{
			Set<HttpSession> sessions = sessionsByLogin.get(login);
			return sessions == null ? Collections.emptyList() : new ArrayList<>(sessions);
		}
		finally
		{
			readLock.unlock();
		}
	}
	
	public static void registerCurrentSession(String login)
	{
		HttpSession currentSession = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
		registerSession(login, currentSession);
	}
	
	public static void registerSession(String login, HttpSession session)
	{
		String sessionId = session.getId();
		writeLock.lock();
		try
		{
			loginBySessionId.put(sessionId, login);
			
			Set<HttpSession> allSessions = sessionsByLogin.computeIfAbsent(login, l -> new HashSet<>());
			allSessions.add(session);
			
			logger.debug("Session '{}' has been registered for '{}'", sessionId, login);
		}
		finally
		{
			writeLock.unlock();
		}
	}
	
	public static void removeSession(HttpSession session)
	{
		writeLock.lock();
		try
		{
			String sessionId = session.getId();
			String sessionLogin = loginBySessionId.remove(sessionId);
			
			Set<HttpSession> sessionsForThisLogin = sessionsByLogin.get(sessionLogin);
			if (sessionsForThisLogin != null)
			{
				sessionsForThisLogin.remove(session);
				if (sessionsForThisLogin.isEmpty())
					sessionsByLogin.remove(sessionLogin);
			}
			
			logger.debug("Session '{}' of '{}' has been unregistered", sessionId, sessionLogin);
		}
		finally
		{
			writeLock.unlock();
		}
	}
}
