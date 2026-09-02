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

package com.exactprosystems.clearth.woodpecker.configuration.blacklist;

import java.util.*;
import java.util.function.Predicate;

public class BlackList
{
	private final String name;
	private final Map<String, EntityBlackList> blackListByEntityType = new HashMap<>();

	
	public BlackList(String name)
	{
		this.name = name;
	}


	public String getName()
	{
		return name;
	}
	

	public boolean isBlocked(String entityType, String entityId)
	{
		EntityBlackList bl = blackListByEntityType.get(entityType);
		return ((bl != null) && bl.isBlocked(entityId));
	}
	
	
	void addId(String entityType, String entityId)
	{
		EntityBlackList bl = getOrCreateEntityBlackList(entityType);
		bl.addId(entityId);
	}
	
	void addIdPredicate(String entityType, Predicate<String> predicate)
	{
		EntityBlackList bl = getOrCreateEntityBlackList(entityType);
		bl.addPredicate(predicate);
	}
	
	private EntityBlackList getOrCreateEntityBlackList(String entityType)
	{
		EntityBlackList bl = blackListByEntityType.get(entityType);
		if (bl == null)
		{
			bl = new EntityBlackList();
			blackListByEntityType.put(entityType, bl);
		}
		return bl;
	}
	
	
	private static class EntityBlackList
	{
		private Set<String> blockedIds;
		private List<Predicate<String>> blockingIdPredicates;
		
		
		public boolean isBlocked(String id)
		{
			return isInBlockedIds(id) || isBlockedByPredicate(id);
		}
		
		private boolean isInBlockedIds(String id)
		{
			return (blockedIds != null) && blockedIds.contains(id);
		}
		
		private boolean isBlockedByPredicate(String id)
		{
			if (blockingIdPredicates == null)
				return false;
			for (Predicate<String> predicate : blockingIdPredicates)
			{
				if (predicate.test(id))
					return true;
			}
			return false;
		}
		
		
		private void addId(String id)
		{
			if (blockedIds == null)
				blockedIds = new HashSet<>();
			blockedIds.add(id);
		}
		
		private void addPredicate(Predicate<String> predicate)
		{
			if (blockingIdPredicates == null)
				blockingIdPredicates = new ArrayList<>();
			blockingIdPredicates.add(predicate);
		}
	}
}
