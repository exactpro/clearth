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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.Assertion;
import org.testng.asserts.SoftAssert;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.HandlingApplicationManager;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.TestActionUtils;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.ListenerConfiguration;
import com.exactprosystems.clearth.connectivity.ListenerType;
import com.exactprosystems.clearth.connectivity.connections.ConnectionTypeInfo;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.connectivity.dummy.DummyMessageConnection;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.DummyTestExecutionHandler.StepExecutionInfo;
import com.exactprosystems.clearth.utils.ClearThException;
import com.exactprosystems.clearth.utils.SettingsException;

public class TestExecutionHandlerTest
{
	private final Path filesRoot = ApplicationManager.USER_DIR.resolve("src").resolve("test").resolve("resources").resolve("TestExecutionHandler"),
			configsDir = filesRoot.resolve("configs"),
			configFile = configsDir.resolve("config.cfg"),
			outputDir = Paths.get("testOutput").resolve("TestExecutionHandlerTest");
	private DummyHandlersFactory dataHandlersFactory;
	private ApplicationManager appManager;
	
	@BeforeClass
	public void init() throws ClearThException
	{
		dataHandlersFactory = new DummyHandlersFactory();
		appManager = new HandlingApplicationManager(dataHandlersFactory);
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		if (appManager != null)
			appManager.dispose();
	}
	
	@Test
	public void testExecution() throws Exception
	{
		dataHandlersFactory.setCreateActiveHandlers(true);
		
		DummyMessageConnection con = createConnection();
		con.start();
		
		Scheduler scheduler = TestActionUtils.runScheduler(appManager, "unittest", "scheduler1", configFile, filesRoot.resolve("matrices"), 5000);
		String step1 = "Step1",
				step2 = "Step2",
				staticMatrix = "static.csv",
				failedMatrix = "failed.csv",
				messagesMatrix = "messages.csv";
		
		DummyTestExecutionHandler handler = dataHandlersFactory.getTestExecutionHandler(scheduler.getName());
		
		SoftAssert soft = new SoftAssert();
		soft.assertTrue(handler.isTestStarted(), "Test started");
		soft.assertTrue(handler.isTestEnded(), "Test ended");
		
		assertStep(step1, handler, soft);
		assertStep(step2, handler, soft);
		
		assertAction(staticMatrix, step2, "id1", true, null, handler, soft);
		assertAction(failedMatrix, step1, "id1", false, null, handler, soft);
		assertAction(failedMatrix, step2, "id2", true, null, handler, soft);
		
		DummyMessageHandler messageHandler = (DummyMessageHandler) con.getClient().getMessageHandler();
		EncodedClearThMessage sentMessage = messageHandler.pollSentMessage(1, TimeUnit.SECONDS),
				receivedMessage = messageHandler.pollReceivedMessage(1, TimeUnit.SECONDS);
		soft.assertNotNull(sentMessage, "Sent message");
		soft.assertNotNull(receivedMessage, "Received message");
		
		assertAction(messagesMatrix, step1, "id1", true, Collections.singletonList(sentMessage), handler, soft);
		assertAction(messagesMatrix, step2, "id2", true, Collections.singletonList(receivedMessage), handler, soft);
		
		soft.assertAll();
	}
	
	@Test
	public void inactiveHandler() throws Exception
	{
		dataHandlersFactory.setCreateActiveHandlers(false);
		
		Scheduler scheduler = TestActionUtils.runScheduler(appManager, "unittest", "scheduler1", configFile, filesRoot.resolve("simplematrix"), 2000);
		
		DummyTestExecutionHandler handler = dataHandlersFactory.getTestExecutionHandler(scheduler.getName());
		
		SoftAssert soft = new SoftAssert();
		soft.assertFalse(handler.isTestStarted(), "Test started");
		soft.assertFalse(handler.isTestEnded(), "Test ended");
		soft.assertNull(handler.getStepInfo("Step1"), "Step1 info");
		soft.assertNull(handler.getStepInfo("Step2"), "Step2 info");
		soft.assertNull(handler.getActionResult("simple.csv", "Step1", "id1"), "Action result");
		soft.assertAll();
	}
	
	
	private DummyMessageConnection createConnection() throws ConnectivityException, SettingsException
	{
		ClearThConnectionStorage conStorage = ClearThCore.connectionStorage();
		conStorage.registerType(new ConnectionTypeInfo("Dummy", DummyMessageConnection.class, outputDir.resolve("connections")));
		
		DummyMessageConnection con = (DummyMessageConnection)conStorage.createConnection("Dummy");
		con.setName("Con1");
		con.addListener(new ListenerConfiguration("PlainCollector", ListenerType.Collector.getLabel(), "", true, false));
		con.setNeedReceiverProcessorThread(true);
		conStorage.addConnection(con);
		return con;
	}
	
	private void assertStep(String name, DummyTestExecutionHandler handler, Assertion assertion)
	{
		StepExecutionInfo stepInfo = handler.getStepInfo(name);
		assertion.assertNotNull(stepInfo);
		if (stepInfo != null)
		{
			assertion.assertTrue(stepInfo.isStarted(), "Step '"+name+"' started");
			assertion.assertTrue(stepInfo.isEnded(), "Step '"+name+"' ended");
		}
	}
	
	private void assertAction(String matrix, String step, String id, 
			boolean success, Collection<EncodedClearThMessage> messages,
			DummyTestExecutionHandler handler, Assertion assertion)
	{
		String actionPath = String.format("%s > %s > %s", matrix, step, id);
		
		Result result = handler.getActionResult(matrix, step, id);
		assertion.assertNotNull(result, "Result of action "+actionPath);
		if (result == null)
			return;
		
		assertion.assertEquals(result.isSuccess(), success, "Success of action "+actionPath);
		
		if (CollectionUtils.isEmpty(messages))
			return;
		
		Collection<EncodedClearThMessage> resultMessages = result.getLinkedMessages();
		assertion.assertNotNull(resultMessages, "Linked messages of action "+actionPath);
		if (resultMessages == null)
			return;
		
		Collection<HandledMessageId> expectedMessageIds = getMessageIds(messages),
				actualMessageIds = getMessageIds(resultMessages);
		//Handled (expected) and verified by action (actual) messages can be different, 
		//because ClearThMessageCollector modifies received message before action verifies it.
		//But IDs of messages must be kept the same
		assertion.assertEquals(actualMessageIds, expectedMessageIds, "IDs of linked messages of action "+actionPath);
	}
	
	private Collection<HandledMessageId> getMessageIds(Collection<EncodedClearThMessage> messages)
	{
		return messages.stream()
				.map(m -> MessageHandlingUtils.getMessageId(m.getMetadata()))
				.collect(Collectors.toList());
	}
}
