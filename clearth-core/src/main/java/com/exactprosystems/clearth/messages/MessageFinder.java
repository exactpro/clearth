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

package com.exactprosystems.clearth.messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.Stopwatch;
import com.exactprosystems.clearth.utils.Utils;

/**
 * Class to find messages that match defined conditions
 */
public class MessageFinder<T extends ClearThMessage<T>>
{
	private static final Logger logger = LoggerFactory.getLogger(MessageFinder.class);
	private static final int DEFAULT_SEARCH_DELAY_MS = 100;
	
	private final int searchDelayMs;
	private long lastDurationMs = 0;
	
	public MessageFinder()
	{
		this(DEFAULT_SEARCH_DELAY_MS);
	}
	
	public MessageFinder(int searchDelayMs)
	{
		this.searchDelayMs = searchDelayMs;
	}
	
	
	/**
	 * Finds first message that matches defined conditions
	 * @param messageSource to find message in
	 * @param matcher holds conditions to check message
	 * @param timeout defines maximum time (in milliseconds) to wait for needed message to appear in messageSource
	 * @param removeMessage if true, found message will be removed from messageSource
	 * @return found message or null if nothing matched
	 * @throws InterruptedException if message search was interrupted
	 * @throws IOException if messageSource failed to provide messages
	 * @throws ParametersException in case of comparison error
	 */
	public T find(MessageSource messageSource, MessageMatcher<T> matcher, long timeout, boolean removeMessage) 
			throws InterruptedException, IOException, ParametersException
	{
		List<T> messages = findAllMessages(messageSource, matcher, timeout, removeMessage, true);
		if (messages == null)
			return null;
		else
			return messages.get(0);
	}
	
	
	/**
	 * Finds all messages that match defined conditions
	 * @param messageSource to find messages in
	 * @param matcher holds conditions to check message
	 * @param timeout defines maximum time (in milliseconds) to wait for needed messages to appear in messageSource
	 * @param removeMessages if true, found messages will be removed from messageSource
	 * @param onlyOneMessage if true, only first found message will be returned in result list
	 * @return found messages or null if nothing matched
	 * @throws InterruptedException if search was interrupted
	 * @throws IOException if messageSource failed to provide messages
	 * @throws ParametersException in case of comparison error
	 */
	public List<T> findAll(MessageSource messageSource, MessageMatcher<T> matcher, long timeout, boolean removeMessages, boolean onlyOneMessage) 
			throws InterruptedException, IOException, ParametersException
	{
		return findAllMessages(messageSource, matcher, timeout, removeMessages, onlyOneMessage);
	}
	
	/**
	 * @return duration (in milliseconds) of last search
	 */
	public long getLastSearchDuration()
	{
		return lastDurationMs;
	}
	
	@SuppressWarnings("unchecked")
	private List<T> findAllMessages(MessageSource messageSource, MessageMatcher<T> matcher, long timeout,
	                                boolean removeMessage, boolean findOnlyFirstMessage) throws InterruptedException, IOException, ParametersException
	{
		List<T> messages = null;

		if (timeout < 0)
			timeout = 0;

		int iteration = 1;
		lastDurationMs = 0;
		Stopwatch sw = Stopwatch.createAndStart(timeout);
		try
		{
			while (true)
			{
				logger.trace("Search for message iteration #{}", iteration++);
				try
				{
					T message;
					while ((message = (T)messageSource.nextMessage()) != null)
					{
						if (Thread.interrupted())
							throw new InterruptedException();
						
						logMessageInfo(message);
						if (matcher.matches(message))
						{
							logger.debug("Message suits key fields");
							if (messages == null)
								messages = new ArrayList<T>();
							messages.add(message);
							
							if (removeMessage)
								messageSource.removeMessage();
							if (findOnlyFirstMessage)
								return messages;
						}
					}
					
					long timeLeft = timeout-sw.getElapsedMillis();
					if (timeLeft <= 0)
						return messages;
					
					long timeToSleep = Math.min(searchDelayMs, timeLeft);
					logger.trace("Pause for {} ms", timeToSleep);
					Thread.sleep(timeToSleep);
				}
				catch (IOException e)
				{
					throw new IOException("Error while getting next message", e);
				}
			}
		}
		finally
		{
			lastDurationMs = sw.stop();
		}
	}
	
	protected void logMessageInfo(T message)
	{
		if (!logger.isDebugEnabled())
			return;
	
		String encoded = message.getEncodedMessage();
		if (encoded == null)
			encoded = "[original message text is not stored]";
	
		if (encoded.length() > 100)
			encoded = encoded.substring(0, 100)+"...";
		logger.debug("Checking message:" + Utils.EOL + encoded);
	}
}
