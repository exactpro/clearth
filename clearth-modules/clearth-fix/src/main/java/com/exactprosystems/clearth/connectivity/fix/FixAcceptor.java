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

package com.exactprosystems.clearth.connectivity.fix;

import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;

import java.util.List;
import java.util.Map;

public class FixAcceptor extends FixApplication
{
	private static final Logger logger = LoggerFactory.getLogger(FixAcceptor.class);
	public static final String SESSION_ID = "SessionId";
	
	private ClearThSocketAcceptor acceptor;
	
	public FixAcceptor(FixConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}
	
	@Override
	protected void processConnectionSettings(FixConnectionSettings connectionSettings) throws SettingsException
	{
	}
	
	@Override
	protected void startFix(Map<String, String> settings, SessionSettings sessionSettings,
			MessageStoreFactory msgStoreFactory, LogFactory logFactory, MessageFactory msgFactory) throws ConnectionException, SettingsException
	{
		try
		{
			acceptor = new ClearThSocketAcceptor(this, msgStoreFactory, sessionSettings, logFactory, msgFactory);
			acceptor.start();
		}
		catch (Exception e)
		{
			acceptor = null;
			throw new ConnectionException(e);
		}
	}
	
	@Override
	protected void stopFix()
	{
		if (acceptor != null)
			acceptor.stop();
	}
	
	@Override
	protected void sendFixMessage(Message message, ClearThMessageMetadata metadata) throws ConnectionException
	{
		String sessionId = metadata != null ? (String) metadata.getField(SESSION_ID) : null;
		Session session = getSession(sessionId);
		session.send(message);
	}
	
	
	//FIX Application methods implementation
	@Override
	public void onCreate(SessionID sessionId)
	{
	}
	
	@Override
	public void onLogon(SessionID sessionId)
	{
		logger.debug("{} logon for {}", name, sessionId);
	}
	
	@Override
	public void onLogout(SessionID sessionId)
	{
		logger.debug("{} logout for {}", name, sessionId);
	}
	
	@Override
	public void toAdmin(Message message, SessionID sessionId)
	{
		logger.trace("{} sends admin message to {}:{}{}", name, sessionId, Utils.EOL, message);
	}
	
	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend
	{
		logger.trace("{} sends message to {}:{}{}", name, sessionId, Utils.EOL, message);
		super.toApp(message, sessionId);
	}
	
	
	private Session getSession(String id) throws ConnectionException
	{
		if (id != null)
		{
			Session result = acceptor.getSessionMap().get(new SessionID(id));
			if (result != null)
				return result;
			throw new ConnectionException("No session for ID '" + sessionID + "'");
		}
		else
		{
			List<Session> sessions = acceptor.getManagedSessions();
			for (Session s : sessions)
			{
				if (s.isLoggedOn())
					return s;
			}
		}
		
		throw new ConnectionException("No sessions available");
	}
}