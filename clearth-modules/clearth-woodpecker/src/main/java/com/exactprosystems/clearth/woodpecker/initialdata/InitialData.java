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

package com.exactprosystems.clearth.woodpecker.initialdata;

import com.exactprosystems.clearth.woodpecker.initialdata.collections.InitialDataCollection;
import com.exactprosystems.clearth.woodpecker.initialdata.queries.InitialDataQuery;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.InitialDataTable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class InitialData
{
	private final Map<String, InitialDataTable> tables = new LinkedHashMap<>();
	private final Map<String, InitialDataQuery> queries = new LinkedHashMap<>();
	private final Map<String, InitialDataCollection> collections = new LinkedHashMap<>();
	
	private volatile ScheduledExecutorService updatesService;


	public void addTable(String name, InitialDataTable initialData)
	{
		tables.put(name, initialData);
	}

	public InitialDataTable getTable(String name)
	{
		return tables.get(name);
	}


	public boolean containsTable(String name)
	{
		return tables.containsKey(name);
	}


	public Collection<String> getTableNames()
	{
		return tables.keySet();
	}
	
	
	public void addQuery(String name, InitialDataQuery query)
	{
		queries.put(name, query);
	}
	
	public InitialDataQuery getQuery(String name)
	{
		return queries.get(name);
	}
	
	
	public boolean containsQuery(String name)
	{
		return queries.containsKey(name);
	}
	
	
	public Collection<String> getQueryNames()
	{
		return queries.keySet();
	}
	
	
	public void addCollection(String name, InitialDataCollection collection)
	{
		collections.put(name, collection);
	}
	
	public InitialDataCollection getCollection(String name)
	{
		return collections.get(name);
	}
	
	
	public boolean containsCollection(String name)
	{
		return collections.containsKey(name);
	}
	
	
	public Collection<String> getCollectionNames()
	{
		return collections.keySet();
	}
	
	
	public void scheduleUpdates(Runnable task, int updatesPeriod, TimeUnit periodUnit)
	{
		if (updatesService == null)
			updatesService = createUpdatesService();
		updatesService.scheduleAtFixedRate(task, updatesPeriod, updatesPeriod, periodUnit);
	}
	
	private ScheduledExecutorService createUpdatesService()
	{
		return newSingleThreadScheduledExecutor(new ThreadFactory()
		{
			private final AtomicInteger counter = new AtomicInteger();
			
			@Override
			public Thread newThread(Runnable r)
			{
				return new Thread(r, format("initial-data-updater-%d", counter.incrementAndGet()));
			}
		});
	}
	
	
	public void dispose()
	{
		tables.clear();
		queries.clear();
		collections.clear();
		
		disposeCustomInitialData();
		
		if (updatesService != null)
			updatesService.shutdown();
	}
	
	protected void disposeCustomInitialData() { /*Override to dispose custom data structures*/ }
}
