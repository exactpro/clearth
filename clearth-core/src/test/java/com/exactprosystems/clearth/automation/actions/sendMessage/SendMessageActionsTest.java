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

package com.exactprosystems.clearth.automation.actions.sendMessage;

import static com.exactprosystems.clearth.ApplicationManager.ADMIN;
import static com.exactprosystems.clearth.ApplicationManager.USER_DIR;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactprosystems.clearth.ApplicationManager;
import com.exactprosystems.clearth.automation.TestActionUtils;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.utils.ClearThException;

public class SendMessageActionsTest
{
	private static final Path TEST_DATA = USER_DIR.resolve("src/test/resources/Action/sendMessage"),
			ACTIONS_PATH = TEST_DATA.resolve("actionsmapping.cfg"),
			MATRICES_DIR = TEST_DATA.resolve("matrices"),
			SEND_MATRICES_DIR = MATRICES_DIR.resolve("SendMessageAction"),
			SEND_PLAIN_MATRICES_DIR = MATRICES_DIR.resolve("SendPlainMessage"),
			CONFIG = TEST_DATA.resolve("configs").resolve("config.cfg");
	
	private ApplicationManager clearThManager;
	private CollectingSender sender;
	private Set<String> customActions;
	
	@BeforeClass
	public void init() throws ClearThException
	{
		clearThManager = new ApplicationManager();
		sender = new CollectingSender();
		customActions = TestActionUtils.addCustomActions(ACTIONS_PATH).keySet();
		CustomSendMessageAction.setSender(sender);
		CustomSendPlainMessage.setSender(sender);
	}
	
	@AfterClass
	public void dispose() throws IOException
	{
		CustomSendPlainMessage.setSender(null);
		CustomSendMessageAction.setSender(null);
		
		TestActionUtils.removeCustomActions(customActions);
		if (clearThManager != null)
			clearThManager.dispose();
	}
	
	@AfterMethod
	public void reset()
	{
		sender.getSentMessages().clear();
		TestActionUtils.resetUserSchedulers(ADMIN);
	}
	
	
	@Test(description = "Checks that SendMessageAction uses MetaFields parameter and puts corresponding action parameters into metadata of message being sent")
	public void sendMessageActionUsesMetadata() throws ClearThException, IOException, AutomationException
	{
		checkMessageSending(SEND_MATRICES_DIR);
	}
	
	@Test(description = "Checks that SendPlainMessage uses MetaFields parameter and puts corresponding action parameters into metadata of message being sent")
	public void sendPlainMessageUsesMetadata() throws ClearThException, IOException, AutomationException
	{
		checkMessageSending(SEND_PLAIN_MATRICES_DIR);
	}
	
	
	private void checkMessageSending(Path matricesDir) throws ClearThException, IOException, AutomationException
	{
		TestActionUtils.runScheduler(clearThManager, ADMIN, ADMIN, CONFIG, matricesDir, 5000);
		
		
		Object message1 = sender.getSentMessages().get(0);
		Assert.assertTrue(message1 instanceof EncodedClearThMessage, "sent message is complex object");
		
		EncodedClearThMessage encoded = (EncodedClearThMessage)message1;
		ClearThMessageMetadata metadata = encoded.getMetadata();
		Assert.assertNotNull(metadata, "sent message has metadata");
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(metadata.getField("MetaField1"), "MetaValue1");
		soft.assertEquals(metadata.getField("MetaField2"), "MetaValue2");
		soft.assertAll();
		
		
		Object message2 = sender.getSentMessages().get(1);
		Assert.assertTrue(message2 instanceof String, "sent message is plain text");
	}
}
