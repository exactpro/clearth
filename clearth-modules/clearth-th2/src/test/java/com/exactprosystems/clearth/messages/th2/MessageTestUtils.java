/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.messages.th2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.apache.commons.io.FileUtils;
import org.testng.Assert;

import com.exactpro.th2.common.grpc.Direction;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.converters.MessageToMap;

public class MessageTestUtils
{
	public static SimpleClearThMessage createComplexMessage()
	{
		SimpleClearThMessageBuilder builder = new SimpleClearThMessageBuilder();
		
		SimpleClearThMessage listItem = builder.subMessageType("simpleList")
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("Field10", "Value20")
				.field("Field20", "Value30")
				.build();
		
		SimpleClearThMessage simpleMap = builder.subMessageType("simpleMap")
				.field("MapField1", "123")
				.field("MapField2", "234")
				.build();
		
		return builder.type("Message1")
				.field("PlainField1", "PlainValue12")
				.field("PlainField2", "PlainValue3")
				.rg(listItem)
				.rg(simpleMap)
				.build();
	}
	
	public static MessageProperties createMessageProperties()
	{
		MessageProperties result = new MessageProperties();
		result.setBook("test_book");
		result.setDirection(Direction.SECOND);
		result.setMsgType("TestMsg");
		result.setSessionAlias("Session1");
		result.setSessionGroup("SessionGroup1");
		result.setTimestamp(LocalDateTime.of(2024, 2, 2, 12, 0, 0).toInstant(ZoneOffset.UTC));
		return result;
	}
	
	public static void assertMessages(Object actualMessage, Path pathToExpectedMessage) throws IOException
	{
		String expectedContent = FileUtils.readFileToString(pathToExpectedMessage.toFile(), StandardCharsets.UTF_8);
		Assert.assertEquals(prepareMessageContent(actualMessage.toString()), 
				prepareMessageContent(expectedContent));
	}
	
	
	private static String prepareMessageContent(String content)
	{
		return content.replace("\r\n", "\n");
	}
}
