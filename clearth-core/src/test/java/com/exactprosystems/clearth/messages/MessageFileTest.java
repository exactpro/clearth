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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessageDirection;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;

public class MessageFileTest
{
	@Test
	public void messageIsReadAsWritten() throws IOException
	{
		EncodedClearThMessage sentMessage = new EncodedClearThMessage("test_message", new ClearThMessageMetadata(ClearThMessageDirection.SENT, Instant.now(), null));
		sentMessage.getMetadata().addField("MetaField1", "MetaValue1");
		sentMessage.getMetadata().addField("MetaField2", "MetaValue2");
		
		Path file = Paths.get("testOutput").resolve("messages").resolve("encoded.txt");
		Files.createDirectories(file.getParent());
		try (MessageFileWriter writer = new MessageFileWriter(file, false))
		{
			writer.write(sentMessage);
		}
		
		new MessageFileReader().processMessages(file, m -> compareMessages(m, sentMessage));
	}
	
	private void compareMessages(EncodedClearThMessage actual, EncodedClearThMessage expected)
	{
		ClearThMessageMetadata actualMetadata = actual.getMetadata(),
				expectedMetadata = expected.getMetadata();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(actual.getPayload(), expected.getPayload());
		
		soft.assertEquals(actualMetadata.getDirection(), expectedMetadata.getDirection());
		soft.assertEquals(actualMetadata.getTimestamp(), expectedMetadata.getTimestamp());
		soft.assertEquals(actualMetadata.getFields(), expectedMetadata.getFields());
		soft.assertAll();
	}
}
