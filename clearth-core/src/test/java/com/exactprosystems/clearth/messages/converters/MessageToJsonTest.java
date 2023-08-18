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

package com.exactprosystems.clearth.messages.converters;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;

public class MessageToJsonTest
{
	private MessageToJson converter = new MessageToJson();
	
	@BeforeClass
	public void init()
	{
		converter = new MessageToJson();
	}
	
	@Test
	public void convertToJson() throws ConversionException
	{
		String subListType = "simpleList";
		SimpleClearThMessageBuilder builder = new SimpleClearThMessageBuilder();
		
		SimpleClearThMessage elem1 = builder.subMessageType(subListType)
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("Field1", "Value1")
				.build();
		SimpleClearThMessage elem2 = builder.subMessageType(subListType)
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("Field1", "Value1")
				.field("Field2", "Value2")
				.build();
		
		SimpleClearThMessage simpleMap = builder.subMessageType("simpleMap")
				.field("MapField1", "123")
				.field("MapField2", "234")
				.build();
		
		SimpleClearThMessage message = builder.type("Message1")
				.field("PlainField1", "PlainValue12")
				.rg(simpleMap)
				.rg(elem1)
				.rg(elem2)
				.build();
		
		String json = converter.convert(message);
		Assert.assertEquals(json,
				"{\"PlainField1\":\"PlainValue12\","
						+ "\"simpleMap\":{"
								+ "\"MapField1\":\"123\",\"MapField2\":\"234\""
								+ "},"
						+ "\"simpleList\":["
								+ "{\"Field1\":\"Value1\"},"
								+ "{\"Field1\":\"Value1\",\"Field2\":\"Value2\"}"
								+ "]"
						+ "}",
				"Message body");
	}
}
