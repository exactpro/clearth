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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.messages.MessageFileReader;
import com.exactprosystems.clearth.messages.MessageFileWriter;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;

public abstract class BasicClearThClient<C extends ClearThMessageConnection<C, S>, S extends ClearThConnectionSettings<S>> implements ClearThClient
{
	protected final C owner;
	protected final String name;
	protected final Path unhandledMessagesFile;
	protected final S storedSettings;
	protected final AtomicLong warnings, sent;
	protected final ValueGenerator msgIdGen;
	protected final Object sendMonitor = new Object();
	
	protected final List<MessageListener> allListeners,
			receiveListeners,
			sendListeners;
	
	protected MessageProcessorThread receivedProcessorThread = null,
			sentProcessorThread = null;
	protected MessageReceiverThread receiverThread = null;
	protected BlockingQueue<EncodedClearThMessage> receivedMessageQueue = createMessageQueue(),
			sentMessageQueue;
	protected boolean running = false;
	
	public BasicClearThClient(C owner) throws ConnectionException, SettingsException
	{
		this.owner = owner;
		name = owner.getName();
		unhandledMessagesFile = Paths.get(ClearThCore.connectionsPath()).resolve(name+".dat");
		
		//Copying settings so that owner can change its own settings, but the client will keep the initial ones
		storedSettings = owner.getSettings().copy();
		warnings = new AtomicLong(0);
		sent = new AtomicLong(0);
		
		Logger logger = getLogger();
		
		if (logger.isInfoEnabled())
			logger.info("Initializing client: {}{}Name={}{}{}", 
					getClass().getCanonicalName(), Utils.EOL, name, Utils.EOL, storedSettings.toString());
		
		msgIdGen = getValueGenerator();
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
	
	protected abstract void connect() throws ConnectionException, SettingsException;
	protected abstract void closeConnections() throws ConnectionException;
	protected abstract Logger getLogger();
	protected abstract ValueGenerator getValueGenerator();
	
	protected abstract boolean isNeedReceiverThread();
	protected abstract MessageReceiverThread createReceiverThread();
	
	protected abstract boolean isConnectionBrokenError(Throwable error);
	protected abstract Object doSendMessage(Object message) throws IOException, ConnectivityException;
	protected abstract Object doSendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException;
	
	
	private void loadUnhandledMessages()
	{
		Logger logger = getLogger();
		
		logger.info("{}: reading unhandled messages from file '{}'", name, unhandledMessagesFile);
		try
		{
			createUnhandledMessageFileReader().processMessagesFromFile(unhandledMessagesFile, m -> receivedMessageQueue.add(m));
			
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
	
	protected boolean isNeedReceivedProcessorThread()
	{
		return true;
	}
	
	protected boolean isNeedSentProcessorThread()
	{
		return sendListeners != null && !sendListeners.isEmpty();
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
		getLogger().info("Starting connection '"+name+"'");
		getLogger().trace("{} : startListeners = {}; listeners = {}", 
				name, startListeners, 
				allListeners == null ? "None" : allListeners.size());
		
		//Listeners should be started before processor thread, because it can immediately start passing messages to them
		if (startListeners && (allListeners != null))
			startListeners();
		
		//If there is a file with unhandled messages, let's read them, remove this file and pass messages to internal queue, so that they will be handled by listeners
		if (Files.isRegularFile(unhandledMessagesFile))
			loadUnhandledMessages();
		
		if (isNeedReceivedProcessorThread())
		{
			getLogger().trace("{}: creating received processor thread", name);
			receivedProcessorThread = createReceivedProcessorThread();
			receivedProcessorThread.start();
		}
		
		if (isNeedSentProcessorThread())
		{
			sentMessageQueue = createMessageQueue();
			getLogger().trace("{}: creating sent processor thread", name);
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
			getLogger().trace(name + ": disposing receiver thread");
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
			getLogger().info("{}: {} message(s) remain unhandled, storing them to file '{}'",
					name, receivedMessageQueue.size(), unhandledMessagesFile);
			
			MessageFileWriter writer = null;
			try
			{
				writer = createUnhandledMessageFileWriter(unhandledMessagesFile);
				EncodedClearThMessage msg;
				while ((msg = receivedMessageQueue.poll()) != null)
					writer.writeMessage(msg);
			}
			catch (IOException e)
			{
				getLogger().warn("{}: could not store unhandled messages to file, {} message(s) lost", 
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
		getLogger().info("{}: disposing listeners", name);
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
	
	protected void handleSendError(Throwable error, final Object message) throws ConnectivityException
	{
		if (isConnectionBrokenError(error))  //Broken connection indicates a need to make one reconnect attempt
		{
			if (owner.restart())
			{
				try
				{
					owner.sendMessage(message);
				}
				catch (ConnectionException ex)
				{
					//No need to close connection here - it will be closed by owner.stop() called from inside of owner.sendMessage() if exception occurred there and next reconnect attempt failed
					throw new ConnectionException("Could not send message, reconnect attempt successful, but right after that messages still can't be sent. This indicates instability in queue access", ex);
				}
				catch (IOException ex)
				{
					try
					{
						getLogger().error("Error while sending message.", ex);
						owner.stop();
					}
					catch (Exception ex1)
					{
						getLogger().error(name+": error occurred while stopping connection after successful reconnect and failed attempt to re-send message", ex1);
					}
					throw new ConnectionException("Could not send message, connection is broken, re-send attempt failed after reconnect");
				}
			}
			else
			{
				try
				{
					getLogger().error("Connection is not reconnected.");
					owner.stop();
				}
				catch (Exception ex)
				{
					getLogger().error(name+": error occurred while stopping connection after failed reconnect", ex);
				}
				throw new ConnectionException("Could not send message, connection is broken, reconnect attempt failed");
			}
		}
		else  //Error with no handler occurred, closing connection
		{
			try
			{
				getLogger().error("Error occured while sending message. Connection will be stopped.",error);
				owner.stop();
			}
			catch (Exception ex)
			{
				getLogger().error(name+": error occurred while stopping connection on unhandled error", ex);
			}
			throw new ConnectionException(error.getMessage());
		}
	}
	
	@Override
	public void addMessageListener(MessageListener listener)
	{
		allListeners.add(listener);
		if (listener instanceof ReceiveListener && ((ReceiveListener)listener).isActiveForReceived())
			receiveListeners.add(listener);
		if (listener instanceof SendListener && ((SendListener)listener).isActiveForSent())
			sendListeners.add(listener);
		getLogger().trace("{}: listener '{}' ({}) added", name, listener.getName(), listener.getType());
	}
	
	@Override
	public void addMessageListeners(List<MessageListener> listeners)
	{
		for (MessageListener listener : listeners)
			addMessageListener(listener);
	}
	
	
	@Override
	public final Object sendMessage(Object message) throws IOException, ConnectivityException
	{
		Object outcome = doSendMessage(message);
		sent.incrementAndGet();
		
		if (isNeedSentProcessorThread())
			notifySendListeners(message, null);
		
		return outcome;
	}
	
	@Override
	public final Object sendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		Object outcome = doSendMessage(message);
		sent.incrementAndGet();
		
		if (isNeedSentProcessorThread())
			notifySendListeners(message.getPayload(), message.getMetadata());
		
		return outcome;
	}
	
	
	public void removeMessageListener(MessageListener listener)
	{
		listener.dispose();
		allListeners.remove(listener);
		receiveListeners.remove(listener);
		sendListeners.remove(listener);
		getLogger().trace("{}: listener '{}' ({}) removed", name, listener.getName(), listener.getType());
	}
	
	
	/**
	 * Notify all message handlers for received messages
	 * @param message received message to notify listeners about
	 */
	public void notifyReceiveListeners(EncodedClearThMessage message)
	{
		notifyListeners(message, ClearThMessageDirection.RECEIVED, receiveListeners);
	}
	
	/**
	 * Notify all message handlers for sent messages
	 * @param message sent message to notify listeners about
	 */
	public void notifySendListeners(EncodedClearThMessage message)
	{
		notifyListeners(message, ClearThMessageDirection.SENT, sendListeners);
	}
	
	
	public boolean isRunning()
	{
		return running;
	}
	
	public Date getStarted()
	{
		if (receiverThread != null)
			return receiverThread.getStarted();
		return null;
	}
	
	public Date getStopped()
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
	
	public long getWarnings()
	{
		return warnings.get();
	}
	
	
	protected BlockingQueue<EncodedClearThMessage> createMessageQueue()
	{
		return new LinkedBlockingQueue<EncodedClearThMessage>();
	}
	
	protected final void notifyListeners(EncodedClearThMessage message, ClearThMessageDirection direction, Collection<MessageListener> listeners)
	{
		String dir = direction == ClearThMessageDirection.RECEIVED ? "receive" : "send";
		for (MessageListener listener : listeners)
		{
			String listenerName = listener.getName(), 
					listenerType = listener.getType();
			try
			{
				getLogger().trace("{}: notifying {} listener '{}' ({})", name, dir, listenerName, listenerType);
				listener.onMessage(message);
			}
			catch (Throwable e)
			{
				getLogger().error("Error in '{}' while notifying {} listener '{}' ({})", name, dir, listenerName, listenerType, e);
			}
		}
	}
	
	protected final void disposeProcessorThread(MessageProcessorThread thread)
	{
		if (thread == null)
			return;
		
		getLogger().trace("{}: disposing processor thread '{}'", name, thread.getName());
		thread.terminate();
	}
	
	protected EncodedClearThMessage createUpdatedMessage(Object payload, ClearThMessageMetadata metadata)
	{
		ClearThMessageMetadata newMetadata = new ClearThMessageMetadata(ClearThMessageDirection.SENT, 
				Instant.now(), 
				metadata != null ? metadata.fieldsAsMap() : null);
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
