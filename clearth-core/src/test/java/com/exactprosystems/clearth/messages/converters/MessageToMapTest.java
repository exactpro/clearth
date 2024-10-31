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

package com.exactprosystems.clearth.messages.converters;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;

public class MessageToMapTest
{
	private MessageToMap converter = new MessageToMap();
	
	@BeforeClass
	public void init()
	{
		converter = new MessageToMap();
	}
	
	@DataProvider(name = "reassignedFields")
	public Object[][] reassignedFields()
	{
		SimpleClearThMessageBuilder builder = new SimpleClearThMessageBuilder();
		
		//Map is default SubMsgKind
		SimpleClearThMessage mapMsg = builder.subMessageType("Field1")
				.field("A", "B")
				.build();
		
		SimpleClearThMessage listMsg = builder.subMessageType("Field1")
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("A", "B")
				.build();
		
		return new Object[][] {
			{
				mapMsg
			},
			{
				listMsg
			}
		};
	}
	
	
	@Test
	public void convertFromRegularMessage() throws ConversionException
	{
		String subListType = "subList";
		SimpleClearThMessageBuilder builder = new SimpleClearThMessageBuilder();
		
		SimpleClearThMessage elem1 = builder.subMessageType(subListType)
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("Field1", "Value1")
				.build();
		SimpleClearThMessage elem2 = builder.subMessageType(subListType)
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("Field1", "Value100")
				.field("Field2", "Value2")
				.build();
		SimpleClearThMessage mapWithList = builder.subMessageType("mapWithList")
				.rg(elem1)
				.rg(elem2)
				.build();
		
		SimpleClearThMessage simpleMap = builder.subMessageType("simpleMap")
				.field("MapField1", "123")
				.field("MapField2", "234")
				.build();
		
		SimpleClearThMessage message = builder.type("Message1")
				.field("PlainField1", "PlainValue12")
				.rg(simpleMap)
				.rg(mapWithList)
				.build();
		
		Map<String, Object> map = converter.convert(message);
		Assert.assertEquals(map.toString(),
				"{PlainField1=PlainValue12, simpleMap={MapField1=123, MapField2=234}, mapWithList={subList=[{Field1=Value1}, {Field1=Value100, Field2=Value2}]}}",
				"Message body");
	}
	
	@Test
	public void convertFromFlatMessage() throws ConversionException
	{
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("PlainField1", "PlainValue12")
				.field("simpleMap_MapField1", "123")
				.field("simpleMap_MapField2", "234")
				.field("mapWithList_subList_1_Field1", "Value1")
				.field("mapWithList_subList_2_Field1", "Value100")
				.field("mapWithList_subList_2_Field2", "Value2")
				.field("mapWithList_arrayList_1", "AV1")
				.field("mapWithList_arrayList_2", "AV2")
				.build();
		
		Map<String, Object> map = converter.convert(message);
		Assert.assertEquals(map.toString(),
				"{PlainField1=PlainValue12, simpleMap={MapField1=123, MapField2=234}, mapWithList={subList=[{Field1=Value1}, {Field1=Value100, Field2=Value2}], arrayList=[AV1, AV2]}}",
				"Message body");
	}
	
	@Test
	public void convertFromComboMessage() throws ConversionException
	{
		SimpleClearThMessageBuilder builder = new SimpleClearThMessageBuilder();
		
		SimpleClearThMessage elem = builder.subMessageType("subList")
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("Field1", "Value100")
				.build();
		SimpleClearThMessage mapWithList = builder.subMessageType("mapWithList")
				.rg(elem)
				.build();
		
		SimpleClearThMessage list = builder.subMessageType("list")
				.field(MessageToMap.SUBMSGKIND, MessageToMap.KIND_LIST)
				.field("LF2", "LV2")
				.build();
		
		SimpleClearThMessage message = builder.type("Message1")
				.field("PlainField1", "PlainValue12")
				.field("simpleMap_MapField1", "123")
				.field("simpleMap_MapField2", "234")
				.field("list_1_LF1", "LV1")
				.rg(mapWithList)
				.rg(list)
				.build();
		
		Map<String, Object> map = converter.convert(message);
		Assert.assertEquals(map.toString(),
				"{PlainField1=PlainValue12, simpleMap={MapField1=123, MapField2=234}, list=[{LF1=LV1}, {LF2=LV2}], mapWithList={subList=[{Field1=Value100}]}}",
				"Message body");
	}
	
	@Test
	public void testEscapingFieldsInFlatMessage() throws ConversionException
	{
		MessageToMap converter = new MessageToMap();
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("_id", "PlainValue1")
				.field("SubMap_MapField1", "123")
				.field("_SubMap_MapField2", "234")
				.field("_SubMap_subList_1_Field1", "Value1")
				.field("_SubMap_subList_2_Field1", "Value2")
				.field("SubMap_subList_2_Field2", "Value3")
				.build();
		
		Map<String, Object> map = converter.convert(message);
		Assert.assertEquals(map.toString(),
				"{id=PlainValue1, " +
				 "SubMap={MapField1=123, " +
						 "MapField2=234, " +
						 "subList=[{Field1=Value1}, " +
						 			"{Field1=Value2, " +
						 			"Field2=Value3}]}}");
	}
	
	@Test(expectedExceptions = ConversionException.class, expectedExceptionsMessageRegExp = "Invalid list index -10 .*")
	public void invalidFlatIndex() throws ConversionException
	{
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("list_-10_Field1", "Value1")
				.build();
		
		converter.convert(message);
	}
	
	@Test(expectedExceptions = ConversionException.class, expectedExceptionsMessageRegExp = "Invalid list index 10.5 .*")
	public void invalidFlatIndex2() throws ConversionException
	{
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("list_10.5_Field1", "Value1")
				.build();
		
		converter.convert(message);
	}
	
	@Test(expectedExceptions = ConversionException.class, expectedExceptionsMessageRegExp = "Not enough elements in list .*")
	public void tooLargeIndex() throws ConversionException
	{
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("list_2_Field1", "Value1")
				.build();
		
		converter.convert(message);
	}
	
	@Test(expectedExceptions = ConversionException.class, expectedExceptionsMessageRegExp = "Unsupported SubMsgKind .*")
	public void invalidKind() throws ConversionException
	{
		SimpleClearThMessageBuilder builder = new SimpleClearThMessageBuilder();
		SimpleClearThMessage subMessage = builder.subMessageType("Map1")
				.field(MessageToMap.SUBMSGKIND, "InvalidKind")
				.field("FieldQ", "ValueQ")
				.build();
		
		SimpleClearThMessage message = builder.type("Message1")
				.rg(subMessage)
				.build();
		
		converter.convert(message);
	}
	
	
	@Test(dataProvider = "reassignedFields",
			expectedExceptions = ConversionException.class, 
			expectedExceptionsMessageRegExp = ".* already contains field 'Field1'.*")
	public void fieldReassignedWithSubMessage(SimpleClearThMessage subMessage) throws ConversionException
	{
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("Field1", "Value1")
				.rg(subMessage)
				.build();
		
		converter.convert(message);
	}
	
	@Test(expectedExceptions = ConversionException.class, expectedExceptionsMessageRegExp = ".* already contains field 'simpleMap'.*")
	public void fieldReassignedFlat() throws ConversionException
	{
		SimpleClearThMessageBuilder builder = new SimpleClearThMessageBuilder();
		
		SimpleClearThMessage subMessage = builder.subMessageType("simpleMap")
				.field("Field1", "Value1")
				.build();
		
		SimpleClearThMessage message = builder.type("Message1")
				.field("simpleMap_MapField1", "123")
				.rg(subMessage)
				.build();
		
		converter.convert(message);
	}
	
	
	@Test
	public void customFlatDelimiter() throws ConversionException
	{
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("M_Field1", "Value1")
				.field("Sub1.SubField1", "SubValue1")
				.build();
		
		MessageToMap converter = new MessageToMap(".");
		Map<String, Object> map = converter.convert(message);
		Assert.assertEquals(map.toString(),
				"{M_Field1=Value1, Sub1={SubField1=SubValue1}}",
				"Message body");
	}
	
	@Test
	public void flatMessagesOff() throws ConversionException
	{
		SimpleClearThMessage message = new SimpleClearThMessageBuilder().type("Message1")
				.field("SimpleField", "SimpleValue")
				.field("M_Field1", "Value1")
				.build();
		
		MessageToMap converter = new MessageToMap("");
		Map<String, Object> map = converter.convert(message);
		Assert.assertEquals(map.toString(),
				"{SimpleField=SimpleValue, M_Field1=Value1}",
				"Message body");
	}
}
