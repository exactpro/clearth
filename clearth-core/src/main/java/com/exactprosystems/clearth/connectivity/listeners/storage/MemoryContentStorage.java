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

package com.exactprosystems.clearth.connectivity.listeners.storage;

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryContentStorage<P, F> implements ContentStorage<P, F>
{
	private static final Logger logger = LoggerFactory.getLogger(MemoryContentStorage.class);
	
	//Storage for parsed messages. Key - message ID in collector
	protected final NavigableMap<Long, P> contentPassed = new ConcurrentSkipListMap<Long, P>();
	//Storage for messages which couldn't be parsed. Key - message ID in collector
	protected final NavigableMap<Long, F> contentFailed = new ConcurrentSkipListMap<Long, F>();
	
	
	@Override
	public void start()
	{
		logger.info("Content will be stored in memory");
	}

	@Override
	public void dispose()
	{
		logger.info("Disposing content storage...");
		clearMemory();
	}
	

	@Override
	public void insertPassed(long id, P item)
	{
		contentPassed.put(id, item);
	}
	
	@Override
	public void insertFailed(long id, F item)
	{
		contentFailed.put(id, item);
	}
	
	
	@Override
	public void removePassed(P item)
	{
		contentPassed.values().remove(item);
	}
	
	@Override
	public void removePassed(long id)
	{
		contentPassed.remove(id);
	}
	
	@Override
	public void removeFailed(F item)
	{
		contentFailed.values().remove(item);
	}

	@Override
	public void removeFailed(long itemId)
	{
		contentFailed.remove(itemId);
	}
	

	@Override
	public void clearMemory()
	{
		clearPassed();
		clearFailed();
	}

	@Override
	public void clearPassed()
	{
		contentPassed.clear();
	}
	
	@Override
	public void clearFailed()
	{
		contentFailed.clear();
	}

	
	@Override
	public Map<Long, P> getContentPassed()
	{
		return contentPassed;
	}
	
	@Override
	public Map<Long, P> getContentPassedAfterId(long id)
	{
		return contentPassed.tailMap(id, false);
	}

	@Override
	public Map<Long, F> getContentFailed()
	{
		return contentFailed;
	}
	
	@Override
	public Map<Long, F> getContentFailedAfterId(long id)
	{
		return contentFailed.tailMap(id, false);
	}
}
