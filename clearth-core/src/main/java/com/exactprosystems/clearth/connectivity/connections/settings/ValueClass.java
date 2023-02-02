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
import java.util.Collection;
import java.util.LinkedHashSet;

public enum ValueClass
{
	STRING(String.class),
	INT(int.class, Integer.class),
	LONG(long.class, Long.class),
	BOOLEAN(boolean.class, Boolean.class),
	ENUM(Enum.class);
	
	private static final Collection<Class<?>> supportedClasses;
	
	private final Class<?>[] classes;
	
	private ValueClass(Class<?>... classes)
	{
		this.classes = classes;
	}
	
	static
	{
		ValueClass[] values = values();
		supportedClasses = new LinkedHashSet<>(values.length);
		for (ValueClass sc : values)
			supportedClasses.addAll(Arrays.asList(sc.getClasses()));
	}
	
	public Class<?>[] getClasses()
	{
		return classes;
	}
	
	public boolean supportsClass(Class<?> clazz)
	{
		for (Class<?> c : getClasses())
			if (c.isAssignableFrom(clazz))
				return true;
		return false;
	}
	
	
	public static ValueClass byClass(Class<?> clazz) throws IllegalArgumentException
	{
		for (ValueClass sc : values())
		{
			if (sc.supportsClass(clazz))
				return sc;
		}
		throw new IllegalArgumentException("Unsupported value class: "+clazz+". Only the following are supported: "+supportedClasses);
	}
}
