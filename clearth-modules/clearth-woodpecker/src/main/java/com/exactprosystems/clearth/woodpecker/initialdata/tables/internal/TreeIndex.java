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

import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerInitialDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static java.lang.String.join;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * 05 October 2018
 */
public class TreeIndex
{
	private static final Logger log = LoggerFactory.getLogger(TreeIndex.class);

	private final List<String> keysSequence;
	private final IndexNode rootNode;

	
	TreeIndex(List<String> keysSequence, IndexNode rootNode)
	{
		this.keysSequence = keysSequence;
		this.rootNode = rootNode;
	}
	
	
	public IndexNode find(Map<String, KeyCondition> conditions) throws WoodpeckerException
	{
		checkKeysMatch(conditions.keySet());
		
		IndexNode node = findFast(conditions);
		if ((node != null) || isExactQuery(conditions))
			return node;
		
		node = findEverywhere(conditions);
		if (node != null)
			log.warn("Full index scanning was executed to find node for query '{}'", conditions);
		
		return node;
	}
	
	public List<IndexNode> findAll(Map<String, KeyCondition> conditions) throws WoodpeckerException
	{
		checkKeysMatch(conditions.keySet());
		return findAllEverywhere(conditions);
	}
	
	
	private IndexNode findFast(Map<String, KeyCondition> conditions)
	{
		IndexNode node = rootNode;
		for (String key : keysSequence)
		{
			KeyCondition condition = conditions.get(key);
			if (condition == null)
				break;
			
			node = condition.findOneRandom(node);
			if (node == null)
				break;
		}
		return node;
	}
	
	private boolean isExactQuery(Map<String, KeyCondition> conditions)
	{
		if (conditions.size() <= 1)
			return true;
		for (KeyCondition condition : conditions.values())
		{
			if (!condition.isExact())
				return false;
		}
		return true;
	}
	
	private IndexNode findEverywhere(Map<String, KeyCondition> conditions)
	{
		List<IndexNode> foundNodes = findAllEverywhere(conditions);		
		if (isEmpty(foundNodes))
			return null;
		else 
			return foundNodes.get(ThreadLocalRandom.current().nextInt(foundNodes.size()));
	}

	private List<IndexNode> findAllEverywhere(Map<String, KeyCondition> conditions)
	{
		List<IndexNode> foundNodes = null;
		for (int i = 0; i < keysSequence.size(); i++)
		{
			String key = keysSequence.get(i);
			KeyCondition condition = conditions.get(key);
			if (condition == null)
				break;

			if (i == 0)
				foundNodes = condition.findAll(rootNode);
			else
				foundNodes = findAllAtLevel(condition, foundNodes);

			if (foundNodes == null)
				break;
		}
		return foundNodes;
	}
	
	private List<IndexNode> findAllAtLevel(KeyCondition condition, List<IndexNode> parentNodes)
	{
		List<IndexNode> result = new ArrayList<>();
		for (IndexNode parentNode : parentNodes)
		{
			List<IndexNode> nodes = condition.findAll(parentNode);
			if (isNotEmpty(nodes))
				result.addAll(nodes);
		}
		return result;
	}


	private void checkKeysMatch(Collection<String> conditionKeys) throws WoodpeckerException
	{
		int conditionKeysCount = conditionKeys.size();
		int matchedCount = 0;
		for (String key : keysSequence)
		{
			if (conditionKeys.contains(key))
				matchedCount++;
			else
				break;
		}
		if (matchedCount != conditionKeysCount)
			throw new WoodpeckerInitialDataException(format("Keys in condition (%s) don't match to index keys (%s).",
					join(", ", conditionKeys),
					join(" > ", keysSequence)));
	}


	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (!(obj instanceof TreeIndex))
			return false;
		
		TreeIndex index = (TreeIndex)obj;
		return Objects.equals(this.keysSequence, index.keysSequence)
				&& Objects.equals(this.rootNode, index.rootNode);
	}

	@Override
	public String toString()
	{
		LineBuilder lb = new LineBuilder();
		return lb.add("keys: ").append(keysSequence)
				.append(rootNode)
				.toString();
	}
}
