/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.connectivity.xml;

import com.exactprosystems.clearth.utils.ComparisonUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;
import static com.exactprosystems.clearth.utils.FileOperationUtils.resourceToAbsoluteFilePath;
import static org.junit.Assert.*;

public class XmlCodecTest
{
	public static final String DICTIONARY_PATH = "dicts/dictionary.xml";
	
	public static final ClearThXmlMessage INFINITE_LOOP_BUG = message(
			map("MsgType", "infiniteLoopBug"),
			message(map("SubMsgType", "b",
					"F", "123",
					"G", "1",
					"e", "asd")),
			message(map("SubMsgType", "b",
					"F", "456",
					"G", "2",
					"n", "48",
					"e", "fgh",
					"h", "A")),
			message(map("SubMsgType", "b",
					"F", "789",
					"G", "3",
					"e", "fgh"))
	);
	
	public static final ClearThXmlMessage REPEATING_GROUP = message(
			map("MsgType", "repeatingGroups"),
			message(map("SubMsgType", "A"),
					message(map("SubMsgType", "E"),
							message(map("SubMsgType", "F",
										"H", "1"))),
					message(map("SubMsgType", "E"),
							message(map("SubMsgType", "F",
										"H", "2")),
							message(map("SubMsgType", "F",
										"H", "3")))),
			message(map("SubMsgType", "A"),
					message(map("SubMsgType", "E"),
							message(map("SubMsgType", "F",
										"H", "4"))),
					message(map("SubMsgType", "E"),
							message(map("SubMsgType", "F",
										"H", "5"))),
					message(map("SubMsgType", "E"),
							message(map("SubMsgType", "F",
										"H", "6")),
							message(map("SubMsgType", "F",
										"H", "7")),
							message(map("SubMsgType", "F",
										"H", "8"))))
	);
	
	public static final ClearThXmlMessage SIMPLE_REPEATING = message(
			map("MsgType", "simpleRepeating"),
			message(map("SubMsgType", "user",
						"id", "1",
						"name", "admin")),
			message(map("SubMsgType", "user",
					"id", "2",
					"name", "user")),
			message(map("SubMsgType", "user",
					"id", "3",
					"name", "guest"))			
	);
	
	public static final ClearThXmlMessage ATTRIBUTES = message(
			map("MsgType", "attributes",
				"msgAttrA", "45",
				"msgAttrB", "67",
				"commonAttrA", "54",
				"commonAttrB", "76",
				"fieldAttrA", "10",
				"fieldAttrB", "30",
				"commonWithAttrs", "abc",
				"withAttrs", "def"));
	
	public static final ClearThXmlMessage COMMON_FIELDS_A = message(
			map("MsgType", "commonFieldsA",
				"privateFieldF", "123",
				"innerCommonFieldA", "345",
				"innerCommonFieldB", "678",
				"innerCommonFieldC", "90"));

	public static final ClearThXmlMessage COMMON_FIELDS_B = message(
			map("MsgType", "commonFieldsB",
				"privateFieldG", "12",
				"innerCommonFieldB", "34",
				"innerCommonFieldC", "56",
				"innerCommonFieldD", "78",
				"innerCommonFieldE", "90"));
	
	public static final ClearThXmlMessage COMMON_FIELDS_AC = message(
			map("MsgType", "commonFieldsAC",
					"privateFieldAC", "123",
					"innerCommonFieldA", "345",
					"innerCommonFieldB", "678",
					"innerCommonFieldC", "90"));
	
	public static final ClearThXmlMessage SELF_CLOSING = message(
			map("MsgType", "selfClosingType"));
	
	public static final ClearThXmlMessage COMMON_FIELDS_GROUP = message(
			map("MsgType", "commonFieldsGroup",
				"a", "12", "b", "34", "c", "56", "d", "78"));
	
	public static final ClearThXmlMessage NAMESPACE = message(
			map("MsgType", "namespace", "a", "123", "c", "456"));
	static 
	{
		NAMESPACE.setNamespacePrefix("test");
	}

	public static final ClearThXmlMessage NAME_AND_SOURCE = message(
			map("MsgType", "nameAndSource",
					"A", "1", "B", "2", "C", "3", "D", "4", "E", "5",
					"F", "6", "FF", "66", "G", "7", "GG", "77", "H", "8", "HH", "88"));

	protected XmlCodec codec;
	
	@Before
	public void setUp() throws Exception
	{
		codec = new XmlCodec(new XmlDictionary(resourceToAbsoluteFilePath(DICTIONARY_PATH)));
	}
	
	//////////////// DECODING ////////////////////////
	
	@Test(timeout = 5000 /* Prevent infinite loop */)
	public void checkInfiniteLoopBug() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/infiniteLoopBug.xml"), INFINITE_LOOP_BUG);
	}

	@Test(timeout = 5000 /* Prevent infinite loop */)
	public void decodeRepeatingGroups() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/repeatingGroups.xml"), REPEATING_GROUP);
	}

	@Ignore("This feature is unused now")
	@Test(timeout = 5000 /* Prevent infinite loop */)
	public void decodeSimpleRepeating() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/simpleRepeating.xml"), SIMPLE_REPEATING);
	}
	
	@Test
	public void decodeAttributes() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/attributes.xml"), ATTRIBUTES);
	}
	
	@Test
	public void decodeCommonFields() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/commonFieldsA.xml"), COMMON_FIELDS_A);
		decode(resourceToAbsoluteFilePath("messages/commonFieldsB.xml"), COMMON_FIELDS_B);
		decode(resourceToAbsoluteFilePath("messages/commonFieldsAC.xml"), COMMON_FIELDS_AC);
	}
	
	@Test
	public void decodeSelfEnclosing() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/selfClosing.xml"), SELF_CLOSING);
	}
	
	@Test
	public void decodeCommonFieldsGroup() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/commonFieldsGroup.xml"), COMMON_FIELDS_GROUP);
	}
	
	@Test
	public void decodeEmpty() throws Exception
	{
		
		String messageText = new String(Files.readAllBytes(Paths.get(resourceToAbsoluteFilePath("messages/empty.xml"))));
		ClearThXmlMessage decoded = codec.decode(messageText);
		assertNull(decoded.getField("absent"));
		assertNull(decoded.getField("absentWithDefault"));
		assertNull(decoded.getField("absentWithEmptyDefault"));
		assertEquals("", decoded.getField("emptyByMatrix"));
		assertEquals("", decoded.getField("emptyByMatrixWithDefault"));
		assertEquals("", decoded.getField("emptyWithDefault"));
	}
	
	@Test
	public void decodeNamespace() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/namespace.xm"), NAMESPACE);
	}

	@Test
	public void decodeNameAndSource() throws Exception
	{
		decode(resourceToAbsoluteFilePath("messages/nameAndSource.xml"), NAME_AND_SOURCE);
	}
	
	protected void decode(String messagePath, ClearThXmlMessage expected) throws Exception
	{
		String encodedMessage = new String(Files.readAllBytes(Paths.get(messagePath)));
		ClearThXmlMessage decoded = codec.decode(encodedMessage);
		assertEquals(expected, decoded);
	}

	//////////////// ENCODING ////////////////////////
	
	@Test
	public void encodeRepeatingGroups() throws Exception
	{
		encode(REPEATING_GROUP, resourceToAbsoluteFilePath("messages/repeatingGroups.xml"));
	}
	
	@Test
	public void encodeInfinite() throws Exception
	{
		encode(INFINITE_LOOP_BUG, resourceToAbsoluteFilePath("messages/infiniteLoopBug.xml"));
	}
	
	@Test
	public void encodeSimpleRepeating() throws Exception
	{
		encode(SIMPLE_REPEATING, resourceToAbsoluteFilePath("messages/simpleRepeating.xml"));
	}
	
	@Test
	public void encodeAttributes() throws Exception
	{
		encode(ATTRIBUTES, resourceToAbsoluteFilePath("messages/attributes.xml"));
	}
	
	@Test
	public void encodeCommonFields() throws Exception
	{
		encode(COMMON_FIELDS_A, resourceToAbsoluteFilePath("messages/commonFieldsA.xml"));
		encode(COMMON_FIELDS_B, resourceToAbsoluteFilePath("messages/commonFieldsB.xml"));
	}
	
	@Test
	public void encodeCommonFieldsGroup() throws Exception
	{
		encode(COMMON_FIELDS_GROUP, resourceToAbsoluteFilePath("messages/commonFieldsGroup.xml"));
	}
	
	@Test
	public void encodeEmpty() throws Exception
	{
		ClearThXmlMessage m = message(map("MsgType", "empty",
				"emptyByMatrix", ComparisonUtils.IS_EMPTY,
				"emptyByMatrixWithDefault", ComparisonUtils.IS_EMPTY,
				"emptyContainer", ClearThXmlMessage.EMPTY_VALUE,
				"emptyByMatrixNonSelfClosed", ComparisonUtils.IS_EMPTY,
				"emptyContainerNonSelfClosed", ComparisonUtils.IS_EMPTY));
		encode(m, resourceToAbsoluteFilePath("messages/emptyEncoded.xml"));
	}
	
	@Test
	public void encodeNamespace() throws Exception
	{
		encode(NAMESPACE, resourceToAbsoluteFilePath("messages/namespace.xm"));
	}

	@Test
	public void encodeNameAndSource() throws Exception
	{
		encode(NAME_AND_SOURCE, resourceToAbsoluteFilePath("messages/nameAndSource.xml"));
	}
	
	protected void encode(ClearThXmlMessage message, String pathToExpected) throws Exception
	{
		String encodedMessage = codec.encode(message);
		String expected = FileUtils.readFileToString(new File(pathToExpected), Charset.forName("UTF-8"));
		assertEquals(expected, encodedMessage);
	}
	
	//////////////////////////////////////////////////
	
	public static ClearThXmlMessage message(Map<String, String> fields, ClearThXmlMessage... subMessages)
	{
		return new ClearThXmlMessage(fields, subMessages.length == 0 ? null : Arrays.asList(subMessages));
	}
}
