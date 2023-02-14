/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.ListenerType;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.dummy.DummyClient;
import com.exactprosystems.clearth.connectivity.dummy.DummyMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;

public class MessageHandlerTest
{
	private final Path storage = Paths.get("testOutput").resolve("MessageHandlerTest").resolve("connections");
	
	@BeforeClass
	public void init() throws IOException
	{
		Files.createDirectories(storage);
	}
	
	@BeforeMethod
	public void prepare() throws IOException
	{
		FileUtils.cleanDirectory(storage.toFile());  //Removing files with unhandled messages if they are present after previous test
	}
	
	@Test
	public void sendMessage() throws Exception
	{
		DummyMessageConnection con = createConnection();
		con.start();
		
		try
		{
			String message = "testmessage";
			
			EncodedClearThMessage sentMessage = con.sendMessage(message);
			Assert.assertEquals(sentMessage.getPayload(), message, "Payload of message returned as sent");
			
			HandledMessageId id = MessageHandlingUtils.getMessageId(sentMessage.getMetadata());
			Assert.assertNotNull(id, "Sent message ID");
			
			DummyMessageHandler handler = (DummyMessageHandler)con.getClient().getMessageHandler();
			EncodedClearThMessage handledMessage = handler.pollSentMessage(2, TimeUnit.SECONDS);
			Assert.assertNotNull(handledMessage, "Handled message");
			Assert.assertEquals(handledMessage.getPayload(), message, "Payload of handled message");
			Assert.assertEquals(MessageHandlingUtils.getMessageId(handledMessage.getMetadata()), id, "Handled message ID");
		}
		finally
		{
			con.stop();
		}
	}
	
	@Test
	public void receiveMessage() throws Exception
	{
		DummyMessageConnection con = createConnection();
		con.addListener(new ListenerConfiguration("Collector", ListenerType.Collector.getLabel(), "", true, false));
		con.setNeedReceiverProcessorThread(true);
		con.start();
		
		try
		{
			String message = "testmessage";
			
			DummyClient client = con.getClient();
			client.putDirectlyToReceivedMessages(message);
			
			DummyMessageHandler handler = (DummyMessageHandler)client.getMessageHandler();
			EncodedClearThMessage handledMessage = handler.pollReceivedMessage(2, TimeUnit.SECONDS);
			Assert.assertNotNull(handledMessage, "Handled message");
			Assert.assertEquals(handledMessage.getPayload(), message, "Payload of handled message");
			HandledMessageId id = MessageHandlingUtils.getMessageId(handledMessage.getMetadata());
			Assert.assertNotNull(id, "Handled message ID");
			
			ClearThMessage<?> collectedMessage = getCollectedMessage(con);
			Assert.assertNotNull(collectedMessage, "Collected message");
			Assert.assertEquals(collectedMessage.getEncodedMessage(), message, "Payload of collected message");
			Assert.assertEquals(MessageHandlingUtils.getMessageId(collectedMessage.getMetadata()), id, "Collected message ID");
		}
		finally
		{
			con.stop();
		}
	}
	
	@Test
	public void inactiveHandler() throws Exception
	{
		DummyMessageConnection con = createConnection();
		con.start();
		
		try
		{
			DummyClient client = con.getClient();
			
			DummyMessageHandler handler = (DummyMessageHandler)client.getMessageHandler();
			handler.setActive(false);
			
			con.sendMessage("testmessage");
			EncodedClearThMessage sent = handler.pollSentMessage(1, TimeUnit.SECONDS);
			Assert.assertNull(sent, "Handled sent message");
			
			client.putDirectlyToReceivedMessages("anothermessage");
			EncodedClearThMessage received = handler.pollReceivedMessage(1, TimeUnit.SECONDS);
			Assert.assertNull(received, "Handled received message");
		}
		finally
		{
			con.stop();
		}
	}
	
	
	private DummyMessageConnection createConnection()
	{
		DummyMessageConnection con = new DummyMessageConnection();
		con.setName("Con1");
		con.setTypeInfo(new ConnectionTypeInfo("Dummy", DummyMessageConnection.class, storage));
		con.setDataHandlersFactory(new DummyHandlersFactory());
		return con;
	}
	
	private ClearThMessage<?> getCollectedMessage(ClearThMessageConnection con) throws InterruptedException
	{
		ClearThMessageCollector collector = (ClearThMessageCollector) con.findListener(ListenerType.Collector.getLabel());
		
		long endTime = System.currentTimeMillis()+2000;
		do
		{
			Collection<ClearThMessage<?>> collected = collector.getMessages();
			if (collected.size() > 0)
				return collected.iterator().next();
			
			if (System.currentTimeMillis() < endTime)
				Thread.sleep(1);
			else
				return null;
		}
		while (true);
	}
}
