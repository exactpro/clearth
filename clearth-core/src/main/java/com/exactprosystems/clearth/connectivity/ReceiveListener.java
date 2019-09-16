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
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;

import com.exactprosystems.clearth.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParsePosition;

import static java.lang.System.currentTimeMillis;

public abstract class ReceiveListener
{
    public static final ThreadLocal<SimpleDateFormat> format =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS"));

	public abstract void start();
	

	public abstract void onMessageReceived(String message, long receivedTimestamp);

	/**
	 * Handler of message
	 * @param message message
	 */
	public void onMessageReceived(String message)
	{
		onMessageReceived(message, currentTimeMillis());
	}
	
	/**
	 * Dispose listener
	 */
	public abstract void dispose();
	
	protected abstract Logger getLogger();
	
	protected boolean isTimestamp(String str)
	{
		ParsePosition position = new ParsePosition(0);
		Date date = format.get().parse(str, position);
		if (date == null)
			return false;
		return position.getIndex() >= str.length();
	}

	protected long parseTimestamp(String str)
	{
		Date date = format.get().parse(str, new ParsePosition(0));
		if (date == null)
		{
			getLogger().trace("Cannot parse timestamp value '{}'", str);
			return -1;
		}
		return date.getTime();
	}

	public void processMessagesFromFile(String fileName, String messageEndIndicator)
	{
		try
		{
			StringBuilder sb;
			long timestamp = -1;
			try (BufferedReader reader = new BufferedReader(new FileReader(fileName)))
            {
                sb = new StringBuilder();
                while (reader.ready())
                {
                    String s = reader.readLine();
                    if (isTimestamp(s))
                    {
                        timestamp = parseTimestamp(s);
                        continue;
                    }
                    boolean endMessage;
                    if (s != null && !s.trim().isEmpty())
                    {
                        if (messageEndIndicator != null)
                            endMessage = s.startsWith(messageEndIndicator);
                        else
                            endMessage = false;

                        if (sb.length() > 0)
                            sb.append(Utils.EOL);
                        sb.append(s);
                    }
                    else
                    {
                        endMessage = true;
                    }

					if (endMessage)
					{
						if (sb.length() > 0)
						{
							receiveMessage(sb.toString(), timestamp);
							sb = new StringBuilder();
						}
                    }
                }
            }
			if (sb.length() > 0)
				receiveMessage(sb.toString(), timestamp);
		}
		catch (IOException e)
		{
			getLogger().error("Unable to load messages from file '{}'", fileName, e);
		}
	}

	protected void receiveMessage(String msg, long timestamp)
	{
		if (timestamp > 0)
			onMessageReceived(msg, timestamp);
		else
			onMessageReceived(msg);
	}
}
