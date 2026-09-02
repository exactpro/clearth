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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static java.lang.String.format;

/**
 * 08 October 2018
 */
public final class KeyNotIn implements KeyCondition
{
	private final Set<String> excludedKeys;

	
	public KeyNotIn(Set<String> excludedKeys)
	{
		this.excludedKeys = excludedKeys;
	}


	@Override
	public IndexNode findOneRandom(IndexNode parentNode)
	{
		String key = selectOneKeyExceptExcluded(parentNode.getSubNodeKeys());
		return (key != null) ? parentNode.getSubNode(key) : null;
	}
	
	private String selectOneKeyExceptExcluded(List<String> keys)
	{
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int keysCount = keys.size();
		int excludedCount = excludedKeys.size();
		
		if ((excludedCount == 1) && (keysCount == 1))
		{
			String key = keys.get(0);
			return (excludedKeys.contains(key)) ? null : key;
		}
		else if (keysCount >= (excludedCount * 2))
		{
			String key;
			do
			{
				key = keys.get(random.nextInt(keysCount));
			}
			while (excludedKeys.contains(key));
			return key;
		}
		else 
		{
			List<String> possibleKeys = null;
			for (String key : keys)
			{
				if (!excludedKeys.contains(key))
				{
					if (possibleKeys == null)
						possibleKeys = new ArrayList<>();
					possibleKeys.add(key);
				}
			}
			return (possibleKeys != null) ? possibleKeys.get(random.nextInt(possibleKeys.size())) : null;
		}
	}

	
	@Override
	public List<IndexNode> findAll(IndexNode parentNode)
	{
		List<IndexNode> nodes = new ArrayList<>();
		for (String key : parentNode.getSubNodeKeys())
		{
			if (!excludedKeys.contains(key))
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
		return format("notIn(%s)", join(excludedKeys));
	}

	/*
	 * Result doesn't depend on keys order.
	 * */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (!(obj instanceof KeyNotIn))
			return false;
		
		return Objects.equals(this.excludedKeys, ((KeyNotIn) obj).excludedKeys);
	}
	
	/*
	 * Result doesn't depend on keys order.
	 * */
	@Override
	public int hashCode()
	{
		return 19 * Objects.hashCode(excludedKeys);
	}
}
