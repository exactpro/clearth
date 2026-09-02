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

import com.exactprosystems.clearth.utils.LineBuilder;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.repeat;

public abstract class IndexNode
{
	private Map<String, IndexNode> subNodes;
	private volatile List<String> subNodeKeys;
	
	
	public abstract int getRowNum(int index);
	
	public abstract int getCountOfReferencedRows();
	
	protected abstract String toString(int level);
	
	
	public void addSubNode(String key, IndexNode node)
	{
		if (subNodes == null)
			subNodes = new HashMap<>();
		subNodes.put(key, node);

		if (subNodeKeys == null)
			subNodeKeys = new ArrayList<>();
		subNodeKeys.add(key);
	}
	
	public IndexNode getSubNode(String key)
	{
		return (subNodes != null) ? subNodes.get(key) : null;
	}

	public Set<String> getSubNodeKeySet()
	{
		return (subNodes != null) ? subNodes.keySet() : emptySet();
	}
	
	public List<String> getSubNodeKeys()
	{
		return (subNodeKeys != null) ? subNodeKeys : emptyList();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (!(obj instanceof IndexNode))
			return false;
		
		IndexNode node = (IndexNode)obj;
		return Objects.equals(this.subNodes, node.subNodes);
	}
	
	
	protected String toStringSubNodes(int level)
	{
		if (subNodes == null)
			return "";
		LineBuilder lb = new LineBuilder();
		for (Map.Entry<String, IndexNode> e : subNodes.entrySet())
		{
			String nodeKey = e.getKey();
			IndexNode node = e.getValue();
			
			lb.add(repeat("\t", level))
					.add(nodeKey).append(':')
					.add(node.toString(level + 1));
		}
		return lb.toString();
	}
}
