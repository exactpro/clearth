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

package com.exactprosystems.clearth.messages;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class MessageFileReader
{
	public static final ObjectMapper JSON_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule());
	
	private static final Logger logger = LoggerFactory.getLogger(MessageFileReader.class);
	private static final ObjectReader JSON_READER = JSON_MAPPER.reader();
	
	public static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy.MM.dd HH:mm:ss.SSS",
			DEFAULT_MESSAGE_END_INDICATOR = Utils.EOL+Utils.EOL;
	
	private final DateFormat format;
	private final String messageEndIndicator;
	private final Class<ClearThMessageMetadata> metadataClass;
	
	public MessageFileReader(String timestampFormat, String messageEndIndicator, Class<ClearThMessageMetadata> metadataClass)
	{
		this.format = new SimpleDateFormat(timestampFormat);
		this.messageEndIndicator = messageEndIndicator;
		this.metadataClass = metadataClass;
	}
	
	public MessageFileReader()
	{
		this(DEFAULT_TIMESTAMP_FORMAT, DEFAULT_MESSAGE_END_INDICATOR, ClearThMessageMetadata.class);
	}
	
	
	public void processMessagesFromFile(Path file, Consumer<EncodedClearThMessage> messageConsumer) throws IOException
	{
		StringBuilder sb;
		ClearThMessageMetadata metadata = null;
		int lineIndex = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile())))
		{
			sb = new StringBuilder();
			while (reader.ready())
			{
				String s = reader.readLine();
				lineIndex++;
				
				if (metadata == null)
				{
					metadata = parseMetadata(s, lineIndex);
					if (metadata != null)
						continue;
				}
				
				boolean endMessage;
				if (s != null && !s.trim().isEmpty())
				{
					endMessage = messageEndIndicator != null ? s.startsWith(messageEndIndicator) : false;
					if (sb.length() > 0)
						sb.append(Utils.EOL);
					sb.append(s);
				}
				else
					endMessage = true;
				
				if (endMessage)
				{
					if (sb.length() > 0)
					{
						processMessage(sb.toString(), metadata, messageConsumer);
						sb = new StringBuilder();
						metadata = null;
					}
				}
			}
		}
		
		if (sb.length() > 0)
			processMessage(sb.toString(), metadata, messageConsumer);
	}
	
	
	protected ClearThMessageMetadata parseMetadata(String str, int lineIndex) throws IOException
	{
		if (str.startsWith("{"))  //Supposed to be JSON
		{
			try
			{
				return parseMetadataFromJson(str, lineIndex);
			}
			catch (Exception e)
			{
				throw new IOException("Could not parse metadata, line #"+lineIndex, e); 
			}
		}
		
		Instant timestamp = parseTimestamp(str, lineIndex);  //For legacy files
		return timestamp == null ? null : new ClearThMessageMetadata(null, timestamp, null);
	}
	
	protected final ClearThMessageMetadata parseMetadataFromJson(String str, int lineIndex) throws IOException
	{
		return JSON_READER.readValue(str, metadataClass);
	}
	
	protected final Instant parseTimestamp(String str, int lineIndex)
	{
		ParsePosition position = new ParsePosition(0);
		Date date = format.parse(str, position);
		if (date == null || position.getIndex() < str.length())
		{
			logger.trace("Could not parse as timestamp the value '{}'", str);
			return null;
		}
		return date.toInstant();
	}
	
	protected final void processMessage(String msg, ClearThMessageMetadata metadata, Consumer<EncodedClearThMessage> messageConsumer)
	{
		EncodedClearThMessage message = new EncodedClearThMessage(msg, metadata);
		messageConsumer.accept(message);
	}
}
