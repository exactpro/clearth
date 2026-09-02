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

import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * 08 October 2018
 */
public final class KeyEquals implements KeyCondition
{
	private final String key;

	public KeyEquals(String key)
	{
		this.key = key;
	}

	@Override
	public IndexNode findOneRandom(IndexNode parentNode)
	{
		return parentNode.getSubNode(key);
	}

	@Override
	public List<IndexNode> findAll(IndexNode parentNode)
	{
		IndexNode node = parentNode.getSubNode(key);
		return (node != null) ? singletonList(node) : emptyList();
	}

	@Override
	public boolean isExact()
	{
		return true;
	}

	@Override
	public String toString()
	{
		return format("eq('%s')", key);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (!(obj instanceof KeyEquals))
			return false;
		
		return Objects.equals(this.key, ((KeyEquals) obj).key);
	}

	@Override
	public int hashCode()
	{
		return 11 * Objects.hashCode(this.key);
	}
}
