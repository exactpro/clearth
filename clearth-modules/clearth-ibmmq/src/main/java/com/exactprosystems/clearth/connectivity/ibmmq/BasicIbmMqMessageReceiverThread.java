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

import com.exactprosystems.clearth.connectivity.connections.clients.MessageReceiverThread;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.constants.MQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BasicIbmMqMessageReceiverThread extends MessageReceiverThread
{
	private static final Logger logger = LoggerFactory.getLogger(BasicIbmMqMessageReceiverThread.class);
	
	protected final MQQueue receiveQueue;
	protected MQMessage message;
	protected final int charset;
	protected AtomicBoolean terminated = new AtomicBoolean(false);
	protected final int retryAttemptsCount;
	protected final long retryTimeout;

	public BasicIbmMqMessageReceiverThread(String name, IbmMqConnection owner, MQQueue receiveQueue,
	                                       BlockingQueue<EncodedClearThMessage> messageQueue, int charset,
	                                       long readDelay)
	{
		super(name, owner, messageQueue, readDelay);
		this.receiveQueue = receiveQueue;
		this.charset = charset;
		retryAttemptsCount = owner.getRetryAttemptCount();
		retryTimeout = owner.getRetryTimeout();
	}

	protected MQMessage createNewMessage(int charset)
	{
		MQMessage m = new MQMessage();
		m.characterSet = charset;
		return m;
	}

	protected void resetMessage(MQMessage message)
	{
		message.encoding = MQConstants.MQENC_NATIVE;
		message.characterSet = MQConstants.MQCCSI_Q_MGR;
		message.messageId = MQConstants.MQMI_NONE;
		message.correlationId = MQConstants.MQCI_NONE;
	}

	protected MQGetMessageOptions createNewGMO(int waitInterval)
	{
		MQGetMessageOptions gmo = new MQGetMessageOptions();
		gmo.options = MQConstants.MQGMO_WAIT | MQConstants.MQGMO_NO_SYNCPOINT | MQConstants.MQGMO_CONVERT;
		gmo.waitInterval = waitInterval;
		return gmo;
	}

	protected void readMessage(MQMessage message, int waitInterval) throws MQException
	{
		MQGetMessageOptions gmo = createNewGMO(waitInterval);
		if (gmo != null)
			receiveQueue.get(message, gmo);
		else
			receiveQueue.get(message);
	}

	protected void reconnect()
	{
		//Making up to MQConnection.retryAttemptCount attempts to reconnect
		for (int i = 1; i <= retryAttemptsCount; i++)
		{
			if (i > 1)
			{
				try
				{
					logger.debug("Waiting for {} seconds before next reconnect attempt", retryTimeout / 1000.0);
					sleep(retryTimeout);
				}
				catch (InterruptedException e)
				{
					logger.warn("Wait between reconnect attempts interrupted", e);
					break;
				}
			}

			logger.debug("Restart attempt #"+i);
			try
			{
				owner.restart();
			}
			catch (Exception e)
			{
				logger.warn("Restart attempt failed", e);
			}
		}

		//If we got here - all reconnect attempts failed
		try
		{
			owner.stop();
		}
		catch (Exception e)
		{
			logger.warn("Error occurred while stopping connection after failed reconnect", e);
		}
	}

	protected void stopConnection(String errorMessage, Throwable reason)
	{
		try
		{
			owner.addErrorInfo(errorMessage, reason, Instant.now());
			owner.stop();
		}
		catch (Exception e)
		{
			logger.warn("Error occurred while stopping connection from receiver thread", e);
		}
	}

	protected EncodedClearThMessage createReceivedMessage(Object payload)
	{
		return EncodedClearThMessage.newReceivedMessage(payload, Instant.now());
	}

	@Override
	protected void beforeRun()
	{
		message = createNewMessage(charset);
	}
}