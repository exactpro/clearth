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
			dictionaryPath = resourcesPath.resolve("dicts").resolve("dictionary.xml");
	
	private JsonCodec codec;
	
	@BeforeClass
	public void init() throws DictionaryLoadException, IOException
	{
		codec = new JsonCodec(new JsonDictionary(dictionaryPath.toFile().getAbsolutePath(), null), null);
	}
	
	@DataProvider(name = "messages")
	public Object[][] messageData()
	{
		ClearThJsonMessage messageObject = message(map("MsgType", "TestMessage",
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

		ClearThJsonMessage msgArray = message(map("MsgType", "arr"),
				message(map("SubMsgType", "key", "dataType", "1")),
				message(map("SubMsgType", "key", "dataType", "2"))
		);

		return new Object[][]
				{
					{
						messageObject, "allTypes.json"
					},
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
					},
					{
						msgArray, "arrType.json"
					}
				};
	}

	@Test(dataProvider = "messages")
	public void decodeMessage(ClearThJsonMessage msgObject, String fileName) throws DecodeException, IOException
	{
		Path path = resourcesPath.resolve("messages").resolve(fileName);
		String message = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
		ClearThJsonMessage decoded = (ClearThJsonMessage) codec.decode(message);
		Assert.assertEquals(decoded, msgObject);
	}

	@Test(dataProvider = "messages")
	public void encodeMessage(ClearThJsonMessage msgObject, String fileName) throws IOException, EncodeException
	{
		Path path = resourcesPath.resolve("messages").resolve(fileName);
		String encoded = codec.encode(msgObject);
		String message = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
		Assert.assertEquals(encoded, message);
	}

	@Test(expectedExceptions = EncodeException.class, expectedExceptionsMessageRegExp = "Sub-message 'Map' does not have field 'key' required for encoding")
	public void encodeMapMsgWithEmptyKeyNameParam() throws EncodeException
	{
		ClearThJsonMessage msg = message(map("MsgType", "TestMapMessage4"), message((map("SubMsgType", "Map", "v", "1", "x", "2"))));
		codec.encode(msg);
	}

	@Test(expectedExceptions = EncodeException.class,
			expectedExceptionsMessageRegExp = "Message definition with type 'TestIncorrectDict' has 'rootType=\"array\"' and cannot have more than one fieldDesc.")
	public void encodeIncorrectArrayMessageDescType() throws EncodeException
	{
		ClearThJsonMessage msg = message(map("MsgType", "TestIncorrectDict"),
				message(map("SubMsgType", "key", "dataType", "1")),
				message(map("SubMsgType", "key", "dataType", "2")),
				message(map("SubMsgType", "errKey", "dataType", "3")),
				message(map("SubMsgType", "errKey", "dataType", "4"))
		);
		codec.encode(msg);
	}

	private ClearThJsonMessage message(Map<String, String> fields, ClearThJsonMessage... subMessages)
	{
		return new ClearThJsonMessage(fields, subMessages.length == 0 ? null : Arrays.asList(subMessages));
	}
}
