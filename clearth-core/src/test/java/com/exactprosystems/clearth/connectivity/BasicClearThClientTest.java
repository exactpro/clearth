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

package com.exactprosystems.clearth.connectivity;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;
import com.exactprosystems.clearth.data.DefaultDataHandlersFactory;
import com.exactprosystems.clearth.messages.ConnectionFinder;
import com.exactprosystems.clearth.messages.MessageFileReader;
import com.exactprosystems.clearth.utils.ClearThException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;

public class BasicClearThClientTest
{
	private static final String RECEIVED_1 = "received1",
			RECEIVED_2 = "received2",
			SENT_1 = "sent1",
			SENT_2 = "sent2";
	private static final Path OUTPUT_ROOT = Paths.get("testOutput")
			.resolve(BasicClearThClientTest.class.getSimpleName()).toAbsolutePath();
	
	private ApplicationManager clearThManager;
	private TestMessageConnection con;
	private BlockingQueue<String> source;
	private Collection<String> target;
	private List<String> expectedSent,
			expectedReceived,
			expectedDual;
	private Collection<Object> toSend;
	private Collection<String> toReceive;
	
	@BeforeClass
	public void init() throws ClearThException, IOException
	{
		clearThManager = new ApplicationManager();
		
		expectedSent = Arrays.asList(SENT_1, SENT_2);
		expectedReceived = Arrays.asList(RECEIVED_1, RECEIVED_2);
		
		expectedDual = new ArrayList<>(expectedSent);
		expectedDual.addAll(expectedReceived);
		Collections.sort(expectedDual);
		
		toSend = new ArrayList<>();
		toSend.add(SENT_1);
		toSend.add(EncodedClearThMessage.newSentMessage(SENT_2));
		
		toReceive = Arrays.asList(RECEIVED_1, RECEIVED_2);
		
		Files.createDirectories(OUTPUT_ROOT);
		clearDirectory(OUTPUT_ROOT);
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (clearThManager != null)
			clearThManager.dispose();
	}
	
	@BeforeMethod
	public void prepareConnection() throws IOException
	{
		con = new TestMessageConnection();
		con.setTypeInfo(new ConnectionTypeInfo("TestTypeInfo",
				TestMessageConnection.class,
				OUTPUT_ROOT));
		
		con.setName("TestCon");
		con.setDataHandlersFactory(new DefaultDataHandlersFactory());
		TestConnectionSettings settings = (TestConnectionSettings) con.getSettings();
		source = settings.getSource();
		target = settings.getTarget();
	}
	
	@Test(description = "Client behavior when no listeners are added")
	public void noListeners() throws Exception
	{
		con.start();
		
		sendReceive(toSend, toReceive, 2000);
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(target, expectedSent, "sent messages");
		soft.assertEquals(con.getSent(), toSend.size(), "number of sent messages");
		soft.assertEquals(con.getReceived(), toReceive.size(), "number of received messages");
		soft.assertAll();
	}
	
	@Test(description = "Client behavior when it has listeners. They should get only messages they were intended to get")
	public void withListeners() throws Exception
	{
		Path receivedFile = OUTPUT_ROOT.resolve("withListeners_received.txt"),
				sentFile = OUTPUT_ROOT.resolve("withListeners_sent.txt"),
				dualFile = OUTPUT_ROOT.resolve("withListeners_dual.txt");
		
		// This file shouldn't be created because listener is switched off
		Path noFile = OUTPUT_ROOT.resolve("withListeners_noFile.txt");  
		
		String type = ListenerType.File.getLabel();
		con.addListener(new ListenerConfiguration("Received messages", type, 
				receivedFile.toString(), 
				true, false));
		con.addListener(new ListenerConfiguration("Sent messages", type, 
				sentFile.toString(), 
				false, true));
		con.addListener(new ListenerConfiguration("Dual file", type, 
				dualFile.toString(), 
				true, true));
		con.addListener(new ListenerConfiguration("No file", type, 
				noFile.toString(), 
				false, false));
		
		con.start();
		sendReceive(toSend, toReceive, 2000);
		con.stop();
		
		MessageFileReader reader = new MessageFileReader();
		List<String> received = readFile(reader, receivedFile),
				sent = readFile(reader, sentFile),
				dual = readFile(reader, dualFile),
				no = readFile(reader, noFile);
		
		Collections.sort(dual);  //Sent and received messages can be mixed, but their order here doesn't matter
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(received, expectedReceived, "received messages");
		soft.assertEquals(sent, expectedSent, "sent messages");
		soft.assertEquals(dual, expectedDual, "all messages");
		soft.assertEquals(no, Collections.EMPTY_LIST, "no messages");
		soft.assertAll();
	}
	
	@Test(description = "Initialization from file with unhandled messages that should be processed as received ones")
	public void unhandledMessages() throws Exception
	{
		String fileName = "unhandledMessagesTest.txt";
		TestConnectionSettings settings = (TestConnectionSettings) con.getSettings();
		settings.setProcessReceived(false);
		con.start();  //Received messages won't be handled
		
		for (String s : toReceive)
			source.add(s);
		waitEmpty(source, 2000);
		
		con.stop();  //BasicClearThClient should save unhandled messages
		
		settings.setProcessReceived(true);
		
		Path receivedFile = OUTPUT_ROOT.resolve(fileName);
		con.addListener(new ListenerConfiguration("Received messages", ListenerType.File.getLabel(), receivedFile.toString(), true, false));
		con.start();  //Unhandled messages should be loaded from file and passed to received processor thread and thus to FileListener
		waitEnd(0, toReceive.size(), 2000);
		
		con.stop();
		
		Collection<String> received = readFile(new MessageFileReader(), receivedFile);
		assertEquals(received, expectedReceived);
	}
	
	@Test(description = "Initialization of Collector from file written by FileListener")
	public void collectorFromFile() throws Exception
	{
		Path file = OUTPUT_ROOT.resolve("collectorFromFile.txt");
		ListenerConfiguration fileListenerConfig = new ListenerConfiguration("Received messages", ListenerType.File.getLabel(), file.toString(), true, true);
		con.addListener(fileListenerConfig);
		con.start();
		
		sendReceive(toSend, toReceive, 2000);  //FileListener will write all sent and received messages, but Collector should process only received ones
		
		con.stop();
		con.removeListener(fileListenerConfig);
		con.addListener(new ListenerConfiguration("Collector", ListenerType.Collector.getLabel(), "fileName="+file.toString(), true, false));
		con.start();
		
		ClearThMessageCollector collector = new ConnectionFinder().findCollector(con);
		Collection<String> messages = collector.getMessages().stream().map(m -> m.getField(ClearThMessageCollector.MESSAGE)).collect(Collectors.toList());
		assertEquals(messages, expectedReceived);
	}

	@Test
	public void checkCloseConnection() throws Exception
	{
		assertEquals(TestBasicClearThClient.value, 15);
		try
		{
			TestBasicClearThClient client = new TestBasicClearThClient(con);
		}
		catch (Exception e)
		{
			assertEquals(e.getMessage(), "value = 50");
		}
		assertEquals(TestBasicClearThClient.value, 30);
	}
	
	private void clearDirectory(Path directory) throws IOException
	{
		try (Stream<Path> files = Files.list(OUTPUT_ROOT))
		{
			Iterator<Path> it = files.iterator();
			while (it.hasNext())
			{
				Path f = it.next();
				if (Files.isRegularFile(f))
					Files.delete(f);
			}
		}
	}
	
	private void sendReceive(Collection<Object> toSend, Collection<String> toReceive, int maxWait) throws ConnectivityException, IOException, InterruptedException
	{
		for (Object m : toSend)
		{
			if (m instanceof EncodedClearThMessage)
				con.sendMessage((EncodedClearThMessage) m);
			else
				con.sendMessage(m);
		}
		
		for (String m : toReceive)
			source.add(m);
		
		waitEnd(toSend.size(), toReceive.size(), maxWait);
	}
	
	private void waitEnd(long sent, long received, long maxWait) throws InterruptedException
	{
		long endTime = System.currentTimeMillis()+maxWait;
		while ((con.getSent() < sent || con.getReceived() < received) && System.currentTimeMillis() < endTime)
			Thread.sleep(1);
	}
	
	private void waitEmpty(Collection<?> collection, int maxWait) throws InterruptedException
	{
		long endTime = System.currentTimeMillis()+maxWait;
		while (collection.size() > 0 && System.currentTimeMillis() < endTime)
			Thread.sleep(1);
	}
	
	private List<String> readFile(MessageFileReader reader, Path file) throws IOException
	{
		List<String> result = new ArrayList<>();
		if (Files.isRegularFile(file))
			reader.processMessages(file, m -> result.add(m.getPayload().toString()));
		return result;
	}
}
