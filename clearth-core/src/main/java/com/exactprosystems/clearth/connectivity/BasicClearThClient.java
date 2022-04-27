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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.Utils;

public abstract class BasicClearThClient implements ClearThClient
{
	protected final MQConnection owner;
	protected final String name;
	protected final File unhandledMessagesFile;
	protected final MQConnectionSettings storedSettings;
	protected AtomicLong warnings, sent;
	protected final ValueGenerator msgIdGen;
	protected final Object sendMonitor = new Object();
	
	protected final List<ReceiveListener> receiveListeners;
	
	protected MessageProcessorThread processorThread = null;
	protected MessageReceiverThread receiverThread = null;
	protected BlockingQueue<Pair<String, Date>> messageQueue = new LinkedBlockingQueue<Pair<String, Date>>();
	protected boolean running = false;
	protected final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public BasicClearThClient(MQConnection owner) throws ConnectionException, SettingsException
	{
		this.owner = owner;
		name = owner.getName();
		unhandledMessagesFile = new File(ClearThCore.connectionsPath(), name+".dat");
		
		//Copying settings so that owner can change its own settings, but the client will keep the initial ones
		storedSettings = owner.getSettings().copy();
		warnings = new AtomicLong(0);
		sent = new AtomicLong(0);
		
		Logger logger = getLogger();
		
		if (logger.isInfoEnabled())
			logger.info("Initializing client: "+getClass().getCanonicalName()+Utils.EOL+"Name=" + name + Utils.EOL + storedSettings.toString());
		
		msgIdGen = getValueGenerator();
		receiveListeners = new ArrayList<ReceiveListener>();
		try
		{
			connect();
		}
		catch (ConnectionException e)
		{
			logger.error("Could not init client for connection '"+name+"', closing all opened related connections", e);
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
	
	protected abstract MessageProcessorThread createProcessorThread();
	protected abstract MessageReceiverThread createReceiverThread();
	
	protected abstract boolean isConnectionBrokenError(Throwable error);
	
	
	protected void loadUnhandledMessages()
	{
		Logger logger = getLogger();
		
		logger.info(name+": reading unhandled messages from file '"+unhandledMessagesFile.getName()+"'");
		boolean toDelete = false;
		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(unhandledMessagesFile));
			StringBuilder sb = new StringBuilder();
			Date d = null;
			while (reader.ready())
			{
				String s = reader.readLine();
				boolean endMessage;
				if (s != null && !s.isEmpty())
				{
					endMessage = false;
					if (d==null)
						try
						{
							d = format.parse(s);
						}
						catch (ParseException e)
						{
							d = new Date();
						}
					else
					{
						if (sb.length() > 0)
							sb.append(Utils.EOL);
						sb.append(s);
					}
				}
				else
					endMessage = true;
				
				if (endMessage)
				{
					if (sb.length()>0)
					{
						messageQueue.add(new Pair<String, Date>(sb.toString(), d));
						sb = new StringBuilder();
						d = null;
					}
				}
			}
			if (sb.length()>0)
				messageQueue.add(new Pair<String, Date>(sb.toString(), d));
			toDelete = true;
		}
		catch (IOException e)
		{
			logger.warn(name+": error while reading unhandled messages", e);
		}
		finally
		{
			Utils.closeResource(reader);
			if (toDelete)
				if (!unhandledMessagesFile.delete())
					logger.warn(name+": could not remove file with unhandled messages after reading it");
			logger.info(name+": "+messageQueue.size()+" unhandled messages read");
		}
	}
	
	
	protected void startListeners()
	{
		for (ReceiveListener listener : receiveListeners)
			listener.start();
	}
	
	protected boolean isNeedProcessorThread()
	{
		return true;
	}
	
	@Override
	public void start(boolean startListeners)
	{
		getLogger().info("Starting connection '"+name+"'");
		if (getLogger().isTraceEnabled())
			getLogger().trace(name+": startListeners = " + startListeners + "; receiveListeners size = " + (receiveListeners == null ? "Null" : receiveListeners.size()));
		
		//Listeners should be started before processor thread, because it can immediately start passing messages to them
		if (startListeners && (receiveListeners != null))
			startListeners();
		
		//If there is a file with unhandled messages, let's read them, remove this file and pass messages to internal queue, so that they will be handled by listeners
		if (getLogger().isTraceEnabled())
			getLogger().trace(name+": loading unhandled messages");
		if (unhandledMessagesFile.isFile())
			loadUnhandledMessages();
		
		getLogger().trace(name+": creating processor thread");
		if (isNeedProcessorThread())
		{
			processorThread = createProcessorThread();
			processorThread.start();
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
	
	protected void disposeProcessorThread()
	{
		if (processorThread != null)
		{
			getLogger().trace(name + ": disposing processor thread");
			processorThread.terminate();
			processorThread = null;
		}
	}
	
	protected void saveUnhandledMessage(Pair<String, Date> msg, PrintWriter writer)
	{
		writer.println(format.format(msg.getSecond()));
		writer.println(msg.getFirst() + Utils.EOL);
	}
	
	protected void saveUnhandledMessages()
	{
		if (messageQueue.isEmpty())
			return;
		
		synchronized (messageQueue)
		{
			getLogger().info(name + ": " + messageQueue.size() + " messages remain unhandled, storing them to file '" + unhandledMessagesFile.getName() + "'");
			PrintWriter writer = null;
			try
			{
				writer = new PrintWriter(unhandledMessagesFile);
				Pair<String, Date> msg;
				while ((msg = messageQueue.poll()) != null)
					saveUnhandledMessage(msg, writer);
			}
			catch (IOException e)
			{
				getLogger().warn(name + ": could not store unhandled messages to file, " + messageQueue.size() + " messages lost", e);
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
		getLogger().info(name + ": disposing listeners");
		for (ReceiveListener listener : receiveListeners)
			listener.dispose();
	}
	
	@Override
	public void dispose(boolean disposeListeners) throws ConnectionException
	{
		disposeReceiverThread();
		disposeProcessorThread();
		
		//If some messages are still in messageQueue (it means they are received, but not handled, i.e. not passed to listeners), 
		//let's store them in file and restore on connection restart
		saveUnhandledMessages();
		
		if (disposeListeners)
			disposeListeners();
		
		closeConnections();
			
		running = false;
	}
	
	
	public int getFirstPackageSize()
	{
		if (receiverThread instanceof DiverReceiverThread)
			return ((DiverReceiverThread) receiverThread).getFirstPackageSize();
		return 0;
	}
	
	protected void handleSendError(Throwable error, final Object message) throws ConnectivityException
	{
		if (isConnectionBrokenError(error))  //Broken connection indicates a need to make one reconnect attempt
		{
			if (owner.reconnect())
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
	public void addReceiveListener(ReceiveListener listener)
	{
		if (!receiveListeners.contains(listener))
		{
			receiveListeners.add(listener);
			getLogger().trace("{}: {} added", name, listener);
		}
	}
	
	@Override
	public void addReceiveListeners(List<ReceiveListener> listeners)
	{
		for (ReceiveListener listener : listeners)
		{
			addReceiveListener(listener);
		}
	}
	
	public void removeReceiveListener(ReceiveListener listener)
	{
		listener.dispose();
		receiveListeners.remove(listener);
		getLogger().trace("{}: {} removed", name, listener);
	}
	
	/**
	 * Notify all message handlers
	 * @param message message
	 */
	public void notifyReceiveListeners(String message)
	{
		for (ReceiveListener listener : receiveListeners)
		{
			try
			{
				getLogger().trace(name+": notifying receive listener");
				listener.onMessageReceived(message);
			}
			catch (Throwable e)
			{
				getLogger().error(e.getMessage(), e);
			}
		}
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
	
	public long getReceived()
	{
		if (processorThread != null)
			return processorThread.getProcessed();
		return 0;
	}
	
	
	public long getWarnings()
	{
		return warnings.get();
	}
	
	public void setWarnings(long warnings)
	{
		this.warnings.set(warnings);
	}
	
	public void incWarnings()
	{
		this.warnings.addAndGet(1);
	}
	
	
	public long getSent()
	{
		return sent.get();
	}
	
	public void setSent(long sent)
	{
		this.sent.set(sent);
	}
	
	public void incSent()
	{
		this.sent.addAndGet(1);
	}
}
