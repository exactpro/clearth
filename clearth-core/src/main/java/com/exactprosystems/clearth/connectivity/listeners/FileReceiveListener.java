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

package com.exactprosystems.clearth.connectivity.listeners;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.connectivity.ListenerDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.ReceiveListener;
import com.exactprosystems.clearth.connectivity.SettingsDetails;

@ListenerDescription(description = "ClearTH file receive listener")
@SettingsDetails(details = "Path to an output file where to store messages.")
public class FileReceiveListener extends ReceiveListener
{
	protected static Logger logger = LoggerFactory.getLogger(FileReceiveListener.class);
	protected BufferedWriter writer;

	/**
	 * Create FileReceiveListener
	 * 
	 * @param fileName
	 *          file's name for storing messages to HDD
	 * @throws IOException 
	 */
	public FileReceiveListener(String fileName) throws IOException
	{
		String fn = ClearThCore.rootRelative(fileName);
		writer = new BufferedWriter(new FileWriter(fn, true));
		logger.debug("File '{}' opened for writing", fn);
	}
	
	@Override
	public void start()
	{
	}

	@Override
	public void onMessageReceived(String message, long receivedTimestamp)
	{
		try
		{
			writer.write(format.get().format(receivedTimestamp));
			writer.newLine();
			writer.write(message);
			writer.newLine();
			writer.newLine();
			writer.flush();
			logger.trace("Received message: {}", message);
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

	@Override
	protected Logger getLogger()
	{
		return logger;
	}
}
