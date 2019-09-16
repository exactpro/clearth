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

package com.exactprosystems.clearth.messages;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class RgKeyFieldNames
{
	private final Map<String, Set<String>> keyNamesInRgs;
	
	public RgKeyFieldNames()
	{
		keyNamesInRgs = new LinkedHashMap<>();
	}
	
	
	public Set<String> getRgKeyFields(String rgType)
	{
		return keyNamesInRgs.get(rgType);
	}
	
	public void addRgKeyFields(String rgType, Set<String> keyFields)
	{
		keyNamesInRgs.put(rgType, keyFields);
	}
	
	public void addRgKeyField(String rgType, String keyField)
	{
		Set<String> rgKeys = getRgKeyFields(rgType);
		if (rgKeys == null)
		{
			rgKeys = new HashSet<String>();
			addRgKeyFields(rgType, rgKeys);
		}
		rgKeys.add(keyField);
	}
	
	public Set<Entry<String, Set<String>>> getRgKeyFields()
	{
		return keyNamesInRgs.entrySet();
	}
	
	
	public boolean hasKeys()
	{
		return !keyNamesInRgs.isEmpty();
	}
}
