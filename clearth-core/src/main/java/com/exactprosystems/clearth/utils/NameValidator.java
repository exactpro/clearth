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

package com.exactprosystems.clearth.utils;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NameValidator
{
	private static final Set<String> FORBIDDEN_CHARACTERS = new HashSet<>(
			Arrays.asList("\\", "/", ":", "*", "?", "\"", "<", ">", "|"));
	private static final String FORBIDDEN_CHARS_STRING;
	
	static
	{
		StringBuilder strB = new StringBuilder();
		for (String character : FORBIDDEN_CHARACTERS)
		{
			strB.append(character).append(" ");
		}
	
		FORBIDDEN_CHARS_STRING = strB.toString().trim();
	}
	
	private NameValidator()
	{
	}
	
	public static void validate(String name) throws SettingsException
	{
		if (StringUtils.isEmpty(name))
			throw new SettingsException("Name must not be empty");
		
		for (String character : FORBIDDEN_CHARACTERS)
		{
			if (name.contains(character))
				throw new SettingsException("Name must not contain the following characters: " + FORBIDDEN_CHARS_STRING);
		}
	}
}
