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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.MsgType;
import quickfix.field.Password;
import quickfix.field.Username;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class FixInitiator extends FixApplication
{
	private static final Logger logger = LoggerFactory.getLogger(FixInitiator.class);
	
	protected int waitForLogon;
	protected String username, password;
	protected int seqNumSender, seqNumTarget;
	protected Initiator initiator;
	protected Session session;
	protected CompletableFuture<Void> logonFuture;
	protected AtomicBoolean needLogoutMessage;
	
	public FixInitiator(FixConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}
	
	@Override
	protected void processConnectionSettings(FixConnectionSettings connectionSettings) throws SettingsException
	{
		waitForLogon = connectionSettings.getWaitForLogon();
		username = connectionSettings.getUsername();
		password = connectionSettings.getPassword();
		seqNumSender = connectionSettings.getSeqNumSender();
		seqNumTarget = connectionSettings.getSeqNumTarget();
	}
	
	@Override
	protected void startFix(Map<String, String> fixSettings, SessionSettings sessionSettings, 
			MessageStoreFactory msgStoreFactory, LogFactory logFactory, MessageFactory msgFactory) throws ConnectionException, SettingsException
	{
		//These two must be initialized here, not as final fields of FixInitiator, 
		//because startFix() is called from parent's constructor when final fields are not initialized yet
		logonFuture = new CompletableFuture<>();
		needLogoutMessage = new AtomicBoolean(false);
		
		try
		{
			initiator = new SocketInitiator(this, msgStoreFactory, sessionSettings, logFactory, msgFactory);
			initiator.start();
		}
		catch (Exception e)
		{
			throw new ConnectionException(e);
		}
		
		session = Session.lookupSession(sessionID);
		setSeqNums(session);
		sendLogonAndWait();
	}
	
	@Override
	protected void stopFix()
	{
		if (initiator == null)
			return;
		
		logonFuture.cancel(true);  //Canceling wait if it is not finished yet
		
		if (needLogoutMessage.get())
		{
			logger.debug("Sending logout");
			needLogoutMessage.set(false);  //We are sending logout message on our own, no need to handle logout in onLogout()
			session.logout();
		}
		else
			logger.debug("Already logged out");
		
		initiator.stop();
	}
	
	@Override
	protected void sendFixMessage(Message message, ClearThMessageMetadata metadata)
	{
		session.send(message);
	}
	
	
	protected void setSeqNums(Session session) throws ConnectionException
	{
		try
		{
			if (seqNumSender > -1)
				session.setNextSenderMsgSeqNum(seqNumSender);
			if (seqNumTarget > -1)
				session.setNextTargetMsgSeqNum(seqNumTarget);
		}
		catch (IOException e)
		{
			throw new ConnectionException("Error while setting sequence number", e);
		}
	}
	
	protected void sendLogonAndWait() throws ConnectionException
	{
		session.logon();
		try
		{
			logonFuture.get(waitForLogon, TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException e)
		{
			if (!session.isLoggedOn())
				throw new ConnectionException("Could not connect to target server: server didn't respond to logon in "+waitForLogon+" millisecond(s)");
		}
		catch (Exception e)
		{
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
			
			throw new ConnectionException("Could not connect to target server", e);
		}
	}
	
	
	//FIX Application methods implementation
	@Override
	public void onCreate(SessionID sessionID)
	{
	}
	
	@Override
	public void onLogon(SessionID sessionID)
	{
		logger.trace("{} logon", name);
		needLogoutMessage.set(true);
		logonFuture.complete(null);
	}
	
	@Override
	public void onLogout(SessionID sessionID)
	{
		logger.trace("{} logout", name);
		if (!needLogoutMessage.get())
			return;
		
		needLogoutMessage.set(false);  //Logged out by message from server, no need to send logout message from stopFix() when stopOwner() triggers it
		
		//Separating stopper thread from connection thread (this one) to prevent the following deadlock:
		//1. thread in QuickFIX/J waits for finish of onLogout();
		//2. onLogout() is blocked till stopOwner() completes;
		//3. stopOwner() is blocked till thread in QuickFIX/J finishes
		new Thread(() -> stopOwner(), name+" (connection stopper)").start();
	}
	
	@Override
	public void toAdmin(Message message, SessionID sessionID)
	{
		if (!StringUtils.isEmpty(username))
		{
			try
			{
				String messageType = getMessageType(message);
				if (isNeedAuth(messageType))
					addAuthFields(message);
			}
			catch (Exception e)
			{
				logger.warn("Error while trying to enrich message with username and password", e);
			}
		}
		
		logger.trace("{} sends admin message:{}{}", name, Utils.EOL, message);
	}
	
	@Override
	public void toApp(Message message, SessionID sessionID) throws DoNotSend
	{
		logger.trace("{} sends message:{}{}", name, Utils.EOL, message);
		super.toApp(message, sessionID);
	}
	
	
	@Override
	protected DataDictionary createTransportDictionary() throws ConnectivityException
	{
		try
		{
			return session.getDataDictionary();  //Real dictionary used by session is preferable
		}
		catch (Exception e)
		{
			logger.warn("Could not get session default data dictionary to use as transport dictionary: {}", e.getMessage());
			try
			{
				return session.getDataDictionaryProvider().getSessionDataDictionary(beginString);
			}
			catch (Exception e1)
			{
				logger.warn("No dictionaries available in session: {}", e1.getMessage());
				return super.createTransportDictionary();
			}
		}
	}
	
	@Override
	protected DataDictionary createAppDictionary() throws ConnectivityException
	{
		try
		{
			return session.getDataDictionary();  //Real dictionary used by session is preferable
		}
		catch (Exception e)
		{
			logger.warn("Could not get session default data dictionary to use as app dictionary: {}", e.getMessage());
			return super.createAppDictionary();
		}
	}
	
	
	protected String getMessageType(Message message) throws FieldNotFound
	{
		return message.getHeader().getString(MsgType.FIELD);
	}
	
	protected boolean isNeedAuth(String messagType)
	{
		return MsgType.LOGON.equals(messagType);
	}
	
	protected void addAuthFields(Message message)
	{
		message.setString(Username.FIELD, username);
		message.setString(Password.FIELD, password);
	}
	
	
	private void stopOwner()
	{
		try
		{
			owner.stop();
		}
		catch (Exception e)
		{
			logger.error("Error while stopping connection after logout", e);
		}
	}
}
