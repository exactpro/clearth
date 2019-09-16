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

package com.exactprosystems.clearth.connectivity;

import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;

import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;

import static com.exactprosystems.clearth.connectivity.MQExceptionUtils.isConnectionBroken;
import static org.apache.commons.lang.StringUtils.isWhitespace;

public abstract class MQClient extends BasicClearThClient implements ClearThClient
{
	public static final int DEFAULT_PORT = 1414;
	public static final String DEFAULT_HOST = "localhost";
	
	protected MQQueueManager sendQueueManager,
			receiveQueueManager;
	protected MQQueue receiveQueue;
	protected MQQueue sendQueue;
	
	public MQClient(MQConnection owner) throws ConnectionException, SettingsException
	{
		super(owner);
	}
	
	
	protected Hashtable createManagerProperties(String hostname, int port, String channel)
	{
		Hashtable properties = new Hashtable();
		properties.put(MQConstants.TRANSPORT_PROPERTY, MQConstants.TRANSPORT_MQSERIES);
		properties.put(MQConstants.HOST_NAME_PROPERTY, isWhitespace(hostname) ? DEFAULT_HOST : hostname);
		properties.put(MQConstants.PORT_PROPERTY, port < 0 ? DEFAULT_PORT : port);
		properties.put(MQConstants.CHANNEL_PROPERTY, channel == null ? "" : channel);
		return properties;
	}

	protected MQQueueManager createManager(String hostname, int port, String managerName, String channel) throws MQException
	{
		Hashtable properties = createManagerProperties(hostname, port, channel);
		return new MQQueueManager(managerName, properties);
	}
	
	protected MQQueueManager getManagerAccess(String hostname, String managerName, int port, String channel) throws MQException
	{
		return createManager(hostname, port, managerName, channel);
	}

	protected void disconnectFromManager(MQQueueManager manager) throws MQException
	{
		manager.disconnect();
	}
	
	protected void connectToSendingManager() throws MQException
	{
		sendQueueManager = getManagerAccess(storedSettings.hostname, storedSettings.queueManager, storedSettings.port, storedSettings.channel);
	}
	
	protected void connectToReceivingManager() throws MQException
	{
		receiveQueueManager = getManagerAccess(storedSettings.hostname, storedSettings.queueManager, storedSettings.port, storedSettings.channel);
	}
	
	protected void connectToSendQueue() throws MQException
	{
		sendQueue = sendQueueManager.accessQueue(storedSettings.sendQueue, CMQC.MQOO_OUTPUT);
	}
	
	protected void connectToReceiveQueue() throws MQException
	{
		receiveQueue = receiveQueueManager.accessQueue(storedSettings.receiveQueue, CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_INQUIRE);
	}
	
	protected void closeConnections() throws ConnectionException
	{
		try
		{
			if (sendQueue != null)
			{
				getLogger().trace(name + ": closing send queue");
				sendQueue.close();
				sendQueue = null;
			}
			
			if (receiveQueue != null)
			{
				getLogger().trace(name + ": closing receive queue");
				receiveQueue.close();
				receiveQueue = null;
			}
			
			getLogger().trace(name + ": disconnecting from queue managers");
			if (sendQueueManager != null)
			{
				disconnectFromManager(sendQueueManager);
				sendQueueManager = null;
			}
			
			if (receiveQueueManager != null)
			{
				disconnectFromManager(receiveQueueManager);
				receiveQueueManager = null;
			}
		}
		catch (MQException e)
		{
			throw new ConnectionException(e);
		}
	}
	
	@Override
	protected void connect() throws ConnectionException, SettingsException
	{
		Logger logger = getLogger();
		if ((storedSettings.queueManager == null) || (storedSettings.queueManager.isEmpty()))
		{
			if (logger.isInfoEnabled())
				logger.info(name+": queue manager not specified, sending and receiving messages from queue is not available, "
						+ "but messages still can be processed according to listeners settings");
			return;
		}
		
		boolean sendingConnection = (storedSettings.sendQueue!=null) && (!storedSettings.sendQueue.trim().isEmpty()),
				receivingConnection = storedSettings.useReceiveQueue;
		try
		{
			//Connecting to one manager using two separate objects in order to avoid locking of message sending while receiveQueue.get() is waiting for message to arrive
			if (sendingConnection)
				connectToSendingManager();
			if (receivingConnection)
				connectToReceivingManager();
				
			logger.trace(name+": connected to queue manager '" + storedSettings.queueManager + "'");
			
			if ((storedSettings.receiveQueue!=null) && (!storedSettings.receiveQueue.isEmpty()))
			{
				if (receivingConnection)
				{
					connectToReceiveQueue();
					logger.trace(name+": connected to receive queue '"+storedSettings.receiveQueue+"'");
				}
				else
					logger.trace(name+": receive queue name specified, but messages receiving is disabled by 'Use receive queue' setting");
			}
			else
				logger.trace(name+": receive queue name not specified, receiving messages is not available");
			
			if (sendingConnection)
			{
				connectToSendQueue();
				logger.trace(name+": connected to send queue '" + storedSettings.sendQueue + "'");
			}
			else
				logger.trace(name+": send queue name not specified, messages sending is not available");
		}
		catch (MQException e)
		{
			throw new ConnectionException(e);
		}
	}
	
	@Override
	protected boolean isNeedReceiverThread()
	{
		return receiveQueue != null;
	}
	
	@Override
	protected boolean isConnectionBrokenError(Throwable error)
	{
		if (!(error instanceof MQException))
			return false;
		
		return isConnectionBroken((MQException)error);
	}
	
	
	public MQPutMessageOptions createPutMessageOptions(MQMessage msg)
	{
		return new MQPutMessageOptions();
	}
	
	@Override
	public String sendMessage(String message) throws IOException, ConnectivityException
	{
		synchronized (sendMonitor)
		{
			if (sendQueue == null)
				throw new ConnectionException("Send queue not specified for '" + name + "'");
			
			Logger logger = getLogger();
			MQMessage msg = prepareMQMessage(message);
			if (logger.isTraceEnabled())
				logger.trace(name+" sends message: "+Utils.EOL+message+Utils.EOL+"header: "+Utils.EOL+messageHeaderToString(msg));
			else if (logger.isDebugEnabled())
				logger.debug(name+" sends message: "+Utils.EOL+message);
			
			try
			{
				sendQueue.put(msg, createPutMessageOptions(msg));
				if (logger.isTraceEnabled())
					logger.trace(name+" has sent message successfully");
				incSent();
			}
			catch (MQException e)
			{
				handleSendError(e, message);
			}
			return message;
		}
	}
	
	public void sendByteMessage(byte[] raw) throws IOException, MQException
	{
		Logger logger = getLogger();
		MQMessage msg = prepareMQMessage(raw);
		if (logger.isTraceEnabled())
			logger.trace(name+" sends message: "+Utils.EOL+new String(raw)+Utils.EOL+"header: "+Utils.EOL+messageHeaderToString(msg));
		else if (logger.isDebugEnabled())
			logger.debug(name+" sends byte message: "+Utils.EOL+new String(raw));
		
		sendQueue.put(msg, createPutMessageOptions(msg));
		if (logger.isTraceEnabled())
			logger.trace(name+" has sent byte message successfully");
		incSent();
	}
	
	
	/* Methods to override */
	
	protected MQMessage prepareMQMessage()
	{
		MQMessage result = new MQMessage();
		result.format = MQConstants.MQFMT_STRING;
		result.messageId = ("MSG"+msgIdGen.generateValue(13)).getBytes();
		result.characterSet = storedSettings.charset;
		return result;
	}
	
	protected MQMessage prepareMQMessage(String message) throws IOException
	{
		MQMessage result = prepareMQMessage();
		if (message!=null)
			result.writeString(message);
		return result;
	}
	
	protected MQMessage prepareMQMessage(byte[] raw) throws IOException
	{
		MQMessage result = prepareMQMessage();
		result.write(raw);
		return result;
	}
	
	
	protected String messageHeaderToString(MQMessage message)
	{
		LineBuilder headers = new LineBuilder();
		headers.append("accountingToken = "+new String(message.accountingToken).trim())
				.append("applicationIdData = "+message.applicationIdData)
				.append("applicationOriginData = "+message.applicationOriginData)
				.append("backoutCount = "+message.backoutCount)
				.append("characterSet = "+message.characterSet)
				.append("correlationId = "+new String(message.correlationId).trim())
				.append("encoding = "+message.encoding)
				.append("expiry = "+message.expiry)
				.append("feedback = "+message.feedback)
				.append("format = "+message.format)
				.append("groupId = "+new String(message.groupId).trim())
				.append("messageFlags = "+message.messageFlags)
				.append("messageId = "+new String(message.messageId).trim())
				.append("messageSequenceNumber = "+message.messageSequenceNumber)
				.append("messageType = "+message.messageType)
				.append("offset = "+message.offset)
				.append("originalLength = "+message.originalLength)
				.append("persistence = "+message.persistence)
				.append("priority = "+message.priority)
				.append("putApplicationName = "+message.putApplicationName)
				.append("putApplicationType = "+message.putApplicationType)
				.append("putDateTime = "+message.putDateTime)
				.append("replyToQueueManagerName = "+message.replyToQueueManagerName)
				.append("replyToQueueName = "+message.replyToQueueName)
				.append("report = "+message.report)
				.append("userId = "+message.userId);
		return headers.toString();
	}
}
