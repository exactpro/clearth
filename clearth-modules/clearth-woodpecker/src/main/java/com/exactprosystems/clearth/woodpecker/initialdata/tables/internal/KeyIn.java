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

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static java.lang.String.format;

/**
 * 08 October 2018
 */
public final class KeyIn implements KeyCondition
{
	private final List<String> keys;
	
	// For equals and hashCode
	private final Set<String> keysSet;

	
	public KeyIn(List<String> keys)
	{
		this.keys = keys;
		this.keysSet = new HashSet<>(keys);
	}

	
	@Override
	public IndexNode findOneRandom(IndexNode parentNode)
	{
		String key = selectOnePresentKey(parentNode);
		return (key != null) ? parentNode.getSubNode(key) : null;
	}
	
	private String selectOnePresentKey(IndexNode parentNode)
	{
		ThreadLocalRandom random = ThreadLocalRandom.current();
		Set<String> parentNodeKeys = parentNode.getSubNodeKeySet();
		
		String randomKey = keys.get(random.nextInt(keys.size()));
		if (parentNodeKeys.contains(randomKey))
			return randomKey;
		
		List<String> presentKeys = null;
		for (String key : keys)
		{
			if (parentNodeKeys.contains(key))
			{
				if (presentKeys == null)
					presentKeys = new ArrayList<>();
				presentKeys.add(key);
			}
		}
		return (presentKeys != null) ? presentKeys.get(random.nextInt(presentKeys.size())) : null;
	}
	

	@Override
	public List<IndexNode> findAll(IndexNode parentNode)
	{
		List<IndexNode> nodes = new ArrayList<>();
		for (String keyValue : keys)
		{
			IndexNode node = parentNode.getSubNode(keyValue);
			if (node != null)
				nodes.add(node);
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
		return format("in(%s)", join(keys));
	}

	/*
	 * Result doesn't depend on keys order.
	 * */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (!(obj instanceof KeyIn))
			return false;
		
		return Objects.equals(this.keysSet, ((KeyIn) obj).keysSet);
	}

	/*
	 * Result doesn't depend on keys order.
	 * */
	@Override
	public int hashCode()
	{
		return 17 * Objects.hashCode(keysSet);
	}
}
