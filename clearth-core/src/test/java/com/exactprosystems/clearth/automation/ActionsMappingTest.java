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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.utils.SettingsException;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ActionsMappingTest {

	private static final Path PATH_TO_CONF_FILES = Paths.get("src/test/resources/Action/Preparable/");
	private static final Path PATH_TO_ACTIONS = Paths.get("com.exactprosystems.clearth.automation.actions.");

	private final ActionsMapping mapping = new ActionsMapping(
		   PATH_TO_CONF_FILES.resolve("actionmapping_checkparams.cfg"), true);

	private final ActionsMapping mappingTemplate = new ActionsMapping(
		   PATH_TO_CONF_FILES.resolve("actionmapping_template.cfg"), true);

	public ActionsMappingTest() throws SettingsException {}

	@Test
	public void parseDefaultInputParams_Simple() throws SettingsException
	{
		String actionName = "ActionSimple";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One");

		assertEquals(expected, actual);
	}

	@Test
	public void parseDefaultInputParams_Simple2() throws SettingsException
	{
		String actionName = "ActionSimple2";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One", "param2", "Two", "param3", "Three");

		assertEquals(expected, actual);
	}

	@Test
	public void parseDefaultInputParams_Whitespaces() throws SettingsException
	{
		String actionName = "ActionWhiteSpaces";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One", "param2", "Two", "param3", "Three");

		assertEquals(expected, actual);
	}

	@Test
	public void parseDefaultInputParams_Quoted() throws SettingsException
	{
		String actionName = "ActionQuoted";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One");

		assertEquals(expected, actual);
	}

	@Test
	public void parseDefaultInputParams_Quoted2() throws SettingsException
	{
		String actionName = "ActionQuoted2";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("param1", "One", "param2", "T\"w\"o", "OtherParam", ",=\t!@$%^&*[]{}()\\");

		assertEquals(expected, actual);
	}

	@Test
	public void parseDefaultInputParams_ActionWithLongParam() throws SettingsException
	{
		String actionName = "ActionWithLongParam";
		Map<String, String> actual = mapping.getParamsByActionName(actionName.toLowerCase());

		Map<String, String> expected = buildMap("loooooooooooooooooooooooooongparamname", "loooooooooooooooooooooooooongparamvalue",
				"param2", "value2");

		assertEquals(expected, actual);
	}

	@Test(expected = SettingsException.class)
	public void parseDefaultInputParams_FailWithoutValue() throws SettingsException
	{
		ActionsMapping mappingWithException = new ActionsMapping(
				PATH_TO_CONF_FILES.resolve("actionmapping_fail_withoutvalue.cfg"), true);
	}

	@Test(expected = SettingsException.class)
	public void parseDefaultInputParams_FailUnclosedQuote() throws SettingsException
	{
		ActionsMapping mappingWithException = new ActionsMapping(
			   PATH_TO_CONF_FILES.resolve("actionmapping_fail_unclosedquote.cfg"), true);
	}

	@Test(expected = SettingsException.class)
	public void parseDefaultInputParams_FailWrongFirstCharacter() throws SettingsException
	{
		ActionsMapping mappingWithException = new ActionsMapping(
				PATH_TO_CONF_FILES.resolve("actionmapping_fail_wrongfirstchar.cfg"), true);
	}

	@Test(expected = SettingsException.class)
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

		assertEquals(expected, actual);
		assertEquals(expected1, actual1);
		assertEquals(initialActionName, mappingTemplate.getClassByActionName(firstAction));
		assertEquals(initialActionName, mappingTemplate.getClassByActionName(secondAction));
	}

	@Test
	public void parseActionWithNoReference() throws SettingsException
	{
		String actionName = "TestAction";
		String actionNameLowCase = actionName.toLowerCase();
		String pathToAction = PATH_TO_ACTIONS + "TestAction";

		assertEquals(actionName, mappingTemplate.getNameByActionName(actionNameLowCase));
		assertEquals(pathToAction, mappingTemplate.getClassByActionName(actionNameLowCase));
	}

	@Test
	public void parseActionWithNoReferenceWithParams() throws SettingsException
	{
		Map<String, String> expected = buildMap("MsgType", "WithParams");

		String actionName = "TestActionParams";
		String actionNameLowCase = actionName.toLowerCase();
		String pathToAction = PATH_TO_ACTIONS + "TestActionParams";

		Map<String, String> actual = mappingTemplate.getParamsByActionName(actionNameLowCase);

		assertEquals(actionName, mappingTemplate.getNameByActionName(actionNameLowCase));
		assertEquals(pathToAction, mappingTemplate.getClassByActionName(actionNameLowCase));
		assertEquals(expected, actual);
	}

	@Test
	public void parsePackageWithReference() throws SettingsException
	{
		String packageName = "actionsdb";
		String pathToPackage = PATH_TO_ACTIONS + "db";

		assertEquals(packageName, mappingTemplate.getNameByActionName(packageName));
		assertEquals(pathToPackage, mappingTemplate.getClassByActionName(packageName));
	}

	@Test
	public void parseActionWithRefInPackage() throws SettingsException
	{
		String actionName = "ActionWithRefInPackage";
		String actionNameLowCase = actionName.toLowerCase();
		String pathToAction = PATH_TO_ACTIONS + "db.ActionWithRefInPackage";

		assertEquals(actionName, mappingTemplate.getNameByActionName(actionNameLowCase));
		assertEquals(pathToAction, mappingTemplate.getClassByActionName(actionNameLowCase));
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
