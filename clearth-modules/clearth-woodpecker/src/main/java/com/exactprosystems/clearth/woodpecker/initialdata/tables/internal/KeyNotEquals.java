/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.internal;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;

public final class KeyNotEquals implements KeyCondition
{
	private final String excludedKey;

	
	public KeyNotEquals(String excludedKey)
	{
		this.excludedKey = excludedKey;
	}

	
	@Override
	public IndexNode findOneRandom(IndexNode parentNode)
	{
		String key = selectOneKeyExceptExcluded(parentNode.getSubNodeKeys());
		return (key != null) ? parentNode.getSubNode(key) : null;
	}

	private String selectOneKeyExceptExcluded(List<String> keys)
	{
		if (keys.size() == 1)
		{
			String key = keys.get(0);
			return StringUtils.equals(key, excludedKey) ? null : key;
		}
		else
		{
			ThreadLocalRandom random = ThreadLocalRandom.current();
			String key;
			do
			{
				key = keys.get(random.nextInt(keys.size()));
			}
			while (StringUtils.equals(key, excludedKey));
			return key;
		}
	}
	

	@Override
	public List<IndexNode> findAll(IndexNode parentNode)
	{
		List<IndexNode> nodes = new ArrayList<>();
		for (String key : parentNode.getSubNodeKeys())
		{
			if (!StringUtils.equals(excludedKey, key))
				nodes.add(parentNode.getSubNode(key));
		}
		return nodes;
	}


	@Override
	public boolean isExact()
	{
		return false;
	}
	

	@Override
	public String toString()
	{
		return format("notEq('%s')", excludedKey);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (!(obj instanceof KeyNotEquals))
			return false;
		
		return Objects.equals(this.excludedKey, ((KeyNotEquals) obj).excludedKey);
	}

	@Override
	public int hashCode()
	{
		return 13 * Objects.hashCode(this.excludedKey);
	}
}
