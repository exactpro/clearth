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

package com.exactprosystems.clearth.connectivity.listeners;

import com.exactprosystems.clearth.connectivity.ListenerDescription;
import com.exactprosystems.clearth.connectivity.ListenerProperties;
import com.exactprosystems.clearth.connectivity.ReceiveListener;
import com.exactprosystems.clearth.connectivity.SettingsDetails;
import com.exactprosystems.clearth.connectivity.iface.AbstractMessageListener;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.ReceivedClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ReceivedMessage;
import com.exactprosystems.clearth.connectivity.iface.ReceivedStringMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.listeners.storage.ContentStorage;
import com.exactprosystems.clearth.connectivity.listeners.storage.DefaultFileContentStorage;
import com.exactprosystems.clearth.connectivity.listeners.storage.FileContentStorage;
import com.exactprosystems.clearth.connectivity.listeners.storage.MemoryContentStorage;
import com.exactprosystems.clearth.connectivity.listeners.storage.WritingContentStorage;
import com.exactprosystems.clearth.messages.MessageFileReader;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ListenerDescription(description = "ClearTH message collector")
@SettingsDetails(details = "Format: <code>setting=value;setting=value</code><br/><br/>" + "Settings:" + "<ul>"
		+ "<li><b>type=&lt;value&gt;</b> &mdash; type of a codec that will decode incoming messages. <br/>" 
		+   "If incoming messages are of various formats, specify multiple codec names delimited with comma (,). <br/>"
		+   "If you just need to collect messages without decoding them, omit this setting.</li>"
		+ "<li><b>fileName=&lt;path&gt;</b> &mdash; path to a file which contains an initial message set for this collector.</li>"
		+ "<li><b>contentsFileName=&lt;path&gt;</b> &mdash; path to a file in which to store current collector contents.</li>"
		+ "<li><b>storeTimestamp=&lt;true/false&gt;</b> &mdash; If set as 'true' message receiving timestamp will be written in storage file before message content. Default value is 'false'.</li>"
		+ "<li><b>maxAge=&lt;value&gt;</b> &mdash; hours after which a message will be removed from collector.</li>"
		+ "<li><b>failedMaxAge=&lt;value&gt;</b> &mdash; hours after which a message will be removed from failed-to-parse messages. Default value is '6'.</li>"
		+ "<li><b>storeFailed=&lt;true/false&gt;</b> &mdash; indicates if failed-to-parse messages should be stored in collector for further analysis. <br/>Please note that they occupy memory if stored. Default value is 'true'.</li>"
		+ "<li><b>allowedTypes=&lt;type&gt;</b> &mdash; If it exists messages of other types are ignored. Separate allowed types with comma (,).</li>"
		+ "<li><b>forbiddenTypes=&lt;type&gt;</b> &mdash; If it exists messages of specified types are ignored. Separate forbidden types with comma (,).</li>"
		+ "</ul>" + "All settings are optional.")
public class ClearThMessageCollector extends AbstractMessageListener implements ReceiveListener
{
	private static final Logger logger = LoggerFactory.getLogger(ClearThMessageCollector.class);
	
	public static final String TYPE_SETTING = "type"; 
	public static final String FILENAME_SETTING = "filename";
	public static final String CONTENTSFILENAME_SETTING = "contentsfilename";
	public static final String MAXAGE_SETTING = "maxage";
	public static final String FAILEDMAXAGE_SETTING = "failedmaxage";
	public static final String STOREFAILEDMESSAGES_SETTING = "storefailed";
	public static final String STORE_RECEIVING_TIMESTAMP_SETTING = "storetimestamp";
	public static final String MESSAGE = "Message";
	public static final String DEFAULT_MESSAGE_END_INDICATOR = Utils.EOL + Utils.EOL;
	public static final String ALLOWED_TYPES = "allowedtypes";
	public static final String FORBIDDEN_TYPES = "forbiddentypes";
	private static final String STORE_THREAD_NAME = "FileContentStorage";

	private static final String DELIMITER = ",";

	private static final int DEBUG_LOG_MSG_SIZE_LIMIT = 1024; //1 KB
	private static final double DEFAULT_MAX_AGE = -1;
	private static final double DEFAULT_FAILED_MAX_AGE = 6;

	private final Object codecMonitor = new Object();

	private volatile String connectionName;
	protected volatile boolean active = true;

 	protected final AtomicLong messageId;

	protected volatile ICodec codec;
	protected ContentStorage<ReceivedClearThMessage, ReceivedStringMessage> contentStorage;
	private final boolean storeFailedMessages;
	private final boolean storeTimestamp;

	private Set<String> filteredTypes;
	private boolean filterForAllowedTypes = true;
	private Instant lastMessageTime = Instant.MIN;

	private final ScheduledExecutorService collectorCleaner;
	private final double maxAgeDouble;
	private final double failedMaxAgeDouble;

	public ClearThMessageCollector(ListenerProperties properties, String connectionName, Map<String,String> settings, String messageEndIndicator) throws SettingsException
	{
		this(properties, connectionName, null, settings, messageEndIndicator);
	}

	public ClearThMessageCollector(ListenerProperties properties, String connectionName, ICodec codec, Map<String,String> settings, String messageEndIndicator) throws SettingsException
	{
		super(properties);
		
		logger.debug("Initializing ClearThMessageCollector");

		this.connectionName = connectionName;
		this.codec = codec;
		this.collectorCleaner = Executors.newScheduledThreadPool(1, r -> new Thread(r,
				connectionName + " (collectorCleanerTimer)"));

		InputParamsHandler handler = new InputParamsHandler(settings);
		storeFailedMessages = handler.getBoolean(STOREFAILEDMESSAGES_SETTING, true);
		storeTimestamp = handler.getBoolean(STORE_RECEIVING_TIMESTAMP_SETTING, false);

		String maxAge = handler.getString(MAXAGE_SETTING),
				failedMaxAge = handler.getString(FAILEDMAXAGE_SETTING);

		if (maxAge != null)
		{
			maxAgeDouble = Double.parseDouble(maxAge);
			if (maxAgeDouble * 60 < 10)
				throw new SettingsException("Error in Collector settings: value of 'maxAge' setting cannot be less than 10 minutes");
		}
		else
		{
			maxAgeDouble = DEFAULT_MAX_AGE;
		}

		if (failedMaxAge != null)
		{
			failedMaxAgeDouble = Double.parseDouble(failedMaxAge);
			if (failedMaxAgeDouble * 60 < 10)
				throw new SettingsException("Error in Collector settings: value of 'failedMaxAge' setting cannot be less than 10 minutes");
		}
		else
		{
			failedMaxAgeDouble = DEFAULT_FAILED_MAX_AGE;
		}

		Set<String> allowedTypes = handler.getSet(ALLOWED_TYPES, DELIMITER);
		Set<String> forbiddenTypes = handler.getSet(FORBIDDEN_TYPES, DELIMITER);
		processFilteredTypes(allowedTypes, forbiddenTypes);

		try
		{
			String contentsFileName = handler.getString(CONTENTSFILENAME_SETTING);
			if (contentsFileName != null)
				this.contentStorage = createFileContentStorage(contentsFileName);
		}
		catch (Exception e)
		{
			logger.error("Unable to create file content storage", e);
		}
		
		if (this.contentStorage == null)
		{
			logger.trace("Content will be stored in memory");
			this.contentStorage = new MemoryContentStorage<ReceivedClearThMessage, ReceivedStringMessage>();
		}

		this.messageId = new AtomicLong(0);
		
		initFromFile(handler.getString(FILENAME_SETTING), messageEndIndicator);
	}
	
	@Override
	public boolean isActiveForReceived()
	{
		return getProperties().isActiveForReceived();
	}

	@Override
	public void start()
	{
		collectorCleaner.scheduleWithFixedDelay(new CollectorCleaner(maxAgeDouble, failedMaxAgeDouble), 10, 10, TimeUnit.MINUTES);
		contentStorage.start();
	}
	
	@Override
	public void onMessage(EncodedClearThMessage message) throws IllegalArgumentException
	{
		String payload = message.getPayload().toString();
		logReceivedMessage(payload);
		long id = messageId.getAndIncrement();
		Instant timestamp = message.getMetadata().getTimestamp();
		
		if (timestamp == null) {
			throw new IllegalArgumentException("Timestamp cannot be null");
		}
		if (lastMessageTime.isAfter(timestamp)) {
			throw new IllegalArgumentException("Timestamp cannot be lower than an already received message");
		}
		lastMessageTime = timestamp;

		try
		{
			ClearThMessage<?> cthMessage;
			if (codec == null)
			{
				cthMessage = new SimpleClearThMessage();
				cthMessage.addField(MESSAGE, payload);
				cthMessage.setEncodedMessage(payload);
			}
			else
			{
				synchronized (codecMonitor)
				{
					cthMessage = codec.decode(payload);
				}
			}

			if (!validateType(cthMessage.getField(ClearThMessage.MSGTYPE)))
			{
				logger.trace("Skipped message: {}", cthMessage);
				return;
			}
			
			
			logger.trace("Adding message: {}, \r\ntimestamp: {}", cthMessage, timestamp);
			ReceivedClearThMessage receivedMessage = new ReceivedClearThMessage(id, timestamp.toEpochMilli(), cthMessage);
			contentStorage.insertPassed(id, receivedMessage);
		}
		catch (Exception e)
		{
			if(storeFailedMessages)
				contentStorage.insertFailed(id, new ReceivedStringMessage(id, timestamp.toEpochMilli(), payload));
			logger.warn("Error while decoding message: {}", message, e);
		}
	}

	@Override
	public void dispose()
	{
		logger.trace("Disposing ClearThMessageCollector");
		active = false;
		codec = null;
		collectorCleaner.shutdown();
		contentStorage.dispose();
	}

	
	/**
	 * @return all messages stored in collector
	 */
	public Collection<ClearThMessage<?>> getMessages()
	{
		logger.trace("Getting all messages from collector");
		Collection<ClearThMessage<?>> result = getMessages(contentStorage.getContentPassed().values());
		logger.trace("Messages count: {}", result.size());

		return result;
	}

	/**
	 * @return all messages stored in collector with their internal IDs and time of retrieval
	 */
	public Collection<ReceivedClearThMessage> getMessagesData()
	{
		return new ArrayDeque<>(contentStorage.getContentPassed().values());
	}

	/**
	 * Gets messages from collector received after message with given ID
	 * 
	 * @param afterId ID of message after which needed messages were received
	 * @return list of messages received after message with given ID
	 */
	public Collection<ClearThMessage<?>> getMessages(long afterId)
	{
		logger.trace("Getting messages with ID > {}", afterId);
		Collection<ClearThMessage<?>> result = getMessages(getMessagesAfterId(afterId));
		logger.trace("Messages count: {}", result.size());

		return result;
	}
	
	/**
	 * Gets from collector data about messages received after message with given ID
	 * 
	 * @param afterId ID of message after which needed messages were received
	 * @return list of messages received after message with given ID
	 */
	public Collection<ReceivedClearThMessage> getMessagesData(long afterId)
	{
		logger.trace("Getting messages data with ID > {}", afterId);
		Collection<ReceivedClearThMessage> result = new ArrayDeque<ReceivedClearThMessage>(getMessagesAfterId(afterId));
		logger.trace("Messages count: {}", result.size());
		return result;
	}
	
	/**
	 * Gets message data for given ID
	 * @param id of message
	 * @return message data for given ID or null if no message with given ID is stored in collector
	 */
	public ReceivedClearThMessage getMessageData(long id)
	{
		ReceivedClearThMessage found = contentStorage.getContentPassed().get(id);
		if (found == null)
			return null;
		return new ReceivedClearThMessage(found);
	}

	/**
	 * Gets message data for given message
	 * @param message to find data for
	 * @return message data for given message or null if this message is not stored in collector. Changes in returned value are not reflected in collector
	 */
	public ReceivedClearThMessage getMessageData(ClearThMessage<?> message)
	{
		for (ReceivedClearThMessage msg : contentStorage.getContentPassed().values())
		{
			if (msg.getMessage() == message)
				return new ReceivedClearThMessage(msg);
		}
		return null;
	}
	
	
	/**
	 * Gets from collector messages that could not be decoded 
	 *
	 * @return list of message data
	 */
	public Collection<ReceivedStringMessage> getMessagesFailed()
	{
		return new ArrayDeque<ReceivedStringMessage>(contentStorage.getContentFailed().values());
	}

	/**
	 * Gets from collector messages that could not be decoded and received after message with given ID
	 *
	 * @param afterId ID of message after which needed messages were received
	 * @return list of messages received after message with given ID
	 */
	public Collection<String> getMessagesFailed(long afterId)
	{
		logger.trace("Getting failed messages from collector with ID > {}", afterId);
		Collection<String> result = getMessages(getFailedMessagesAfterId(afterId));
		logger.trace("Failed messages count: {}", result.size());

		return result;
	}

	
	/**
	 * Removes message from collector
	 * 
	 * @param message to remove from collector
	 */
	public void removeMessage(ClearThMessage<?> message)
	{
		logger.trace("Removing message {} from collector", message);

		Collection<ReceivedClearThMessage> values = contentStorage.getContentPassed().values();
		Iterator<ReceivedClearThMessage> it = values.iterator();
		while (it.hasNext())
		{
			ReceivedClearThMessage v = it.next();
			//Doing so we should be able to remove only the message object which is got from collector, not the similar one in the meaning of fields. This is correct
			if (v.getMessage() == message)
			{
				it.remove();
				contentStorage.removePassed(v);
				break;
			}
		}
	}
	
	/**
	 * Removes from collector message with given ID
	 * @param id of message to remove
	 */
	public void removeMessage(long id)
	{
		logger.trace("Removing message with ID={} from collector", id);
		contentStorage.removePassed(id);
	}
	
	/**
	 * Removes all messages from collector
	 */
	public void clear()
	{
		logger.trace("Removing all messages from collector");
		contentStorage.clearPassed();
	}
	
	
	public ICodec getCodec()
	{
		return codec;
	}

	public void setCodec(ICodec codec)
	{
		this.codec = codec;
	}
	
	
	public String getConnectionName()
	{
		return connectionName;
	}

	public void setConnectionName(String connectionName)
	{
		this.connectionName = connectionName;
	}

	
	public boolean isActive()
	{
		return active;
	}
	
	protected void logReceivedMessage(String message)
	{
		if (message.length() < getDebugLogMessageSizeLimit())
			logger.debug("Received message: {}", message);
		else if (logger.isTraceEnabled())
			logger.trace("Received message: {}", message);
		else 
			logger.debug("Received message is too long to show on current logging level");
	}
	
	protected Collection<ReceivedClearThMessage> getMessagesAfterId(long id)
	{
		return contentStorage.getContentPassedAfterId(id).values();
	}
	
	protected Collection<ReceivedStringMessage> getFailedMessagesAfterId(long id)
	{
		return contentStorage.getContentFailedAfterId(id).values();
	}

	protected <T> Collection<T> getMessages(Collection<? extends ReceivedMessage<T>> messages)
	{
		Collection<T> result = new ArrayDeque<T>(messages.size());
		for (ReceivedMessage<T> storedMessage : messages)
			result.add(storedMessage.getMessage());

		return result;
	}
	
	protected MessageFileReader createMessageFileReader(String messageEndIndicator)
	{
		return new MessageFileReader(MessageFileReader.DEFAULT_TIMESTAMP_FORMAT, messageEndIndicator, ClearThMessageMetadata.class);
	}

	private void initFromFile(String fileName, String messageEndIndicator)
	{
		if (fileName == null)
			return;
		
		setWriteContent(false);  // Avoiding reader/writer conflict for case when fileName and contentFileName point to the same file
		try
		{
			createMessageFileReader(messageEndIndicator)
					.processMessagesFromFile(Paths.get(fileName), m -> collectMessage(m));
		}
		catch (IOException e)
		{
			logger.warn("Error while reading messages from file '{}'", fileName, e);
		}
		finally
		{
			setWriteContent(true);
		}
	}
	
	private void collectMessage(EncodedClearThMessage message)
	{
		ClearThMessageMetadata metadata = message.getMetadata();
		if (metadata == null || metadata.getDirection() == null || metadata.getDirection() == ClearThMessageDirection.RECEIVED)
			onMessage(message);
	}
	
	
	private class CollectorCleaner implements Runnable
	{
		private final long maxAgeMillis;
		private final long failedMaxAgeMillis;

		public CollectorCleaner(double maxAgeHour, double failedMaxAgeHour)
		{
			if (maxAgeHour > -1)
				this.maxAgeMillis = (long)(maxAgeHour * 60 * 60 * 1000);
			else
				this.maxAgeMillis = (long)-1;

			if (failedMaxAgeHour > -1)
				this.failedMaxAgeMillis = (long)(failedMaxAgeHour * 60 * 60 * 1000);
			else
			{
				// 6 hours by default
				this.failedMaxAgeMillis = 6L * 60 * 60 * 1000;
			}

		}
		
		@Override
		public void run()
		{
			long currentTime = System.currentTimeMillis();
			if (maxAgeMillis > -1)
			{
				Iterator<ReceivedClearThMessage> itr = contentStorage.getContentPassed().values().iterator();
				while (itr.hasNext())
				{
					ReceivedClearThMessage elem = itr.next();
					if (elem != null && currentTime - elem.getReceived() > maxAgeMillis)
						itr.remove();
				}
			}
			
			Iterator<ReceivedStringMessage> itr = contentStorage.getContentFailed().values().iterator();
			while (itr.hasNext())
			{
				ReceivedStringMessage elem = itr.next();
				if (elem != null && currentTime - elem.getReceived() > failedMaxAgeMillis)
					itr.remove();
			}
		}
	}


	protected FileContentStorage<ReceivedClearThMessage, ReceivedStringMessage> createFileContentStorage(String contentsFilePath)
			throws IOException
	{
		return new DefaultFileContentStorage(contentsFilePath, storeTimestamp, 
				String.format("%s (%s)", connectionName, STORE_THREAD_NAME));
	}
	
	
	protected int getDebugLogMessageSizeLimit()
	{
		return DEBUG_LOG_MSG_SIZE_LIMIT;
	}


	protected void setWriteContent(boolean writeContent)
	{
		if (contentStorage instanceof WritingContentStorage)
			((WritingContentStorage<?,?>) contentStorage).setWriteContent(writeContent);
	}

	protected void setWriteBeforeDispose(boolean writeBeforeDispose)
	{
		if (contentStorage instanceof WritingContentStorage)
			((WritingContentStorage<?,?>) contentStorage).setWriteBeforeDispose(writeBeforeDispose);
	}

	private void processFilteredTypes(Set<String> allowedTypes, Set<String> forbiddenTypes)
		throws SettingsException
	{
		if (!allowedTypes.isEmpty() && !forbiddenTypes.isEmpty())
			throw new SettingsException("Error in Collector settings: " + ALLOWED_TYPES + " and " + FORBIDDEN_TYPES + " cannot be used together");
		else if (!forbiddenTypes.isEmpty())
		{
			filteredTypes = forbiddenTypes;
			filterForAllowedTypes = false;
		}
		else
			filteredTypes = allowedTypes;
	}

	private boolean validateType(String messageType)
	{
		if(filteredTypes.isEmpty())
			return true;

		if (filterForAllowedTypes && !filteredTypes.contains(messageType))
			return false;

		return filterForAllowedTypes || !filteredTypes.contains(messageType);
	}
}
