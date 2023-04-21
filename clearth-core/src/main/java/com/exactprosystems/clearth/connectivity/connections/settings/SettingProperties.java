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
import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

public class SettingProperties
{
	private final String name,
			fieldName;
	private final InputType inputType;
	private final ValueTypeInfo valueTypeInfo;
	private final Method getter,
			setter;
	
	public SettingProperties(String name, InputType inputType, ValueTypeInfo valueTypeInfo, Field field, Class<?> methodsOwner) throws SettingDeclarationException
	{
		this.name = name;
		this.fieldName = field.getName();
		this.inputType = inputType;
		this.valueTypeInfo = valueTypeInfo;
		
		String variableName = StringUtils.capitalize(fieldName);
		Class<?> owner = methodsOwner != null ? methodsOwner : field.getDeclaringClass();
		this.getter = findGetter(variableName, field, owner);
		this.setter = findSetter(variableName, field, owner);
	}
	
	public SettingProperties(String name, InputType inputType, ValueTypeInfo valueTypeInfo, Method getter, Method setter)
	{
		this.name = name;
		this.fieldName = name;
		this.inputType = inputType;
		this.valueTypeInfo = valueTypeInfo;
		this.getter = getter;
		this.setter = setter;
	}
	
	
	public String getName()
	{
		return name;
	}
	
	public String getFieldName()
	{
		return fieldName;
	}
	
	public InputType getInputType()
	{
		return inputType;
	}
	
	public ValueTypeInfo getValueTypeInfo()
	{
		return valueTypeInfo;
	}
	
	public Method getGetter()
	{
		return getter;
	}
	
	public Method getSetter()
	{
		return setter;
	}
	
	
	private Method findGetter(String name, Field field, Class<?> owner) throws SettingDeclarationException
	{
		String methodName = field.getType() == boolean.class || field.getType() == Boolean.class ? "is"+name : "get"+name;
		
		try
		{
			return owner.getMethod(methodName);
		}
		catch (NoSuchMethodException e)
		{
			throw new SettingDeclarationException("Could not obtain getter for field '"+field.getName()+"'", e);
		}
	}
	
	private Method findSetter(String name, Field field, Class<?> owner) throws SettingDeclarationException
	{
		String methodName = "set"+name;
		
		try
		{
			return owner.getMethod(methodName, field.getType());
		}
		catch (NoSuchMethodException e)
		{
			throw new SettingDeclarationException("Could not obtain setter for field '"+field.getName()+"'", e);
		}
	}
}
