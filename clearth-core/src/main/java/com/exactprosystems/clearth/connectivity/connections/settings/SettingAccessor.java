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

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;

public class SettingAccessor
{
	private final SettingProperties properties;
	private final Object owner;
	private boolean applyChange;
	
	public SettingAccessor(SettingProperties properties, Object owner)
	{
		this.properties = properties;
		this.owner = owner;
		this.applyChange = false;
	}
	
	
	public static void copyValue(SettingProperties properties, Object from, Object to) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Object value = properties.getGetter().invoke(from);
		properties.getSetter().invoke(to, value);
	}
	
	
	public SettingProperties getProperties()
	{
		return properties;
	}
	
	
	public String getStringValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Object result = getValue();
		return result != null ? result.toString() : null;
	}
	
	public void setStringValue(String value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		setValue(value);
	}
	
	
	public String getPasswordValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return getStringValue();
	}
	
	public void setPasswordValue(String password) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		if (!StringUtils.isEmpty(password))
			setStringValue(password);
	}
	
	
	public int getIntValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return (Integer)getValue();
	}
	
	public void setIntValue(int value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		setValue((Integer)value);
	}
	
	
	public long getLongValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return (Long)getValue();
	}
	
	public void setLongValue(long value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		setValue((Long)value);
	}
	
	
	public boolean isBooleanValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return (Boolean)getValue();
	}
	
	public void setBooleanValue(boolean value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		setValue((Boolean)value);
	}
	
	
	public boolean isApplyChange()
	{
		return applyChange;
	}
	
	public void setApplyChange(boolean applyChange)
	{
		this.applyChange = applyChange;
	}
	
	
	protected Object getValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return properties.getGetter().invoke(owner);
	}
	
	protected void setValue(Object value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		properties.getSetter().invoke(owner, value);
	}
}
