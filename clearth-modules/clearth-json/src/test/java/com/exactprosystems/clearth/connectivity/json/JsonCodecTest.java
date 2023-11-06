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

package com.exactprosystems.clearth.connectivity.json;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.utils.DictionaryLoadException;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

import static com.exactprosystems.clearth.utils.CollectionUtils.map;

public class JsonCodecTest
{
	private final Path resourcesPath = Paths.get("src", "test", "resources"),
			dictionaryPath = resourcesPath.resolve("dicts").resolve("dictionary.xml"),
			messagePath = resourcesPath.resolve("messages").resolve("allTypes.json");
	
	private JsonCodec codec;
	private String messageText;
	private ClearThJsonMessage messageObject;
	
	@BeforeClass
	public void init() throws DictionaryLoadException, IOException
	{
		codec = new JsonCodec(new JsonDictionary(dictionaryPath.toFile().getAbsolutePath(), null), null);
		messageText = FileUtils.readFileToString(messagePath.toFile(), StandardCharsets.UTF_8);
		messageObject = message(map("MsgType", "TestMessage",
				"Name", "DummyName",
				"Price", "250",
				"Timestamp", "2023-03-27 15:30:24",
				"Confirmed", "true",
				"Variants", "1, 3",
				"Alternatives", "alt1, alt2"),
				message(map("SubMsgType", "Linked",
						"name", "LD1")),
				message(map("SubMsgType", "Linked",
						"name", "Link2"))
			);
	}

	@DataProvider(name = "mapMessage")
	public Object[][] mapMessageData()
	{
		ClearThJsonMessage mapMsgObj1 = message(map("MsgType", "TestMapMessage1", "Name", "MappingMessage"),
				message(map("SubMsgType", "Map", "key", "a", "v", "123"),
						message(map("SubMsgType", "x", "v1", "1", "v2", "2")),
						message(map("SubMsgType", "x", "v1", "99", "v2", "39", "v3", "120"))),
				message(map("SubMsgType", "Map", "key", "b", "v", "123"),
						message(map("SubMsgType", "x", "v1", "1", "v2", "2")),
						message(map("SubMsgType", "x", "v1", "99", "v2", "39", "v3", "120")))
		);

		ClearThJsonMessage mapMsgObj2 = message(map("MsgType", "TestMapMessage2", "Name", "MappingMessage"),
				message(map("SubMsgType", "Map", "key", "a", "v", "1", "x", "2")),
				message(map("SubMsgType", "Map", "key", "b", "v", "12", "x", "22")),
				message(map("SubMsgType", "Map", "key", "c", "v", "13", "x", "23"))
		);

		ClearThJsonMessage mapMsgObj3 = message(map("MsgType", "TestMapMessage3"),
				message(map("SubMsgType", "Map", "MapKey", "a", "v", "1", "x", "2")),
				message(map("SubMsgType", "Map", "MapKey", "b", "v", "12", "x", "22")),
				message(map("SubMsgType", "Map", "MapKey", "c", "v", "13", "x", "23"))
		);

		ClearThJsonMessage mapMsgObj4 = message(map("MsgType", "TestMapMessage4"),
				message(map("SubMsgType", "Map", "key", "a", "v", "1", "x", "2")),
				message(map("SubMsgType", "Map", "key", "b", "v", "12", "x", "22")),
				message(map("SubMsgType", "Map", "key", "c", "v", "13", "x", "23"))
		);

		return new Object[][]
				{
					{
						mapMsgObj1, "mapType1.json"
					},
					{
						mapMsgObj2, "mapType2.json"
					},
					{
						mapMsgObj3, "mapType3.json"
					},
					{
						mapMsgObj4, "mapType4.json"
					}
				};
	}

	@Test(dataProvider = "mapMessage")
	public void decodeMapMessage(ClearThJsonMessage mapMsgObject, String fileName) throws DecodeException, IOException
	{
		Path path = resourcesPath.resolve("messages").resolve(fileName);
		String message = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
		ClearThJsonMessage decoded = (ClearThJsonMessage) codec.decode(message);
		Assert.assertEquals(decoded, mapMsgObject);
	}

	@Test(dataProvider = "mapMessage")
	public void encodeMapMessage(ClearThJsonMessage mapMsgObject, String fileName) throws IOException, EncodeException
	{
		Path path = resourcesPath.resolve("messages").resolve(fileName);
		String encoded = codec.encode(mapMsgObject);
		String message = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
		Assert.assertEquals(encoded, message);
	}

	@Test(expectedExceptions = EncodeException.class, expectedExceptionsMessageRegExp = "Sub-message 'Map' does not have field 'key' required for encoding")
	public void encodeMapMsgWithEmptyKeyNameParam() throws EncodeException, IOException
	{
		ClearThJsonMessage msg = message(map("MsgType", "TestMapMessage4"), message((map("SubMsgType", "Map", "v", "1", "x", "2"))));
		codec.encode(msg);
	}

	@Test
	public void decode() throws DecodeException
	{
		ClearThJsonMessage decoded = (ClearThJsonMessage) codec.decode(messageText);
		Assert.assertEquals(decoded, messageObject);
	}
	
	@Test
	public void encode() throws EncodeException
	{
		String encoded = codec.encode(messageObject);
		Assert.assertEquals(encoded, messageText);
	}
	
	private ClearThJsonMessage message(Map<String, String> fields, ClearThJsonMessage... subMessages)
	{
		return new ClearThJsonMessage(fields, subMessages.length == 0 ? null : Arrays.asList(subMessages));
	}
}
