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
