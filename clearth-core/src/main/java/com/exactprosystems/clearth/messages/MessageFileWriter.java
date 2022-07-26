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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.nio.file.Path;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class MessageFileWriter implements AutoCloseable, Flushable
{
	private static final ObjectWriter JSON_WRITER = MessageFileReader.JSON_MAPPER
			.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
			.writer();
	private final BufferedWriter writer;
	private final String messageEnd;
	
	public MessageFileWriter(Path file, boolean append, String messageEnd) throws IOException
	{
		writer = new BufferedWriter(new FileWriter(file.toFile(), append));
		this.messageEnd = messageEnd;
	}
	
	public MessageFileWriter(Path file, boolean append) throws IOException
	{
		this(file, append, MessageFileReader.DEFAULT_MESSAGE_END_INDICATOR);
	}
	
	@Override
	public void close() throws Exception
	{
		writer.close();
	}
	
	@Override
	public void flush() throws IOException
	{
		writer.flush();
	}
	
	
	public void writeMessage(EncodedClearThMessage message) throws IOException
	{
		ClearThMessageMetadata metadata = message.getMetadata();
		if (metadata != null)
		{
			writer.write(JSON_WRITER.writeValueAsString(metadata));
			writer.newLine();
		}
		writer.write(message.getPayload().toString());
		writer.write(messageEnd);
	}
}
