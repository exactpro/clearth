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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.utils.SettingsException;
import com.ibm.mq.MQException;

import static com.exactprosystems.clearth.connectivity.MQExceptionUtils.isConnectionBroken;

@XmlRootElement(name="AbstractMQConnection")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class MQConnection extends ClearThMessageConnection<MQConnection,MQConnectionSettings>
{
	protected final Object connectionMonitor = new Object();
	
	public MQConnection()
	{
		super();
	}
	
	public abstract Logger getLogger();
	
	protected abstract ClearThClient createMqClient() throws ConnectionException, SettingsException;
		

	public int getReadDelay()
	{
		return settings.readDelay;
	}

	public void setReadDelay(int readDelay)
	{
		this.settings.readDelay = readDelay;
	}
	
	
	public int getCharset()
	{
		return settings.charset;
	}
	
	public void setCharset(int charset)
	{
		this.settings.charset = charset;
	}
	
	
	public String getReceiveQueue()
	{
		return settings.receiveQueue;
	}

	public void setReceiveQueue(String receiveQueue)
	{
		this.settings.receiveQueue = receiveQueue;
	}
	

	public String getSendQueue()
	{
		return settings.sendQueue;
	}

	public void setSendQueue(String sendQueue)
	{
		this.settings.sendQueue = sendQueue;
	}
	

	public boolean isUseReceiveQueue()
	{
		return settings.useReceiveQueue;
	}
	
	public void setUseReceiveQueue(boolean useReceiveQueue)
	{
		this.settings.useReceiveQueue = useReceiveQueue;
	}
	
	
	public String getHostname()
	{
		return settings.hostname;
	}

	public void setHostname(String hostname)
	{
		this.settings.hostname = hostname;
	}
	

	public int getPort()
	{
		return settings.port;
	}

	public void setPort(int port)
	{
		this.settings.port = port;
	}
	

	public String getQueueManager()
	{
		return settings.queueManager;
	}

	public void setQueueManager(String queueManager)
	{
		this.settings.queueManager = queueManager;
	}
	

	public String getChannel()
	{
		return settings.channel;
	}

	public void setChannel(String channel)
	{
		this.settings.channel = channel;
	}


	public boolean isAutoReconnect()
	{
		return settings.autoReconnect;
	}
	
	public void setAutoReconnect(boolean autoReconnect)
	{
		this.settings.autoReconnect = autoReconnect;
	}

	public int getRetryAttemptCount() {
		return settings.retryAttemptCount;
	}

	public void setRetryAttemptCount(int retryAttemptCount) {
		this.settings.retryAttemptCount = retryAttemptCount;
	}

	public long getRetryTimeout() {
		return settings.retryTimeout;
	}

	public void setRetryTimeout(long retryTimeout) {
		this.settings.retryTimeout = retryTimeout;
	}

	@Override
	public boolean isAutoConnect()
	{
		return settings.autoConnect;
	}
	
	public void setAutoConnect(boolean autoConnect)
	{
		this.settings.autoConnect = autoConnect;
	}
	
	public void sendByteMessage(byte[] message, ClearThMessageMetadata metadata) throws ConnectionException, IOException, MQException
	{
		if (!running)
			throw new ConnectionException("Connection '"+name+"' is not running");
		((MQClient)client).sendByteMessage(message, metadata);
	}
	
	@Override
	public void start() throws SettingsException, Exception
	{
		synchronized (connectionMonitor)  //Preventing from starting connection twice if requested simultaneously
		{
			Logger logger = getLogger();
			
			if (isRunning())
			{
				logger.warn("Connection '"+name+"' is already running");
				return;
			}

			ClearThCore.getInstance().getConnectionStorage().getConnectionStartValidator().checkIfCanStartConnection(this);

			started = null;
			stopped = null;

			List<ReceiveListener> listenerImplementations;
			try
			{
				listenerImplementations = createListeners(listeners);
			}
			catch (Exception e)
			{
				String msg = "Could not start connection '"+name+"' - error while creating listeners";
				logger.error(msg, e);
				throw e;
			}
			
			try
			{
				logger.debug("Trying to start connection '"+name+"'");
				client = createMqClient();
			}
			catch (SettingsException e)
			{
				disposeListeners();
				String msg = "Could not start connection '"+name+"' - invalid settings";
				logger.error(msg, e);
				throw e;
			}
			catch (Exception e)
			{
				disposeListeners();
				String msg = "Could not start connection '"+name+"'";
				logger.error(msg, e);
				throw e;
			}
			
			client.addReceiveListeners(listenerImplementations);
			
			client.start(true);
			started = new Date();
			running = true;
			ClearThCore.getInstance().getConnectionStorage().removeStoppedConnectionErrors(name);
			logger.info("Connection '"+name+"' is now running");
		}
	}
	
	public boolean reconnect() throws ConnectivityException, ConnectionException
	{
		synchronized (connectionMonitor)
		{
			Logger logger = getLogger();
			
			if (!isRunning())
			{
				logger.warn("Connection '"+name+"' is stopped, reconnect won't be performed");
				return false;
			}
			
			if (client!=null)
			{
				try
				{
					client.dispose(false);  //Keeping listeners to reuse the same instances after reconnect
				}
				catch (ConnectionException e)
				{
					if (!isConnectionBroken(e))
						logger.warn(name+": errors occurred while closing client before reconnect. In spite of that reconnect will be performed", e);
				}
				client = null;
			}
			
			try
			{
				client = createMqClient();
				for (ListenerConfiguration listener : listeners)
					client.addReceiveListener(listener.getImplementation());
				client.start(false);  //Listeners are already running, started by previous client instance
				started = new Date();
				stopped = null;
				logger.info("Connection '"+name+"' reconnected");
				return true;
			}
			catch (Exception e)
			{
				logger.error("Could not reconnect connection '"+name+"'", e);
				client = null;
			}
			return false;
		}
	}
	
	
	protected void disconnect() throws ConnectivityException
	{
		Logger logger = getLogger();
		if (client!=null)
		{
			try
			{
				logger.debug("Trying to stop connection '"+name+"'");
				client.dispose(true);  //Will also dispose listeners
			}
			catch (ConnectionException e)
			{
				if (isConnectionBroken(e))
				{
					logger.debug(name+": connection is broken, closing connection on our side");
					//See below where "client = null"
				}
				else
				{
					logger.error("Unexpected MQ exception while closing connection '"+name+"'", e);
					throw e;
				}
			}
			catch (ConnectivityException e)
			{
				logger.error("Could not close connection '"+name+"'", e);
				throw e;
			}
			
			client = null;
		}
		else  //Need to dispose listeners so they can finish their work correctly, i.e. store something in files
		{
			logger.debug("Client already destroyed, disposing listeners");
			for (ListenerConfiguration listener : listeners)
				if (listener.getImplementation()!=null)
					listener.getImplementation().dispose();
		}
		
		//No need to keep listeners instances of disposed connection
		for (ListenerConfiguration listener : listeners)
			listener.setImplementation(null);
	}
	
	@Override
	public void stop() throws ConnectivityException
	{
		synchronized (connectionMonitor)
		{
			Logger logger = getLogger();
			
			if (!isRunning())
			{
				logger.warn("Connection '"+name+"' is already stopped");
				return;
			}
			
			disconnect();
			
			stopped = new Date();
			running = false;
			logger.info("Connection '"+name+"' is now stopped");
		}
	}
	
	
	public long getReceived()
	{
		if (client != null)
			return ((MQClient)client).getReceived();
		else
			return 0;
	}
	
	
	public long getSent()
	{
		if (client != null)
			return ((MQClient)client).getSent();
		else
			return 0;
	}
	
	
	public long getWarnings()
	{
		if (client != null)
			return ((MQClient)client).getWarnings();
		else
			return 0;
	}

	public void setWarnings(long warnings)
	{
		if (client != null)
			((MQClient)client).setWarnings(warnings);
	}
	

	public MQClient getClient()
	{
		return ((MQClient)client);
	}

	public void setClient(MQClient client)
	{
		this.client = client;
	}
	
	@Override
	public String connectionFilePath()
	{
		return ClearThCore.connectionsPath();
	}

}
