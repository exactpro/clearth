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

package com.exactprosystems.clearth.connectivity.connections2.clients;

import com.exactprosystems.clearth.connectivity.*;
import com.exactprosystems.clearth.connectivity.connections2.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections2.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.messages.MessageFileReader;
import com.exactprosystems.clearth.messages.MessageFileWriter;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

public abstract class BasicClearThClient implements ClearThClient
{
	private static final Logger logger = LoggerFactory.getLogger(BasicClearThClient.class);
	protected final ClearThMessageConnection owner;
	protected final String name;
	protected final Path unhandledMessagesFile;
	protected final ClearThConnectionSettings storedSettings;
	protected final AtomicLong sent;

	protected final List<MessageListener> allListeners,
			receiveListeners,
			sendListeners;

	protected MessageProcessorThread receivedProcessorThread = null,
			sentProcessorThread = null;
	protected MessageReceiverThread receiverThread = null;
	protected BlockingQueue<EncodedClearThMessage> receivedMessageQueue = createMessageQueue(),
			sentMessageQueue;
	protected boolean running = false;

	public BasicClearThClient(ClearThMessageConnection owner) throws ConnectionException, SettingsException
	{
		this.owner = owner;
		name = owner.getName();
		unhandledMessagesFile = createUnhandledMessagesFilePath(name);

		//Copying settings so that owner can change its own settings, but the client will keep the initial ones
		storedSettings = owner.getSettings().copy();
		sent = new AtomicLong(0);

		if (logger.isInfoEnabled())
			logger.info("Initializing client: {}{}Name={}{}{}",
					getClass().getCanonicalName(), Utils.EOL, name, Utils.EOL, storedSettings.toString());

		allListeners = new ArrayList<>();
		receiveListeners = new ArrayList<>();
		sendListeners = new ArrayList<>();
		try
		{
			connect();
		}
		catch (ConnectionException e)
		{
			logger.error("Could not init client for connection '{}', closing all opened related connections", name, e);
			try
			{
				closeConnections();
			}
			catch (ConnectionException e1)
			{
				logger.error("Error while closing related connections", e1);
			}
			throw e;
		}
	}

	protected Path createUnhandledMessagesFilePath(String name)
	{
		if (StringUtils.isEmpty(name))
			throw new IllegalArgumentException("Name is not specified for connection of class " + owner.getClass().getSimpleName());
		return owner.getTypeInfo().getDirectory().resolve(name + ".dat");
	}

	protected abstract void connect() throws ConnectionException, SettingsException;
	protected abstract void closeConnections() throws ConnectionException;

	protected abstract boolean isNeedReceiverThread();
	protected abstract MessageReceiverThread createReceiverThread();

	protected abstract Object doSendMessage(Object message) throws IOException, ConnectivityException;
	protected abstract Object doSendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException;


	protected void loadUnhandledMessages()
	{
		logger.info("{}: reading unhandled messages from file '{}'", name, unhandledMessagesFile);
		try
		{
			createUnhandledMessageFileReader().processMessages(unhandledMessagesFile, m -> receivedMessageQueue.add(m));

			try
			{
				Files.delete(unhandledMessagesFile);
			}
			catch (IOException e)
			{
				logger.warn("{}: could not remove file with unhandled messages after reading it", name, e);
			}
		}
		catch (IOException e)
		{
			logger.warn("{}: error while reading unhandled messages", name, e);
		}
		finally
		{
			logger.info("{}: "+receivedMessageQueue.size()+" unhandled message(s) read", name);
		}
	}


	protected MessageFileReader createUnhandledMessageFileReader()
	{
		return new MessageFileReader();
	}

	protected MessageFileWriter createUnhandledMessageFileWriter(Path file) throws IOException
	{
		return new MessageFileWriter(file, false);
	}


	protected void startListeners()
	{
		for (MessageListener listener : allListeners)
			listener.start();
	}

	protected boolean hasReceiveListeners()
	{
		return !isEmpty(receiveListeners);
	}

	protected boolean hasSendListeners()
	{
		return !isEmpty(sendListeners);
	}

	protected MessageProcessorThread createReceivedProcessorThread()
	{
		return new MessageProcessorThread(name+" (Received processor thread)", receivedMessageQueue, receiveListeners);
	}

	protected MessageProcessorThread createSentProcessorThread()
	{
		return new MessageProcessorThread(name+" (Sent processor thread)", sentMessageQueue, sendListeners);
	}

	@Override
	public void start(boolean startListeners)
	{
		logger.info("Starting connection '"+name+"'");
		logger.trace("{} : startListeners = {}; listeners = {}",
				name, startListeners,
				allListeners == null ? "None" : allListeners.size());

		//Listeners should be started before processor thread, because it can immediately start passing messages to them
		if (startListeners && (allListeners != null))
			startListeners();

		//If there is a file with unhandled messages, let's read them, remove this file and pass messages to internal queue, so that they will be handled by listeners
		if (Files.isRegularFile(unhandledMessagesFile))
			loadUnhandledMessages();

		if (hasReceiveListeners())
		{
			logger.trace("{}: creating received processor thread", name);
			receivedProcessorThread = createReceivedProcessorThread();
			receivedProcessorThread.start();
		}

		if (hasSendListeners())
		{
			sentMessageQueue = createMessageQueue();
			logger.trace("{}: creating sent processor thread", name);
			sentProcessorThread = createSentProcessorThread();
			sentProcessorThread.start();
		}

		if (isNeedReceiverThread())
		{
			receiverThread = createReceiverThread();
			receiverThread.start();
		}

		running = true;
	}


	protected void disposeReceiverThread()
	{
		if (receiverThread != null)
		{
			logger.trace(name + ": disposing receiver thread");
			receiverThread.terminate();
			receiverThread = null;
		}
	}

	protected void disposeReceivedProcessorThread()
	{
		disposeProcessorThread(receivedProcessorThread);
	}

	protected void disposeSentProcessorThread()
	{
		disposeProcessorThread(sentProcessorThread);
	}

	protected void saveUnhandledMessages()
	{
		if (receivedMessageQueue.isEmpty())
			return;

		synchronized (receivedMessageQueue)
		{
			logger.info("{}: {} message(s) remain unhandled, storing them to file '{}'",
					name, receivedMessageQueue.size(), unhandledMessagesFile);

			MessageFileWriter writer = null;
			try
			{
				writer = createUnhandledMessageFileWriter(unhandledMessagesFile);
				EncodedClearThMessage msg;
				while ((msg = receivedMessageQueue.poll()) != null)
					writer.write(msg);
			}
			catch (IOException e)
			{
				logger.warn("{}: could not store unhandled messages to file, {} message(s) lost",
						name, receivedMessageQueue.size(), e);
			}
			finally
			{
				Utils.closeResource(writer);
			}
		}
	}

	protected void disposeListeners()
	{
		//Processor thread terminated, no messages will be passed to listeners, thus it's safe to stop them
		logger.info("{}: disposing listeners", name);
		for (MessageListener listener : allListeners)
			listener.dispose();
	}

	@Override
	public void dispose(boolean disposeListeners) throws ConnectionException
	{
		disposeReceiverThread();
		disposeReceivedProcessorThread();
		disposeSentProcessorThread();

		//If some messages are still in messageQueue (it means they are received, but not handled, i.e. not passed to listeners), 
		//let's store them in file and restore on connection restart
		saveUnhandledMessages();

		if (disposeListeners)
			disposeListeners();

		closeConnections();

		running = false;
	}

	@Override
	public void addMessageListener(MessageListener listener)
	{
		allListeners.add(listener);
		if (listener instanceof ReceiveListener && ((ReceiveListener)listener).isActiveForReceived())
			receiveListeners.add(listener);
		if (listener instanceof SendListener && ((SendListener)listener).isActiveForSent())
			sendListeners.add(listener);
		logger.trace("{}: listener '{}' ({}) added", name, listener.getName(), listener.getType());
	}


	@Override
	public final Object sendMessage(Object message) throws IOException, ConnectivityException
	{
		Object outcome = doSendMessage(message);
		sent.incrementAndGet();

		if (hasSendListeners())
			notifySendListeners(message, null);

		return outcome;
	}

	@Override
	public final Object sendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		Object outcome = doSendMessage(message);
		sent.incrementAndGet();

		if (hasSendListeners())
			notifySendListeners(message.getPayload(), message.getMetadata());

		return outcome;
	}


	public void removeMessageListener(MessageListener listener)
	{
		listener.dispose();
		allListeners.remove(listener);
		receiveListeners.remove(listener);
		sendListeners.remove(listener);
		logger.trace("{}: listener '{}' ({}) removed", name, listener.getName(), listener.getType());
	}

	public boolean isRunning()
	{
		return running;
	}

	public LocalDateTime getStarted()
	{
		if (receiverThread != null)
			return receiverThread.getStarted();
		return null;
	}

	public LocalDateTime getStopped()
	{
		if (receiverThread != null)
			return receiverThread.getStopped();
		return null;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public long getSent()
	{
		if (sentProcessorThread != null)
			return sentProcessorThread.getProcessed();  //Message is considered as sent if it is processed by all send listeners 
		return sent.get();
	}

	@Override
	public long getReceived()
	{
		return receivedProcessorThread != null ? receivedProcessorThread.getProcessed() : 0;
	}

	protected BlockingQueue<EncodedClearThMessage> createMessageQueue()
	{
		return new LinkedBlockingQueue<>();
	}

	protected final void disposeProcessorThread(MessageProcessorThread thread)
	{
		if (thread == null)
			return;

		logger.trace("{}: disposing processor thread '{}'", name, thread.getName());
		thread.terminate();
	}

	protected EncodedClearThMessage createUpdatedMessage(Object payload, ClearThMessageMetadata metadata)
	{
		ClearThMessageMetadata newMetadata = new ClearThMessageMetadata(ClearThMessageDirection.SENT,
				Instant.now(),
				metadata != null ? metadata.getFields() : null);
		return new EncodedClearThMessage(payload, newMetadata);
	}

	protected final void notifySendListeners(Object payload, ClearThMessageMetadata metadata) throws IOException, ConnectivityException
	{
		EncodedClearThMessage updated = createUpdatedMessage(payload, metadata);
		try
		{
			sentMessageQueue.put(updated);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new ConnectionException("Wait for message put into send listeners queue interrupted", e);
		}
	}
}