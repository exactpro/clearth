/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.web.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ancestor for classes that implement caching of data
 * @param <A> class of cached data
 * @param <B> class of processed data
 */
public abstract class ProcessedCache<A, B>
{
	private List<A> cachedData = null;
	private List<B> processedData = null;
	
	/**
	 * Refreshes the cache if data in given list differs from cached data
	 * @param currentData list to compare the cache with
	 * @return list of processed data
	 */
	public List<B> refreshIfNeeded(List<A> currentData)
	{
		if (isNeedRefresh(currentData))
			refresh(currentData);
		return getProcessedData();
	}
	/**
	 * @return list of cached data
	 */
	public List<A> getCachedData()
	{
		return cachedData;
	}
	
	/**
	 * @return list of processed data
	 */
	public List<B> getProcessedData()
	{
		return processedData;
	}
	
	/**
	 * Transforms given data into processed data
	 * @param toStore unprocessed data list
	 * @return processed data list
	 */
	protected abstract List<B> processData(List<A> toStore);
	
	/**
	 * Checks if the data in given list differs from cached data
	 * @param currentData list of unprocessed data
	 * @return true if cached data differs from current data, false if it's not
	 */
	protected boolean isNeedRefresh(List<A> currentData)
	{
		return cachedData == null || !cachedData.equals(currentData);
	}
	
	private void refresh(List<A> currentData)
	{
		List<A> toStore = new ArrayList<>(currentData);
		cachedData = Collections.unmodifiableList(toStore);
		processedData = processData(cachedData);
	}
}