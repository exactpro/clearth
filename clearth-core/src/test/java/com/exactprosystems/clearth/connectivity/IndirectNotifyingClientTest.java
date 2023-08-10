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

import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.data.DummyHandlersFactory;
import com.exactprosystems.clearth.data.DummyMessageHandler;
import com.exactprosystems.clearth.utils.Utils;

public class IndirectNotifyingClientTest
{
	@Test(description = "Basic client returns the same message that was used by client object in send listeners notification")
	public void indirectNotificationMessage() throws Exception
	{
		DummyHandlersFactory handlersFactory = new DummyHandlersFactory();
		IndirectNotifyingConnection con = new IndirectNotifyingConnection();
		try
		{
			con.setName("TestCon");
			con.setDataHandlersFactory(handlersFactory);
			con.start();
			
			EncodedClearThMessage outcome = con.sendMessage("test");
			DummyMessageHandler handler = handlersFactory.getMessageHandler(con.getName());
			EncodedClearThMessage sent = handler.pollSentMessage(3000, TimeUnit.MILLISECONDS);
			Assert.assertEquals(sent, outcome);
		}
		finally
		{
			con.stop();
			Utils.closeResource(handlersFactory);
		}
	}
}
