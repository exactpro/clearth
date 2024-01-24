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
import java.util.Collections;
import java.util.Set;

import com.exactprosystems.clearth.utils.ComparisonUtils;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.exactpro.th2.common.grpc.MessageFilter;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.messages.RgKeyFieldNames;
import com.exactprosystems.clearth.messages.converters.ConversionException;
import com.exactprosystems.clearth.messages.converters.MessageToMap;

public class MessageFilterFactoryTest
{
	private MessageFilterFactory factory;
	
	@BeforeClass
	public void init()
	{
		factory = new MessageFilterFactory(new ComparisonUtils());
	}
	
	@Test
	public void buildFromSimpleMessage() throws ConversionException, IOException
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
		
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("PlainField1", "PlainValue12")
				.field("PlainField2", "PlainValue3")
				.rg(listItem)
				.rg(simpleMap)
				.build();
		
		Set<String> keyFields = Collections.singleton("PlainField1");
		RgKeyFieldNames rgKeyFields = new RgKeyFieldNames();
		rgKeyFields.addRgKeyField("simpleList", "Field10");
		rgKeyFields.addRgKeyField("simpleMap", "MapField2");
		
		MessageFilter filter = factory.createMessageFilter(message, keyFields, rgKeyFields);
		assertEquals(filter, "expectedFilter.txt");
	}
	
	@Test
	public void matrixFunctionsToFilter() throws ConversionException, IOException
	{
		SimpleClearThMessageBuilder builder = new SimpleClearThMessageBuilder();
		SimpleClearThMessage message = builder.subMessageType("simpleList")
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("Field1", "@{isGreaterThan(50)}")
				.field("Field2", "@{isLessThan(65)}")
				.field("Field3", "@{isNotEqualText(b)}")
				.field("Field4", "{pattern('[0-9].*')}")
				.field("Field5", "@{isEmpty}")
				.field("Field6", "@{isNotEmpty}")
				.field("Field7", "{asNumber('53.0')}")
				.build();
		
		Set<String> keyFields = Collections.emptySet();
		RgKeyFieldNames rgKeyFields = new RgKeyFieldNames();
		rgKeyFields.addRgKeyField("simpleList", "Field1");
		
		MessageFilter filter = factory.createMessageFilter(message, keyFields, rgKeyFields);
		assertEquals(filter, "matrixFunctionsToFilter.txt");
	}
	
	private void assertEquals(MessageFilter filter, String fileName) throws IOException
	{
		Path expectedContentFile = Path.of("src", "test", "resources", "MessageFilterFactory", fileName);
		String expectedContent = FileUtils.readFileToString(expectedContentFile.toFile(), StandardCharsets.UTF_8)
				.replace("\r\n", "\n");  //MessageFilter.toString() uses \n an all platforms
		Assert.assertEquals(filter.toString(), expectedContent);
	}
}
