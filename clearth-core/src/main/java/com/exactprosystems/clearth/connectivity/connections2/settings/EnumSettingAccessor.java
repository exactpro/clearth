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

package com.exactprosystems.clearth.connectivity.connections2.settings;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class EnumSettingAccessor extends SettingAccessor
{
	private final Map<String, Object> values;
	
	public EnumSettingAccessor(EnumSettingProperties properties, Object owner)
	{
		super(properties, owner);
		this.values = properties.getValuesMap();
	}
	
	
	public String getEnumValue() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		return getStringValue();
	}
	
	public void setEnumValue(String value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		Object realValue = values.get(value);
		setValue(realValue);
	}
}
