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

package com.exactprosystems.clearth.tools;

import static com.exactprosystems.clearth.ClearThCore.connectionStorage;
import static com.exactprosystems.clearth.utils.Utils.nvl;

import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.clearth.connectivity.CollectorMessage;
import com.exactprosystems.clearth.connectivity.ReceiveListener;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.ReceivedClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ReceivedStringMessage;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;


/**
 * Created by alexander.magomedov on 11/3/16.
 */
public class CollectorScannerTool
{
	protected String collectorFilter;
	
	public CollectorScannerTool()
	{
		this.collectorFilter = null;
	}
	
	private boolean checkFilter(String checkedValue)
	{
		return (this.collectorFilter == null || this.collectorFilter.isEmpty() || checkedValue.contains(collectorFilter));
	}
	
	private ReceiveListener getCollector(ClearThMessageConnection<?, ?> connection)
	{
		return connection.findListener(connection.getMessageCollectorClass());
	}


	/**
	 * Returns list of CollectorMessages (correct message, parsed message, timestamp) from selected
	 * collector
	 *
	 * @param collector
	 *          selected collector
	 * @return list of correct messages from collector
	 */
	public List<CollectorMessage> getCollectorMessages(ReceiveListener collector)
	{
		if (collector == null)
			return null;

		//Using try-catch block to get around NPE while connection is stopping and is chosen in 'Collector scanner' tool
		try
		{
			List<CollectorMessage> msgList = new ArrayList<CollectorMessage>();
			if (collector instanceof ClearThMessageCollector)
			{
				ClearThMessageCollector imc = (ClearThMessageCollector) collector;
				for (ReceivedClearThMessage msgData : imc.getMessagesData())
				{
					String encoded = nvl(msgData.getMessage().getEncodedMessage(), "[could not get original message]");
					if (checkFilter(encoded))
						msgList.add(new CollectorMessage(encoded, msgData.getMessage().toString(), msgData.getReceived()));
				}
			}
	
			if (msgList.size() > 0)
				return msgList;
			else
				return null;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Returns list of CollectorMessages (correct message, parsed message, timestamp) from selected
	 * connection
	 *
	 * @param connection
	 *          selected connection
	 * @return list of correct messages from collector
	 */
	public List<CollectorMessage> getCollectorMessages(ClearThMessageConnection<?, ?> connection)
	{
		return (connection == null) ? null : getCollectorMessages(getCollector(connection));
	}


	public List<CollectorMessage> getCollectorMessagesFailed(ReceiveListener collector)
	{
		if (collector == null)
			return null;
		if (!(collector instanceof ClearThMessageCollector))
			return null;
		
		// Use try-catch block to get around NPE while connection is stopping and is chosen in 'Collector scanner' tool
		try
		{
			ClearThMessageCollector imc = (ClearThMessageCollector)collector;
			List<CollectorMessage> msgList = new ArrayList<CollectorMessage>();
			for (ReceivedStringMessage msgData : imc.getMessagesFailed())
			{
				if (checkFilter(msgData.getMessage()))
					msgList.add(new CollectorMessage(msgData.getMessage(), "[could not get parsed message]", msgData.getReceived()));
			}
			
			if (msgList.size() > 0)
				return msgList;
			else
				return null;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public List<CollectorMessage> getCollectorMessagesFailed(ClearThMessageConnection<?, ?> connection)
	{
		return (connection == null) ? null : getCollectorMessagesFailed(getCollector(connection));
	}

	public ClearThMessageConnection getConnectionByName(String name)
	{
		return (ClearThMessageConnection) connectionStorage().findConnection(name);
	}


	/**
	 * Returns names of all message connections which contain collector
	 * 
	 * @return list of connections names
	 */
	public List<String> getCollectingConnections()
	{
		return connectionStorage().listConnections(con -> 
		{
			if (ClearThMessageConnection.isMessageConnection(con))
			{
				ClearThMessageConnection<?, ?> msgCon = (ClearThMessageConnection<?, ?>) con;
				return msgCon.isRunning() && (msgCon.findListener(msgCon.getMessageCollectorClass()) != null);
			}
			else 
				return false;
		});
	}

	public String getCollectorFilter()
	{
		return collectorFilter;
	}

	public void setCollectorFilter(String collectorFilter)
	{
		this.collectorFilter = collectorFilter;
	}
}
