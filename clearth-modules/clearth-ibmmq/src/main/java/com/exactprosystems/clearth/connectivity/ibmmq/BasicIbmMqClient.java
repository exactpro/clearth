/*******************************************************************************
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

package com.exactprosystems.clearth.connectivity.ibmmq;

import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.clients.BasicClearThClient;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import com.ibm.mq.*;
import com.ibm.mq.constants.CMQC;
import com.ibm.mq.constants.MQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Hashtable;

import static com.exactprosystems.clearth.connectivity.MQExceptionUtils.isConnectionBroken;
import static org.apache.commons.lang.StringUtils.isWhitespace;

public abstract class BasicIbmMqClient extends BasicClearThClient
{
	private static final Logger logger = LoggerFactory.getLogger(BasicIbmMqClient.class);
	public static final int DEFAULT_PORT = 1414;
	public static final String DEFAULT_HOST = "localhost";
	private final ValueGenerator msgIdGen;

	protected final Object sendMonitor = new Object();
	protected MQQueueManager sendQueueManager,
			receiveQueueManager;
	protected MQQueue receiveQueue;
	protected MQQueue sendQueue;

	public BasicIbmMqClient(IbmMqConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
		msgIdGen = createValueGenerator();
	}

	protected abstract ValueGenerator createValueGenerator();

	protected IbmMqConnectionSettings getSettings()
	{
		return (IbmMqConnectionSettings) storedSettings;
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

	protected MQQueueManager createManager(String hostname, int port, String managerName, String channel) throws
			MQException
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
		IbmMqConnectionSettings settings = getSettings();
		sendQueueManager = getManagerAccess(settings.getHostname(), settings.getQueueManager(), settings.getPort(),
				settings.getChannel());
	}

	protected void connectToReceivingManager() throws MQException
	{
		IbmMqConnectionSettings settings = getSettings();
		receiveQueueManager = getManagerAccess(settings.getHostname(), settings.getQueueManager(), settings.getPort(),
				settings.getChannel());
	}

	protected void connectToSendQueue() throws MQException
	{
		sendQueue = sendQueueManager.accessQueue(getSettings().getSendQueue(), CMQC.MQOO_OUTPUT);
	}

	protected void connectToReceiveQueue() throws MQException
	{
		receiveQueue = receiveQueueManager.accessQueue(getSettings().getReceiveQueue(),
				CMQC.MQOO_INPUT_AS_Q_DEF | CMQC.MQOO_INQUIRE);
	}

	protected void closeConnections() throws ConnectionException
	{
		try
		{
			if (sendQueue != null)
			{
				logger.trace(name + ": closing send queue");
				sendQueue.close();
				sendQueue = null;
			}

			if (receiveQueue != null)
			{
				logger.trace(name + ": closing receive queue");
				receiveQueue.close();
				receiveQueue = null;
			}

			logger.trace(name + ": disconnecting from queue managers");
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
		IbmMqConnectionSettings settings = getSettings();
		if ((settings.getQueueManager() == null) || (settings.getQueueManager().isEmpty()))
		{
			if (logger.isInfoEnabled())
				logger.info(name+": queue manager not specified, sending and receiving messages from queue is not available, "
						+ "but messages still can be processed according to listeners settings");
			return;
		}

		boolean sendingConnection =
				(settings.getSendQueue() != null) && (!settings.getSendQueue().trim().isEmpty()),
				receivingConnection = settings.isUseReceiveQueue();
		try
		{
			//Connecting to one manager using two separate objects in order to avoid locking of message sending while receiveQueue.get() is waiting for message to arrive
			if (sendingConnection)
				connectToSendingManager();
			if (receivingConnection)
				connectToReceivingManager();

			logger.trace(name+": connected to queue manager '" + settings.getQueueManager() + "'");

			if ((settings.getReceiveQueue()!=null) && (!settings.getReceiveQueue().isEmpty()))
			{
				if (receivingConnection)
				{
					connectToReceiveQueue();
					logger.trace(name+": connected to receive queue '"+settings.getReceiveQueue()+"'");
				}
				else
					logger.trace(name+": receive queue name specified, but messages receiving is disabled by 'Use receive queue' setting");
			}
			else
				logger.trace(name+": receive queue name not specified, receiving messages is not available");

			if (sendingConnection)
			{
				connectToSendQueue();
				logger.trace(name+": connected to send queue '" + settings.getSendQueue() + "'");
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

	public void sendByteMessage(byte[] raw, ClearThMessageMetadata metadata) throws IOException, MQException
	{
		MQMessage msg = prepareMQMessage(raw, metadata);
		if (logger.isTraceEnabled())
			logger.trace(name+" sends byte message: "+ Utils.EOL+new String(raw)+Utils.EOL+"header: "+Utils.EOL+messageHeaderToString(msg));
		else if (logger.isDebugEnabled())
			logger.debug(name+" sends byte message: "+Utils.EOL+new String(raw));

		sendQueue.put(msg, createPutMessageOptions(msg));
		if (logger.isTraceEnabled())
			logger.trace(name+" has sent byte message successfully");
		sent.incrementAndGet();
	}


	/* Methods to override */

	@Override
	protected EncodedClearThMessage doSendMessage(Object message) throws IOException, ConnectivityException
	{
		return doSendMessage(message, null);
	}

	@Override
	protected EncodedClearThMessage doSendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		return doSendMessage(message.getPayload(), message.getMetadata());
	}

	protected EncodedClearThMessage doSendMessage(Object payload, ClearThMessageMetadata metadata) throws IOException, ConnectivityException
	{
		synchronized (sendMonitor)
		{
			if (sendQueue == null)
				throw new ConnectionException("Send queue not specified for '" + name + "'");

			MQMessage msg = prepareMQMessage(payload, metadata);

			if (logger.isTraceEnabled())
				logger.trace("{} sends message:{}{}{}header:{}{}",
						name, Utils.EOL, payload, Utils.EOL, Utils.EOL, messageHeaderToString(msg));
			else
				logger.debug("{} sends message:{}{}",
						name, Utils.EOL, payload);

			try
			{
				sendQueue.put(msg, createPutMessageOptions(msg));
				logger.trace("{} has sent message successfully", name);
			}
			catch (MQException e)
			{
				handleSendError(e, payload);
			}

			return null;
		}
	}

	protected void handleSendError(Throwable error, final Object message) throws ConnectivityException
	{
		if (isConnectionBrokenError(error))  //Broken connection indicates a need to make one reconnect attempt
		{
			try
			{
				owner.restart();
				try
				{
					owner.sendMessage(message);
				}
				catch (ConnectionException ex)
				{
					//No need to close connection here - it will be closed by owner.stop() called from inside of owner.sendMessage() if exception occurred there and next reconnect attempt failed
					throw new ConnectionException("Could not send message, reconnect attempt successful, but right after that messages still can't be sent. This indicates instability in queue access", ex);
				}
				catch (Exception ex)
				{
					try
					{
						String errMessage1 = "Error while sending message.";
						owner.addErrorInfo(errMessage1, ex, Instant.now());
						logger.error(errMessage1, ex);
						owner.stop();
					}
					catch (Exception ex1)
					{
						String errMessage2 = name+": error occurred while stopping connection after successful " +
								"reconnect and failed attempt to re-send message";
						owner.addErrorInfo(errMessage2, ex1, Instant.now());
						logger.error(errMessage2, ex1);
					}
					throw new ConnectionException("Could not send message, connection is broken, re-send attempt failed after reconnect");
				}
			}
			catch (SettingsException e)
			{
				try
				{
					logger.error("Connection is not reconnected.", e);
					owner.stop();
				}
				catch (Exception ex)
				{
					logger.error(name+": error occurred while stopping connection after failed reconnect", ex);
				}
				throw new ConnectionException("Could not send message, connection is broken, reconnect attempt failed");
			}
		}
		else  //Error with no handler occurred, closing connection
		{
			try
			{
				String errMessage = "Error occurred while sending message. Connection will be stopped.";
				owner.addErrorInfo(errMessage, error, Instant.now());
				logger.error(errMessage,error);
				owner.stop();
			}
			catch (Exception ex)
			{
				logger.error(name+": error occurred while stopping connection on unhandled error", ex);
			}
			throw new ConnectionException(error.getMessage());
		}
	}
	

	protected MQMessage prepareMQMessage()
	{
		MQMessage result = new MQMessage();
		result.format = MQConstants.MQFMT_STRING;
		result.messageId = ("MSG"+msgIdGen.generateValue(13)).getBytes();
		result.characterSet = getSettings().getCharset();
		return result;
	}

	protected MQMessage prepareMQMessage(Object message, ClearThMessageMetadata metadata) throws IOException
	{
		MQMessage result = prepareMQMessage();
		if (message != null)
			result.writeString(message.toString());
		return result;
	}

	protected MQMessage prepareMQMessage(byte[] raw, ClearThMessageMetadata metadata) throws IOException
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