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

import java.util.Arrays;
import java.util.List;

public class ValueTypeInfo
{
	public static final ValueTypeInfo STRING = new ValueTypeInfo(String.class, ValueType.STRING),
			PRIMITIVE_INT = new ValueTypeInfo(int.class, ValueType.INT),
			OBJECT_INT = new ValueTypeInfo(Integer.class, ValueType.INT),
			PRIMITIVE_LONG = new ValueTypeInfo(long.class, ValueType.LONG),
			OBJECT_LONG = new ValueTypeInfo(Long.class, ValueType.LONG),
			PRIMITIVE_BOOLEAN = new ValueTypeInfo(boolean.class, ValueType.BOOLEAN),
			OBJECT_BOOLEAN = new ValueTypeInfo(Boolean.class, ValueType.BOOLEAN),
			ENUM = new ValueTypeInfo(Enum.class, ValueType.ENUM);
	
	private static final List<ValueTypeInfo> SUPPORTED_TYPES = Arrays.asList(
			STRING,
			PRIMITIVE_INT,
			OBJECT_INT,
			PRIMITIVE_LONG,
			OBJECT_LONG,
			PRIMITIVE_BOOLEAN,
			OBJECT_BOOLEAN,
			ENUM);
	
	private final Class<?> valueClass;
	private final ValueType type;
	
	private ValueTypeInfo(Class<?> valueClass, ValueType type)
	{
		this.valueClass = valueClass;
		this.type = type;
	}
	
	public Class<?> getValueClass()
	{
		return valueClass;
	}
	
	public ValueType getType()
	{
		return type;
	}
	
	
	public static ValueTypeInfo byClass(Class<?> clazz) throws IllegalArgumentException
	{
		for (ValueTypeInfo sc : SUPPORTED_TYPES)
		{
			if (sc.getValueClass().isAssignableFrom(clazz))
				return sc;
		}
		return new ValueTypeInfo(clazz, ValueType.SPECIAL);
	}
}
