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

package com.exactprosystems.clearth.web.misc;

import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.web.misc.users.UserSessionCollector;

/**
 * @author andrey.panarin
 *
 */
public class SessionEventListener implements HttpSessionListener, EventListener
{	
	private static final Logger logger = LoggerFactory.getLogger(SessionEventListener.class);

	@Override
	public void sessionCreated(HttpSessionEvent event)
	{
		logger.debug("Created a new session '{}'", event.getSession().getId());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event)
	{
		HttpSession destroyedSession = event.getSession();
		String destroyedSessionId = destroyedSession.getId();
		logger.debug("Destroyed session '{}'", destroyedSessionId);
		
		UserSessionCollector.removeSession(destroyedSession);
	}
	
	
}
