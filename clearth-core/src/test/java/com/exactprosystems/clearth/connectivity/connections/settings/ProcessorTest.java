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

package com.exactprosystems.clearth.connectivity.connections.settings;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

public class ProcessorTest
{
	private Processor processor;
	
	@BeforeClass
	public void init() throws NoSuchMethodException, SecurityException
	{
		processor = new Processor();
	}
	
	@Test
	public void fieldsProcessing() throws SettingDeclarationException
	{
		SettingsModel model = processor.process(CorrectSettings.class);
		
		checkDefinedColumns(model);
		checkDefinedFieldsOrder(model);
		checkFieldProperties(model);
	}
	
	
	@Test(expectedExceptions = SettingDeclarationException.class, expectedExceptionsMessageRegExp = ".*forceReconnect.*column.*absent.*")
	public void missingColumn() throws SettingDeclarationException
	{
		processor.process(MissingColumnSettings.class);
	}
	
	@Test
	public void autoColumns() throws SettingDeclarationException
	{
		SettingsModel model = processor.process(AutoColumnsAndOrderSettings.class);
		
		for (SettingProperties c : model.getColumnsModel().getColumns())
			Assert.assertNotEquals(c.getInputType(), InputType.PASSWORD, "Type of automatically populated column is "+InputType.PASSWORD);
	}
	
	
	@Test(expectedExceptions = SettingDeclarationException.class, expectedExceptionsMessageRegExp = ".*altPort.*order.*absent.*")
	public void missingOrderField() throws SettingDeclarationException
	{
		processor.process(MissingOrderFieldSettings.class);
	}
	
	@Test
	public void autoOrder() throws SettingDeclarationException
	{
		SettingsModel model = processor.process(AutoColumnsAndOrderSettings.class);
		
		List<Object> expected = Arrays.asList(Arrays.array("host", "port", "login", "password")),
				actual = model.getFieldsModel().getSettingsProps().stream()
						.map(SettingProperties::getFieldName).collect(Collectors.toList());
		Assert.assertEquals(actual, expected, "Fields are automatically extracted from class in order they are defined");
	}
	
	
	@Test(expectedExceptions = SettingDeclarationException.class, expectedExceptionsMessageRegExp = ".*getter.*port.*")
	public void missingGetter() throws SettingDeclarationException
	{
		processor.process(MissingGetterSettings.class);
	}
	
	@Test(expectedExceptions = SettingDeclarationException.class, expectedExceptionsMessageRegExp = ".*setter.*port.*")
	public void missingSetter() throws SettingDeclarationException
	{
		processor.process(MissingSetterSettings.class);
	}
	
	@Test(expectedExceptions = SettingDeclarationException.class, expectedExceptionsMessageRegExp = ".*Name.*reserved.*")
	public void reservedFieldName() throws SettingDeclarationException
	{
		processor.process(SettingsWithName.class);
	}
	
	
	private void checkDefinedColumns(SettingsModel model)
	{
		List<Object> expected = Arrays.asList(Arrays.array("host", "login", "mode")),
				actual = model.getColumnsModel().getColumns().stream()
						.map(SettingProperties::getFieldName).collect(Collectors.toList());
		Assert.assertEquals(actual, expected, "Columns collection looks as defined in annotation");
	}
	
	private void checkDefinedFieldsOrder(SettingsModel model)
	{
		List<Object> expected = Arrays.asList(Arrays.array("login", "password", "host", "port", "mode", "timeout", "autoReconnect", "multiline")),
				actual = model.getFieldsModel().getSettingsProps().stream()
						.map(SettingProperties::getFieldName).collect(Collectors.toList());
		Assert.assertEquals(actual, expected, "Fields are extracted from class in defined order");
	}
	
	private void checkFieldProperties(SettingsModel model)
	{
		FieldsModel fields = model.getFieldsModel();
		
		SoftAssert soft = new SoftAssert();
		soft.assertEquals(fields.getSettingProps("host").getValueTypeInfo().getType(), ValueType.STRING, "Value type of 'host' field");
		soft.assertEquals(fields.getSettingProps("port").getValueTypeInfo().getType(), ValueType.INT, "Value type of 'port' field");
		soft.assertEquals(fields.getSettingProps("login").getName(), "Username", "Defined name of 'login' field");
		soft.assertEquals(fields.getSettingProps("password").getInputType(), InputType.PASSWORD, "Input type for 'password' field");
		
		SettingProperties mode = fields.getSettingProps("mode");
		soft.assertEquals(mode.getName(), "Connection mode", "Defined name of 'mode' field");
		soft.assertEquals(mode.getValueTypeInfo().getType(), ValueType.ENUM, "Value type of 'mode' field");
		
		soft.assertEquals(fields.getSettingProps("timeout").getValueTypeInfo().getType(), ValueType.LONG, "Value type of 'timeout' field");
		
		SettingProperties autoReconnect = fields.getSettingProps("autoReconnect");
		soft.assertEquals(autoReconnect.getName(), "AutoReconnect", "Automatically assigned name of 'autoReconect' field");
		soft.assertEquals(autoReconnect.getValueTypeInfo().getType(), ValueType.BOOLEAN, "Value type of 'autoReconect' field");
		
		ValueTypeInfo multilineInfo = fields.getSettingProps("multiline").getValueTypeInfo();
		soft.assertEquals(multilineInfo.getType(), ValueType.SPECIAL, "Value type of 'multiline' field");
		soft.assertEquals(multilineInfo.getValueClass(), List.class, "Value class of 'multiline' field");
		soft.assertAll();
	}
}
