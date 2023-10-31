/*******************************************************************************
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

package com.exactprosystems.clearth.connectivity.dummy;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.ListenerType;
import com.exactprosystems.clearth.connectivity.MessageListener;
import com.exactprosystems.clearth.connectivity.connections.BasicClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.connections.clients.BasicClearThClient;
import com.exactprosystems.clearth.connectivity.connections.storage.DefaultClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.listeners.DummyListener;
import com.exactprosystems.clearth.connectivity.listeners.FileListener;
import com.exactprosystems.clearth.data.DefaultDataHandlersFactory;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/** Purpose of this class and it's Dummies is to test together abstract classes
 *  {@link ClearThMessageConnection},
 *  {@link BasicClearThClient}, 
 *  {@link BasicClearThMessageConnection}
 *  */
public class DummyConnectionTest
{
	private static ApplicationManager applicationManager;
	private static DefaultClearThConnectionStorage storage;
	private static Path testingDirectory;
	private static ConnectionTypeInfo dummyTypeInfo;
	
	@BeforeClass
	public static void beforeClass() throws ClearThException, SettingsException
	{
		applicationManager = new ApplicationManager();
		testingDirectory = Paths.get("testOutput")
				.resolve(DummyConnectionTest.class.getSimpleName());
		prepareDirectory(testingDirectory);
		storage = new DefaultClearThConnectionStorage(new DefaultDataHandlersFactory());
		dummyTypeInfo = createDummyTypeInfo();
		storage.registerType(dummyTypeInfo);
	}
	
	@AfterClass
	public static void afterClass()throws IOException
	{
		if (applicationManager != null)
			applicationManager.dispose();
	}

	private static void prepareDirectory(Path connectionDir)
	{
		connectionDir.toFile().mkdirs();
		try
		{
			FileUtils.cleanDirectory(connectionDir.toFile());
		}
		catch (IOException e)
		{
			throw new RuntimeException("Error occurred during preparing directory for tests: " + connectionDir.toAbsolutePath(), e);
		}
	}

	private static ConnectionTypeInfo createDummyTypeInfo()
	{
		return dummyTypeInfo = new ConnectionTypeInfo(
				DummyMessageConnection.TYPE,
				DummyMessageConnection.class,
				testingDirectory);
	}

	private DummyMessageConnection createDummyCon() throws ConnectivityException
	{
		return (DummyMessageConnection) storage.createConnection(dummyTypeInfo.getName());
	}
	
	@Test
	public void testUnhandledMessagesWritingReading()
			throws ConnectivityException, SettingsException, InterruptedException
	{
		String message = "Message from testUnhandledMessagesWritingReading";
		DummyMessageConnection con = createDummyCon();
		con.setName("testUnhandledMessagesWritingReading");
		con.start();
		try
		{
			con.getClient().putDirectlyToReceivedMessages(message);
			con.restart();
			assertEquals(message, con.pollFirstFromReceivedMessages());
		}
		finally
		{
			con.stop();
		}
	}
	
	@Test
	public void testSendListener() throws ConnectivityException, SettingsException, IOException
	{
		String conName = "testSendListener";
		String message = conName + " - message";
		
		List<String> lines = testConWithListener(conName, message, false, true);
		
		//first line is a metadata; second one is a message; third one is empty
		assertEquals(3, lines.size());
		assertEquals(message, lines.get(1));
		assertEquals("", lines.get(2));
	}
	
	@Test
	public void testReceiveListener()
			throws ConnectivityException, SettingsException, InterruptedException, IOException
	{
		String conName = "testReceiveListener";
		String message = conName + " - message";

		List<String> lines = testConWithListener(conName, message, true, false);

		//1st line is metadata, 2nd line is message itself, 3rd line is empty
		assertEquals(3, lines.size());
		assertEquals(message, lines.get(1));
		assertEquals("", lines.get(2));
	}

	@Test
	public void testSendAndReceiveListener() throws IOException, ConnectivityException, SettingsException
	{
		String conName = "testBothDirectionsListener";
		String message = conName + " - message";

		List<String> lines = testConWithListener(conName, message, true, true);

		//3 lines from receive listener, 3 from send listener; they can have different order
		//1st and 4th line will always be the message
		assertEquals(6, lines.size());
		assertEquals(message, lines.get(1));
		assertEquals(message, lines.get(4));
	}

	private List<String> testConWithListener(String conName, String messageToSend, boolean activeForReceived, boolean activeForSent)
			throws ConnectivityException, SettingsException, IOException
	{
		String listenerName = conName + "_listener";
		Path listenerPath = testingDirectory.resolve(listenerName + ".txt");
		DummyMessageConnection connection = createDummyCon();
		connection.setName(conName);
		connection.setNeedReceiverProcessorThread(true);

		connection.addListener(createFileListenerConfig(listenerPath, listenerName, activeForReceived,activeForSent));

		connection.start();
		connection.sendMessage(messageToSend);
		if (activeForSent)
			ensureHasMessagesProcessed(() -> connection.getSent());
		if (activeForReceived)
			ensureHasMessagesProcessed(() -> connection.getReceived());
		connection.stop();
		
		return getFileContent(listenerPath.toAbsolutePath().toFile());
	}
	
	private void ensureHasMessagesProcessed(Supplier<Long> getter) throws ConnectivityException
	{
		LocalDateTime giveUpTime = LocalDateTime.now().plusSeconds(1);
		while (LocalDateTime.now().isBefore(giveUpTime))
		{
			if (getter.get() > 0)
				return;

			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e)
			{
				throw new ConnectivityException(e);
			}
		}
		throw new ConnectivityException("Connection couldn't process messages");
	}
	
	private ListenerConfiguration createFileListenerConfig(Path listenerPath, String listenerName,
	                                                         boolean activeForReceived, boolean activeForSent)
	{
		ListenerConfiguration config = new ListenerConfiguration();
		config.setName(listenerName);
		config.setType(ListenerType.File.getLabel());
		config.setActive(activeForReceived);
		config.setActiveForSent(activeForSent);
		config.setSettings(listenerPath.toAbsolutePath().toString());

		return config;
	}
	
	protected List<String> getFileContent(File file) throws IOException
	{
		return new BufferedReader(new FileReader(file)).lines().collect(Collectors.toList());
	}
	
	@Test
	public void testSendMessage() throws ConnectivityException, SettingsException
	{
		String message = "Message to send";
		DummyMessageConnection con = createDummyCon();
		con.setName("testSendMessage");
		assertThrows(ConnectivityException.class, () -> con.sendMessage(message));
		con.start();
		con.sendMessage(message);
		assertEquals(message, con.getLastSentMessage());
	}

	@Test
	public void testOrder() throws IOException, ConnectivityException, SettingsException, InterruptedException
	{
		String line1 = "Line #1";
		String line2 = "Line #2";
		String line3 = "Line #3";
		DummyMessageConnection con = createDummyCon();
		con.setName("testOrder");
		con.start();
		try
		{
			con.sendMessage(line3);
			con.sendMessage(line1);
			con.sendMessage(line2);

			assertEquals(line3, con.pollFirstFromReceivedMessages());
			assertEquals(line1, con.pollFirstFromReceivedMessages());
			assertEquals(line2, con.pollFirstFromReceivedMessages());
		}
		finally
		{
			con.stop();
		}
	}

	@Test
	public void testCustomisingFactory()
	{
		DummyMessageConnection connection = new DummyMessageConnection();
		Class<?> listener = connection.getListenerClass("Dummy");
		assertEquals(DummyListener.class, listener);

		Map<String, Class<? extends MessageListener>> listenerTypeMap = connection.getSupportedListenerTypes();
		assertEquals(createListenerTypeMap(), listenerTypeMap);
	}

	private Map<String, Class<? extends MessageListener>> createListenerTypeMap()
	{
		Map<String, Class<? extends MessageListener>> map = new LinkedHashMap<>();
		map.put("File", FileListener.class);
		map.put("Dummy", DummyListener.class);
		return map;
	}
}