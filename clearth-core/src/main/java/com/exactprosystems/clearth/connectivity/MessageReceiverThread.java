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

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.exactprosystems.clearth.ClearThCore;
import org.slf4j.Logger;

import com.exactprosystems.clearth.utils.Pair;
import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.constants.MQConstants;

import com.exactprosystems.clearth.connectivity.connections.ConnectionErrorInfo;

public abstract class MessageReceiverThread extends Thread
{
	protected final MQConnection owner;
	protected final MQQueue receiveQueue;
	protected final BlockingQueue<Pair<String, Date>> messageQueue;
	protected final int charset;
	protected AtomicBoolean terminated = new AtomicBoolean(false);
	protected Date started = null, stopped = null;

	public MessageReceiverThread(String name, MQConnection owner, MQQueue receiveQueue, BlockingQueue<Pair<String, Date>> messageQueue, int charset)
	{
		super(name);
		
		this.owner = owner;
		this.receiveQueue = receiveQueue;
		this.messageQueue = messageQueue;
		this.charset = charset;
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
	
	public void terminate()
	{
		terminated.set(true);
	}
	
	public Date getStarted()
	{
		return started;
	}
	
	public Date getStopped()
	{
		return stopped;
	}
	
	protected abstract void doRun();
	
	protected abstract Logger getLogger();
	
	@Override
	public void run()
	{
		started = new Date();
		doRun();
		stopped = new Date();
	}
	
	protected void reconnect()
	{
		//Making up to MQConnection.retryAttemptCount attempts to reconnect
		for (int i=1; i<=owner.getRetryAttemptCount(); i++)
		{
			if (i>1)
			{
				try
				{
					long retryTimeout = owner.getRetryTimeout();
					getLogger().debug("Waiting for {} seconds before next reconnect attempt", retryTimeout / 1000.0);
					sleep(retryTimeout);
				}
				catch (InterruptedException e)
				{
					getLogger().warn("Wait between reconnect attempts interrupted", e);
					break;
				}
			}
			
			getLogger().debug("Restart attempt #"+i);
			try
			{
				if ((owner.reconnect()) || (!owner.isRunning()))  //Stop reconnect attempts if reconnect successfully done or if connection is closed by user 
					return;
				else
					getLogger().debug("Restart attempt failed");
			}
			catch (Exception e)
			{
				getLogger().warn("Restart attempt failed with error", e);
			}
		}

		//If we got here - all reconnect attempts failed
		try
		{
			owner.stop();
		}
		catch (Exception e)
		{
			getLogger().warn("Error occurred while stopping connection after failed reconnect", e);
		}
	}
	
	protected void stopConnection(String errorMessage, Throwable reason)
	{
		try
		{
			owner.stop();
			ClearThCore.connectionStorage().addStoppedConnectionError(createConnectionErrorInfo(errorMessage, reason));
		}
		catch (Exception e)
		{
			getLogger().warn("Error occurred while stopping connection from receiver thread", e);
		}
	}
	
	protected ConnectionErrorInfo createConnectionErrorInfo(String error, Throwable reason)
	{
		return new ConnectionErrorInfo(owner.getName(), error, Instant.now());
	}
}
