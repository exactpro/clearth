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
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.constants.MQConstants;

import static com.exactprosystems.clearth.connectivity.MQExceptionUtils.isConnectionBroken;

public class SimpleReceiverThread extends MessageReceiverThread
{
	private static final Logger logger = LoggerFactory.getLogger(SimpleReceiverThread.class);
	
	private final boolean autoReconnect;
	private final int readDelay;
	
	public SimpleReceiverThread(String name, MQConnection owner, MQQueue receiveQueue, BlockingQueue<EncodedClearThMessage> messageQueue, int charset, 
			boolean autoReconnect, int readDelay)
	{
		super(name, owner, receiveQueue, messageQueue, charset);
		this.autoReconnect = autoReconnect;
		this.readDelay = readDelay;
	}
	
	@Override
	public void doRun()
	{
		int errorCount = 0;
		MQMessage message = createNewMessage(charset);
		while (!terminated.get())
		{
			resetMessage(message);
			try
			{
				readMessage(message, 10000);
				errorCount = 0;
				
				if (message.originalLength!=0)
				{
					try
					{
						logger.trace("Adding message to internal queue");
						String m = message.readStringOfByteLength(message.getDataLength());
						
						boolean inserted = messageQueue.offer(createReceivedMessage(m));
						
						if (!inserted) 
							logger.warn("It is not possible to add message to queue due to capacity restrictions");
					}
					catch (Exception e)
					{
						logger.warn("Error while converting MQMessage to String, message not added to internal queue", e);
					}
				}
				else
					logger.trace("Received message is empty (originalLength=0)");
			}
			catch (MQException e)
			{
				if (isConnectionBroken(e))
				{
					if (autoReconnect)
					{
						logger.error("Connection broken, reconnecting");
						reconnect();
					}
					else
					{
						logger.error("Connection broken, stopping connection");
						stopConnection("RC"+e.reasonCode+". Connection broken.", e);
					}
					break;
				}
				else if ((e.completionCode != MQConstants.MQCC_FAILED) || (e.reasonCode != MQConstants.MQRC_NO_MSG_AVAILABLE))
				{
					logger.error("Exception while reading message", e);
					errorCount++;
					if (errorCount>=10)
					{
						logger.info("Too many errors on end, seems like receive queue is down. Stopping connection");
						stopConnection("RC"+e.reasonCode+". Too many errors while reading messages.", e);
						break;
					}
				}
			}
			
			if (readDelay > 0)
			{
				try
				{
					Thread.sleep(readDelay);
				}
				catch (InterruptedException e)
				{
					logger.warn("Read delay interrupted, stopping receiver");
					break;
				}
			}
			
			if (this.isInterrupted()) 
				this.terminated.set(true);
		}
	}

	@Override
	protected Logger getLogger()
	{
		return logger;
	}
}
