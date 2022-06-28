/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import java.util.Date;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.utils.Pair;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.constants.MQConstants;

import static com.exactprosystems.clearth.connectivity.MQExceptionUtils.isConnectionBroken;

public class DiverReceiverThread extends MessageReceiverThread
{
	private static final Logger logger = LoggerFactory.getLogger(DiverReceiverThread.class);
	
	private final boolean autoReconnect;
	private final long readDelay;    //time interval in millis before reading next message
	
	public DiverReceiverThread(String name, MQConnection owner, MQQueue receiveQueue, BlockingQueue<Pair<String, Date>> messageQueue, int charset, 
			boolean autoReconnect, long readDelay)
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
			int depth = 0;
			try
			{
				depth = receiveQueue.getCurrentDepth();
				errorCount = 0;
			}
			catch (MQException e)
			{
				if (isConnectionBroken(e))
				{
					if (autoReconnect)
					{
						logger.info("Connection broken while getting queue size, reconnecting");
						reconnect();
						logger.debug("Exit the thread in spite of reconnect result, because new receiver will be created");
					}
					else
					{
						logger.info("Connection broken, stopping connection");
						stopConnection("RC"+e.reasonCode+". Connection broken.", e);
					}
					break;
				}
				else
				{
					logger.warn("Exception while reading message", e);
					errorCount++;
					if (errorCount>=10)
					{
						logger.info("Too many errors on end, seems like receive queue is down. Stopping connection");
						stopConnection("RC"+e.reasonCode+". Too many errors while reading messages.", e);
						break;
					}
				}
			}

			if (depth>0)
			{
				logger.trace("Queue depth: "+depth);
				try
				{
					resetMessage(message);
					readMessage(message, 1000);
					if (message.originalLength!=0)
					{
						try
						{
							logger.trace("Adding message to internal queue");
							String m = message.readStringOfByteLength(message.getDataLength());
							boolean inserted = messageQueue.offer(new Pair<>(m, new Date()));
							
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
							logger.info("Connection broken while processing queue, reconnecting");
							reconnect();
							logger.debug("Exit the thread in spite of reconnect result, because new receiver will be created");
						}
						else
						{
							logger.info("Connection broken, stopping connection");
							stopConnection("RC"+e.reasonCode+". Connection broken.", e);
						}
						break;
					}
					else if ((e.completionCode!=MQConstants.MQCC_FAILED) || (e.reasonCode!=MQConstants.MQRC_NO_MSG_AVAILABLE))
					{
						logger.error("Exception while receiving message, stopping connection", e);
						stopConnection("RC"+e.reasonCode+". Error while receiving message.", e);
						break;
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
			}
			else
			{
				try
				{
					Thread.sleep(2000);
				}
				catch (InterruptedException e)
				{
					logger.warn("Message wait interrupted, stopping receiver");
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
