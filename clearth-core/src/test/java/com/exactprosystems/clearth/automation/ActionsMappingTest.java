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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.utils.SettingsException;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class ActionsMappingTest {

	private static final Path PATH_TO_CONF_FILES = Paths.get("src/test/resources/Action/Preparable/");
	private static final Path PATH_TO_ACTIONS = Paths.get("com.exactprosystems.clearth.automation.actions.");

	private final ActionsMapping mapping = new ActionsMapping(
		   PATH_TO_CONF_FILES.resolve("actionmapping_checkparams.cfg"), true);

	private final ActionsMapping mappingTemplate = new ActionsMapping(
		   PATH_TO_CONF_FILES.resolve("actionmapping_template.cfg"), true);

	public ActionsMappingTest() throws SettingsException {}

	@DataProvider(name = "causingInfiniteLoopFiles")
	public Object[][] causingInfiniteLoopFiles()
	{
		return new Object[][]
				{
						{"actionsmapping_fail_infinite_loop.cfg"},
						{"actionsmapping_fail_infinite_loop2.cfg"}
				};
	}

	@Test(expectedExceptions = SettingsException.class, dataProvider = "causingInfiniteLoopFiles")
	public void testIncorrectParamsCausingAnInfiniteLoop(String fileName) throws SettingsException
	{
		ActionsMapping mappingWithException = new ActionsMapping(PATH_TO_CONF_FILES.resolve(fileName), true);
	}

	@Test
	public void parseDefaultInputParams_Simple() throws SettingsException
	{
		String actionName = "ActionSimple";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One");

		assertEquals(actual, expected);
	}

	@Test
	public void parseDefaultInputParams_Simple2() throws SettingsException
	{
		String actionName = "ActionSimple2";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One", "param2", "Two", "param3", "Three");

		assertEquals(actual, expected);
	}

	@Test
	public void parseDefaultInputParams_Whitespaces() throws SettingsException
	{
		String actionName = "ActionWhiteSpaces";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One", "param2", "Two", "param3", "Three");

		assertEquals(actual, expected);
	}

	@Test
	public void parseDefaultInputParams_Quoted() throws SettingsException
	{
		String actionName = "ActionQuoted";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One");

		assertEquals(actual, expected);
	}

	@Test
	public void parseDefaultInputParams_Quoted2() throws SettingsException
	{
		String actionName = "ActionQuoted2";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One", "param2", "T\"w\"o", "OtherParam", ",=\t!@$%^&*[]{}()\\");

		assertEquals(actual, expected);
	}

	@Test
	public void parseDefaultInputParams_ActionWithLongParam() throws SettingsException
	{
		String actionName = "ActionWithLongParam";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("loooooooooooooooooooooooooongparamname", "loooooooooooooooooooooooooongparamvalue",
				"param2", "value2");

		assertEquals(actual, expected);
	}

	@Test(expectedExceptions = SettingsException.class)
	public void parseDefaultInputParams_FailWithoutValue() throws SettingsException
	{
		ActionsMapping mappingWithException = new ActionsMapping(
				PATH_TO_CONF_FILES.resolve("actionmapping_fail_withoutvalue.cfg"), true);
	}

	@Test(expectedExceptions = SettingsException.class)
	public void parseDefaultInputParams_FailUnclosedQuote() throws SettingsException
	{
		ActionsMapping mappingWithException = new ActionsMapping(
			   PATH_TO_CONF_FILES.resolve("actionmapping_fail_unclosedquote.cfg"), true);
	}

	@Test(expectedExceptions = SettingsException.class)
	public void parseDefaultInputParams_FailWrongFirstCharacter() throws SettingsException
	{
		ActionsMapping mappingWithException = new ActionsMapping(
				PATH_TO_CONF_FILES.resolve("actionmapping_fail_wrongfirstchar.cfg"), true);
	}

	@Test(expectedExceptions = SettingsException.class)
	public void parseDefaultInputParams_FailWrongFirstCharacterNumber() throws SettingsException
	{
		ActionsMapping mappingWithException = new ActionsMapping(
				PATH_TO_CONF_FILES.resolve("actionmapping_fail_wrongfirstchar_number.cfg"), true);
	}

	@Test
	public void parseByCommonActionTemplate() throws SettingsException
	{
		String initialActionName = PATH_TO_ACTIONS + "ReceiveMessage";

		String firstAction = "ReceiveMessageXml".toLowerCase();
		String secondAction = "ReceiveMessageX".toLowerCase();

		Map<String, String> expected = buildMap("Codec", "Xml");
		Map<String, String> expected1 = buildMap("Codec", "Xml", "MsgType", "X");

		Map<String, String> actual = mappingTemplate.getParamsByActionName(firstAction);
		Map<String, String> actual1 = mappingTemplate.getParamsByActionName(secondAction);

		assertEquals(actual, expected);
		assertEquals(actual1, expected1);
		assertEquals(mappingTemplate.getClassByActionName(firstAction), initialActionName);
		assertEquals(mappingTemplate.getClassByActionName(secondAction), initialActionName);
	}

	@Test
	public void parseActionWithNoReference() throws SettingsException
	{
		String actionName = "TestAction";
		String actionNameLowCase = actionName.toLowerCase();
		String pathToAction = PATH_TO_ACTIONS + "TestAction";

		assertEquals(mappingTemplate.getNameByActionName(actionNameLowCase), actionName);
		assertEquals(mappingTemplate.getClassByActionName(actionNameLowCase), pathToAction);
	}

	@Test
	public void parseActionWithNoReferenceWithParams() throws SettingsException
	{
		Map<String, String> expected = buildMap("MsgType", "WithParams");

		String actionName = "TestActionParams";
		String actionNameLowCase = actionName.toLowerCase();
		String pathToAction = PATH_TO_ACTIONS + "TestActionParams";

		Map<String, String> actual = mappingTemplate.getParamsByActionName(actionNameLowCase);

		assertEquals(mappingTemplate.getNameByActionName(actionNameLowCase), actionName);
		assertEquals(mappingTemplate.getClassByActionName(actionNameLowCase), pathToAction);
		assertEquals(actual, expected);
	}

	@Test
	public void parsePackageWithReference() throws SettingsException
	{
		String packageName = "actionsdb";
		String pathToPackage = PATH_TO_ACTIONS + "db";

		assertEquals(mappingTemplate.getNameByActionName(packageName), packageName);
		assertEquals(mappingTemplate.getClassByActionName(packageName), pathToPackage);
	}

	@Test
	public void parseActionWithRefInPackage() throws SettingsException
	{
		String actionName = "ActionWithRefInPackage";
		String actionNameLowCase = actionName.toLowerCase();
		String pathToAction = PATH_TO_ACTIONS + "db.ActionWithRefInPackage";

		assertEquals(mappingTemplate.getNameByActionName(actionNameLowCase), actionName);
		assertEquals(mappingTemplate.getClassByActionName(actionNameLowCase), pathToAction);
	}

	private static Map<String, String> buildMap(String... keysAndValues)
	{
		if (keysAndValues.length % 2 != 0)
			throw new RuntimeException("Please use even number of arguments for buildMap() method");
		Map<String, String> map = new LinkedHashMap<>();
		for (int i = 1; i < keysAndValues.length; i+=2)
		{
			map.put(keysAndValues[i-1], keysAndValues[i]);
		}
		return map;
	}
}
