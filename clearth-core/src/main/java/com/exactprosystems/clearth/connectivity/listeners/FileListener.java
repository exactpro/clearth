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

import java.io.IOException;
import java.nio.file.Paths;

import com.exactprosystems.clearth.ClearThCore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.ListenerDescription;
import com.exactprosystems.clearth.connectivity.ListenerProperties;
import com.exactprosystems.clearth.connectivity.ReceiveListener;
import com.exactprosystems.clearth.connectivity.SendListener;
import com.exactprosystems.clearth.connectivity.SettingsDetails;
import com.exactprosystems.clearth.connectivity.iface.AbstractMessageListener;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.messages.MessageFileWriter;

@ListenerDescription(description = "ClearTH file receive listener")
@SettingsDetails(details = "Path to an output file where to store messages.")
public class FileListener extends AbstractMessageListener implements ReceiveListener, SendListener
{
	private static Logger logger = LoggerFactory.getLogger(FileListener.class);
	
	private final MessageFileWriter writer;
	private final Object monitor;
	
	/**
	 * Create FileReceiveListener
	 * @param properties listener properties (name, active directions)
	 * @param fileName name of file to store messages on disk
	 * @throws IOException if file creation failed
	 */
	public FileListener(ListenerProperties properties, String fileName) throws IOException
	{
		super(properties);
		
		String fn = ClearThCore.rootRelative(fileName);
		writer = createMessageFileWriter(fn);
		monitor = new Object();
		logger.debug("File '{}' opened for writing", fn);
	}
	
	
	@Override
	public boolean isActiveForReceived()
	{
		return getProperties().isActiveForReceived();
	}
	
	@Override
	public boolean isActiveForSent()
	{
		return getProperties().isActiveForSent();
	}
	
	@Override
	public void start()
	{
	}
	
	@Override
	public void onMessage(EncodedClearThMessage message)
	{
		try
		{
			synchronized (monitor)  //This avoids threads clash when listener is active for sent and received messages
			{
				writer.write(message);
				writer.flush();
				logger.trace("Received message: {}", message);
			}
		}
		catch (IOException e)
		{
			logger.error("Could not write message into file", e);
		}
	}
	
	@Override
	public void dispose()
	{
		try
		{
			writer.flush();
			writer.close();
			logger.trace("File listener disposed");
		}
		catch (Exception e)
		{
			logger.error("Error while disposing file listener", e);
		}
	}
	
	
	protected MessageFileWriter createMessageFileWriter(String fileName) throws IOException
	{
		return new MessageFileWriter(Paths.get(fileName), true);
	}
}
