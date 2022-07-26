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

package com.exactprosystems.clearth.connectivity.listeners.storage;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.exactprosystems.clearth.connectivity.iface.ReceivedClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ReceivedStringMessage;
import com.exactprosystems.clearth.messages.MessageFileReader;

public class DefaultFileContentStorage extends FileContentStorage<ReceivedClearThMessage, ReceivedStringMessage>
{
	private final DateFormat formatter = new SimpleDateFormat(MessageFileReader.DEFAULT_TIMESTAMP_FORMAT);
	
	public DefaultFileContentStorage(String contentsFilePath, String threadName) throws IOException
	{
		super(contentsFilePath, threadName);
	}

	public DefaultFileContentStorage(String contentsFilePath, boolean storeTimestamp, String threadName) throws IOException
	{
		super(contentsFilePath, storeTimestamp, threadName);
	}
	

	@Override
	protected String extractContentPassed(ReceivedClearThMessage item)
	{
		if (item == null || item.getMessage() == null)
			return "";
		return item.getMessage().getEncodedMessage();
	}

	@Override
	protected String extractContentFailed(ReceivedStringMessage item)
	{
		if (item == null)
			return "";
		return item.getMessage();
	}

	@Override
	protected String extractTimestampPassed(ReceivedClearThMessage item)
	{
		if (item == null)
			return "";
		return formatter.format(item.getReceived());
	}
}
