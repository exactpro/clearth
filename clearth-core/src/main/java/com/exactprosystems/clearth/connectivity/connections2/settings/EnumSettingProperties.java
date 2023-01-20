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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EnumSettingProperties extends SettingProperties
{
	private final Map<String, Object> values;
	
	public EnumSettingProperties(String name, InputType inputType, Field field, Class<?> methodsOwner)
			throws SettingDeclarationException
	{
		super(name, inputType, ValueClass.ENUM, field, methodsOwner);
		values = Collections.unmodifiableMap(buildValues(field));
	}
	
	
	public Set<String> getValues()
	{
		return values.keySet();
	}
	
	
	protected Map<String, Object> getValuesMap()
	{
		return values;
	}
	
	
	private Map<String, Object> buildValues(Field field)
	{
		Object[] values = field.getType().getEnumConstants();
		if (values.length == 0)
			return Collections.emptyMap();
		
		Map<String, Object> result = new LinkedHashMap<>(values.length);
		for (Object v : values)
			result.put(v.toString(), v);
		return result;
	}
}
