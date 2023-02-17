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

package com.exactprosystems.clearth.connectivity.jms;

import com.exactprosystems.clearth.connectivity.ConnectionException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.clients.BasicClearThClient;
import com.exactprosystems.clearth.connectivity.connections.clients.MessageReceiverThread;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.net.ConnectException;
import java.util.concurrent.BlockingQueue;

import static com.exactprosystems.clearth.utils.Utils.EOL;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;

public abstract class JmsClient extends BasicClearThClient
{
	private static final Logger logger = LoggerFactory.getLogger(JmsClient.class);
	private final Object sendMonitor = new Object();
	protected Connection connection;
	protected Session session;
	protected MessageProducer producer;
	protected MessageConsumer consumer;

	protected abstract ConnectionFactory createConnectionFactory() throws SettingsException, ConnectivityException;

	public abstract boolean isClosedSession() throws IllegalStateException;
		
	public JmsClient(ClearThMessageConnection owner) throws ConnectivityException, SettingsException
	{
		super(owner);
	}
	
	@Override
	protected void connect() throws ConnectivityException, SettingsException
	{
		try
		{
			ConnectionFactory factory = createConnectionFactory();

			connection = createJmsConnection(factory);
			connection.start();
			session = createSession(connection);
			JmsConnectionSettings settings = (JmsConnectionSettings) storedSettings;
			if (!StringUtils.isBlank(settings.getReceiveQueue()))
			{
				consumer = createConsumer(session, settings.getReceiveQueue());
				logger.trace("{}: connected to receive queue '{}'", name, settings.getReceiveQueue());
			}
			else
				logger.trace("{}: receive queue name not specified, messages receiving is not available", name);

			if (!StringUtils.isBlank(settings.getSendQueue()))
			{
				producer = createProducer(session, settings.getSendQueue());
				logger.trace("{}: connected to send queue '{}'", name, settings.getSendQueue());
			}
			else
				logger.trace("{}: send queue name not specified, messages sending is not available", name);
		}
		catch (JMSException e)
		{
			throw handleJMSException(e);
		}
	}

	protected Session createSession(Connection connection) throws JMSException
	{
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	protected MessageConsumer createConsumer(Session session, String queueName) throws JMSException
	{
		Destination dest = session.createQueue(queueName);
		return session.createConsumer(dest);
	}

	protected MessageProducer createProducer(Session session, String sendQueue) throws JMSException
	{
		Destination dest = session.createQueue(sendQueue);
		return session.createProducer(dest);
	}

	protected Connection createJmsConnection(ConnectionFactory factory) throws JMSException
	{
		return factory.createConnection();
	}
	
	protected ConnectionException handleJMSException(JMSException e)
	{
		if (e.getLinkedException() instanceof ConnectException)
		{
			ConnectException reason = (ConnectException) e.getLinkedException();
			if (containsIgnoreCase(reason.getMessage(), "connection refused"))
				return new ConnectionException("Connection refused. Probably settings are incorrect or MQ isn't running", e);
		}
		return new ConnectionException(e);
	}
	
	@Override
	protected Object doSendMessage(Object message) throws ConnectivityException
	{
		return doSendMessage(message, null);
	}
	
	@Override
	protected Object doSendMessage(EncodedClearThMessage message) throws ConnectivityException
	{
		return doSendMessage(message.getPayload(), message.getMetadata());
	}
	
	protected Object doSendMessage(Object payload, ClearThMessageMetadata metadata) throws ConnectivityException
	{

		if (producer == null)
			throw new ConnectionException("Send queue not specified for '" + name + "'");
		
		synchronized (sendMonitor)
		{
			logger.trace("JMS client '{}' sends message: {}", name, payload);
			
			try
			{
				Message msg = createMessage(payload, metadata);
				
				producer.send(msg);
				logger.debug("JMS client '{}' sent message", name);
				return msg.toString();
			}
			catch (JMSException e)
			{
				logger.error("Error while sending message via JMS client '{}'", name, e);
				throw new ConnectivityException("Error while sending message: " + e.getMessage(), e);
			}
		}
	}
	
	protected Message createMessage(Object payload, ClearThMessageMetadata metadata) throws JMSException
	{
		return session.createTextMessage(payload.toString());
	}
	
	@Override
	protected boolean isNeedReceiverThread()
	{
		return consumer != null;
	}
	
	@Override
	protected JmsReceiverThread createReceiverThread()
	{
		return new JmsReceiverThread(name + " (Receiver thread)",
				consumer,
				owner,
				((JmsConnectionSettings) storedSettings).getReadDelay(),
				receivedMessageQueue);
	}
	
	
	protected void closeConnections() throws ConnectionException
	{
		try
		{
			if (consumer != null)
			{
				logger.trace("{}: closing consumer", name);
				consumer.close();
				consumer = null;
			}
				
			if (producer != null)
			{
				logger.trace("{}: closing producer", name);
				producer.close();
				producer = null;
			}
			
			if (session != null)
			{
				logger.trace("{}: closing session", name);
				session.close();
				session = null;
			}

			if (connection != null)
			{
				logger.trace("{}: closing JMS connection", name);
				connection.close();
				connection = null;
			}
		}
		catch (Exception e)
		{
			throw new ConnectionException(e);
		}
	}
	
	
	static class JmsReceiverThread extends MessageReceiverThread
	{
		private final MessageConsumer consumer;
		
		public JmsReceiverThread(String name, MessageConsumer consumer, ClearThMessageConnection owner, long readDelay,
		                         BlockingQueue<EncodedClearThMessage> receivedMessageQueue)
		{
			super(name, owner, receivedMessageQueue, readDelay);
			
			this.consumer = consumer;
		}
		
		
		@Override
		protected void getAndHandleMessage() throws Exception
		{
			Message message = consumer.receive(1000);
			if (message == null)
				return;
			
			if (message instanceof TextMessage)
			{
				String body = ((TextMessage) message).getText();
				receivedMessageQueue.add(EncodedClearThMessage.newReceivedMessage(body));
			}
			else
				logger.warn("Received non-text message, skipping it as not supported:{}{}", EOL, message);
		}
	}
}
