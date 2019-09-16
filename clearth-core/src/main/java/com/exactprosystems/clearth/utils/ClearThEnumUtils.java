/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

import java.util.ArrayList;
import java.util.List;

public class ClearThEnumUtils 
{
	public static <E extends Enum<E>> List<String> enumToTextValues(Class<E> enumClass)
	{
		E[] values = enumClass.getEnumConstants();
		if (values == null)
			return emptyList();

		List<String> textValues = new ArrayList<String>();
		for (E value: values)
			textValues.add(value.name());
		return textValues;
	}
	
	public static <E extends Enum<E>> E valueOfIgnoreCase(Class<E> enumClass, String text)
	{
		E[] values = enumClass.getEnumConstants();
		if (values != null)
		{
			for (E value: values)
			{
				if (equalsIgnoreCase(text, value.name()))
					return value;
			}
		}
		return null;
	}
}
