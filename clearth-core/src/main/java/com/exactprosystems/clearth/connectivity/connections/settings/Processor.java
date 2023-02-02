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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnectionSettings;

public class Processor
{
	private static final String FIELD_NAME = "Name";
	
	private final SettingProperties nameProps;
	
	public Processor() throws NoSuchMethodException, SecurityException
	{
		nameProps = new SettingProperties(FIELD_NAME, InputType.TEXTBOX, ValueClass.STRING, 
				ClearThConnection.class.getMethod("getName"),
				ClearThConnection.class.getMethod("setName", String.class));
	}
	
	public SettingsModel process(Class<? extends ClearThConnectionSettings> settingsClass) throws SettingDeclarationException
	{
		List<Field> fields = getFields(settingsClass);
		if (fields.isEmpty())
			return new SettingsModel(new FieldsModel(nameProps, Collections.emptyList()), 
					new ColumnsModel(Collections.emptyList()));
		
		Map<String, SettingProperties> annotatedProps = processFieldAnnotations(fields, settingsClass);
		
		ConnectionSettings settings = settingsClass.getAnnotation(ConnectionSettings.class);
		
		String[] columnFields = getColumnFields(settings);
		Collection<SettingProperties> columnProps = processColumnsAnnotation(columnFields, annotatedProps);
		
		String[] orderFields = getOrderFields(settings);
		Collection<SettingProperties> fieldsProps = processOrderAnnotation(orderFields, annotatedProps);
		
		return new SettingsModel(new FieldsModel(nameProps, fieldsProps), 
				new ColumnsModel(columnProps));
	}
	
	private List<Field> getFields(Class<?> clazz)
	{
		List<Field> result = new ArrayList<>(Arrays.asList(clazz.getDeclaredFields()));
		Class<?> parent = clazz.getSuperclass();
		if (ClearThConnectionSettings.class.isAssignableFrom(parent))
		{
			List<Field> parentFields = getFields(parent);
			if (!parentFields.isEmpty())
				result.addAll(0, parentFields);
		}
		return result;
	}
	
	private String[] getColumnFields(ConnectionSettings settings)
	{
		return settings == null ? null : settings.columns();
	}
	
	private String[] getOrderFields(ConnectionSettings settings)
	{
		return settings == null ? null : settings.order();
	}
	
	private Map<String, SettingProperties> processFieldAnnotations(List<Field> fields, Class<?> fieldsOwner) throws SettingDeclarationException
	{
		Map<String, SettingProperties> result = new LinkedHashMap<>(fields.size());
		for (Field f : fields)
		{
			ConnectionSetting annotation = f.getAnnotation(ConnectionSetting.class);
			if (annotation == null)
				continue;
			
			if (FIELD_NAME.equals(f.getName()))
				throw new SettingDeclarationException("Settings should not contain field '"+FIELD_NAME+"', it is reserved");
			
			String name = getName(annotation, f);
			InputType type = annotation.inputType();
			ValueClass valueClass = getValueClass(f.getType());
			SettingProperties setting = createSettingData(name, type, valueClass, f, fieldsOwner);
			result.put(setting.getFieldName(), setting);
		}
		return result;
	}
	
	private Collection<SettingProperties> processColumnsAnnotation(String[] columns, Map<String, SettingProperties> props) throws SettingDeclarationException
	{
		if (columns == null || columns.length == 0)  //No visible columns defined. Taking all available settings, skipping password fields
			return props.values().stream()
					.filter(p -> p.getInputType() != InputType.PASSWORD)
					.collect(Collectors.toList());
		
		List<SettingProperties> result = new ArrayList<>(columns.length);
		for (String c : columns)
		{
			SettingProperties p = props.get(c);
			if (p == null)
				throw new SettingDeclarationException("Field '"+c+"' is used in @ConnectionSettings.columns annotation but is absent in settings class");
			
			result.add(p);
		}
		return result;
	}
	
	private Collection<SettingProperties> processOrderAnnotation(String[] orderFields, Map<String, SettingProperties> props) throws SettingDeclarationException
	{
		if (orderFields == null || orderFields.length == 0)
			return props.values();
		
		Map<String, SettingProperties> unorderredProps = new LinkedHashMap<>(props);
		List<SettingProperties> result = new ArrayList<>(unorderredProps.size());
		for (String f : orderFields)
		{
			SettingProperties p = unorderredProps.remove(f);
			if (p == null)
				throw new SettingDeclarationException("Field '"+f+"' is used in @ConnectionSettings.order annotation but is absent in settings class");
			
			result.add(p);
		}
		
		result.addAll(unorderredProps.values());  //Fields not listed in @ConnectionSettings.order will appear at the end of list
		return result;
	}
	
	private String getName(ConnectionSetting annotation, Field field)
	{
		String result = annotation.name();
		return StringUtils.isBlank(result) ? StringUtils.capitalize(field.getName()) : result;
	}
	
	private ValueClass getValueClass(Class<?> type) throws SettingDeclarationException
	{
		try
		{
			return ValueClass.byClass(type);
		}
		catch (IllegalArgumentException e)
		{
			throw new SettingDeclarationException(e.getMessage());
		}
	}
	
	private SettingProperties createSettingData(String name, InputType type, ValueClass valueClass, Field field, Class<?> methodsOwner) throws SettingDeclarationException
	{
		return valueClass != ValueClass.ENUM
				? new SettingProperties(name, type, valueClass, field, methodsOwner)
				: new EnumSettingProperties(name, type, field, methodsOwner);
	}
}
