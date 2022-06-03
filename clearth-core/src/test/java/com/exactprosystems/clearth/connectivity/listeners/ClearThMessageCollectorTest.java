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

package com.exactprosystems.clearth.connectivity.listeners;

import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.SimpleKeyValueCodec;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;
import static com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector.DEFAULT_MESSAGE_END_INDICATOR;
import static com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector.ALLOWED_TYPES;
import static com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector.FORBIDDEN_TYPES;

public class ClearThMessageCollectorTest
{
	private static final Path MESSAGE_COLLECTOR_TEST_OUTPUT_DIR = Paths.get("src/test/resources/MessageCollectorTest");

	private static final String MESSAGES_FILE = "messages.txt";
	private static final String MESSAGES_DELIMITER = "\n\n\n";
	
	private ICodec codec;

	@BeforeClass
	public void init()
	{
		codec = new SimpleKeyValueCodec();
	}

	@DataProvider(name = "listeners-and-messages")
	public Object[][] createListenersAndMessagesData()
			throws IOException, SettingsException
	{
		String[] messages = getMessagesFromFile(MESSAGES_FILE);
		
		Map<String, String> allowedTypesSettings = new HashMap<>();
		allowedTypesSettings.put(ALLOWED_TYPES, "AAA,CCC,EEE,GGG");
		
		Map<String, String> forbiddenTypesSettings = new HashMap<>();
		forbiddenTypesSettings.put(FORBIDDEN_TYPES, "AAA,CCC,EEE,GGG");

		ClearThMessageCollector defaultListener = createListener("Default", codec, new HashMap<>());
		ClearThMessageCollector allowedTypesListener = createListener("Allowed", codec, allowedTypesSettings);
		ClearThMessageCollector forbiddenTypesListener = createListener("Forbidden", codec, forbiddenTypesSettings);
		
		return new Object[][]
			{
				{defaultListener, messages, new HashSet<>(Arrays.asList("AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "GGG"))},
				{allowedTypesListener, messages, new HashSet<>(Arrays.asList("AAA", "CCC", "EEE", "GGG"))},
				{forbiddenTypesListener, messages, new HashSet<>(Arrays.asList("BBB", "DDD", "FFF"))},
			};
	}

	@Test(dataProvider = "listeners-and-messages")
	public void checkIfMessageFilteringByTypeWorks(ClearThMessageCollector listener, String[] messages, Set<String> expected)
	{
		Arrays.stream(messages).forEach(listener::onMessageReceived);
		
		Set<String> actual = listener
								.getMessages()
								.stream()
								.map(msg->msg.getField(MSGTYPE))
								.collect(Collectors.toSet());
		
		Assert.assertEquals(actual, expected);
	}
	
	@Test(expectedExceptions = SettingsException.class)
	public void checkIfBothFilterParamCannotBeUsedTogether() throws SettingsException
	{
		Map<String, String> settings = new HashMap<>();
		settings.put(ALLOWED_TYPES, "AAA");
		settings.put(FORBIDDEN_TYPES, "AAA");
		
		ClearThMessageCollector listener = createListener("Wrong", codec, settings);
	}
	
	private ClearThMessageCollector createListener(String name, ICodec codec, Map<String, String> settings)
			throws SettingsException
	{
		return new ClearThMessageCollector(name, "con", codec, settings, DEFAULT_MESSAGE_END_INDICATOR);
	}
	
	private String[] getMessagesFromFile(String fileName) throws IOException
	{
		File file = MESSAGE_COLLECTOR_TEST_OUTPUT_DIR.resolve(fileName).toFile();
		return FileUtils.readFileToString(file, Charset.defaultCharset()).split(MESSAGES_DELIMITER);
	}
	
}