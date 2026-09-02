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

package com.exactprosystems.clearth.woodpecker.initialdata;

import com.exactprosystems.clearth.woodpecker.configuration.blacklist.BlackList;
import com.exactprosystems.clearth.woodpecker.configuration.blacklist.BlackListLoader;
import com.exactprosystems.clearth.woodpecker.configuration.start.*;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerInitialDataException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.initialdata.collections.InitialDataCollection;
import com.exactprosystems.clearth.woodpecker.initialdata.collections.InitialDataQueue;
import com.exactprosystems.clearth.woodpecker.initialdata.collections.InitialDataSet;
import com.exactprosystems.clearth.woodpecker.initialdata.queries.InitialDataQuery;
import com.exactprosystems.clearth.woodpecker.initialdata.queries.InitialDataQueryFactory;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.InitialDataTable;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.UpdateTableTask;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TableDataLoader;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TableDataLoaderFactory;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TreeIndexBuilder;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.utils.Utils.nvl;
import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerInitialDataException.whenBlackListNotFound;
import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerConfigUtils.findConfigFile;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class InitialDataLoader
{
	private final String daemonName;
	private final Map<String, TableDataLoaderFactory> tableDataLoaderFactoriesByType;
	private final Map<String, InitialDataQueryFactory> queryFactoriesByType;

	
	public InitialDataLoader(String daemonName,
	                         Map<String, TableDataLoaderFactory> tableDataLoaderFactoriesByType,
	                         Map<String, InitialDataQueryFactory> queryFactoriesByType)
	{
		this.daemonName = daemonName;
		this.tableDataLoaderFactoriesByType = tableDataLoaderFactoriesByType;
		this.queryFactoriesByType = queryFactoriesByType;
	}
	
	
	@SuppressWarnings({"unused", "RedundantThrows"})
	protected void loadCustomInitialData(InitialData initialData, InitialDataBlockDesc blockDesc, Path configsDirPath,
	                                     DaemonContext context) throws WoodpeckerException
	{ /*Override to load custom initial data structures.*/ }


	public InitialData load(InitialDataBlockDesc blockDesc, Path configsDirPath, DaemonContext context) throws WoodpeckerException
	{
		InitialData initialData = woodpecker().getObjectsFactory().createInitialData();
		try
		{
			createCollections(blockDesc, initialData);
			
			Map<String, BlackList> blacklists = loadBlacklists(blockDesc, configsDirPath);
			
			TablesBlockDesc tablesBlockDesc = blockDesc.getTablesBlock();
			if (tablesBlockDesc != null)
				loadTables(initialData, tablesBlockDesc, context, configsDirPath, blacklists);
			
			QueriesBlockDesc queriesBlockDesc = blockDesc.getQueriesBlock();
			if (queriesBlockDesc != null)
				loadQueries(initialData, queriesBlockDesc, configsDirPath, context);
			
			loadCustomInitialData(initialData, blockDesc, configsDirPath, context);
			
			return initialData;
		}
		catch (Exception e)
		{
			initialData.dispose();
			throw e;
		}
	}
	
	
	private void createCollections(InitialDataBlockDesc blockDesc, InitialData initialData) throws WoodpeckerException
	{
		CollectionsBlockDesc collectionsDesc = blockDesc.getCollectionsBlock();
		if (collectionsDesc != null)
		{
			createQueues(collectionsDesc.getQueues(), initialData);
			createSets(collectionsDesc.getSets(), initialData);
		}
	}
	
	private void createSets(List<InitialDataCollectionDesc> descriptions, InitialData initialData) throws WoodpeckerException
	{
		if (isNotEmpty(descriptions))
		{
			for (InitialDataCollectionDesc description : descriptions)
			{
				int capacity = nvl(description.getCapacity(), InitialDataCollection.DEFAULT_CAPACITY);
				initialData.addCollection(description.getName(), new InitialDataSet(capacity));
			}
		}
	}
	
	private void createQueues(List<InitialDataQueueDesc> descriptions, InitialData initialData) throws WoodpeckerException
	{
		if (isNotEmpty(descriptions))
		{
			for (InitialDataQueueDesc description : descriptions)
			{
				int capacity = nvl(description.getCapacity(), InitialDataCollection.DEFAULT_CAPACITY);
				initialData.addCollection(description.getName(), new InitialDataQueue(capacity, description.getDelaySec()));
			}
		}
	}


	private Map<String, BlackList> loadBlacklists(InitialDataBlockDesc blockDesc, Path configsDirPath) throws WoodpeckerException
	{
		BlacklistsBlockDesc blsDesc = blockDesc.getBlacklistsBlock();
		if (blsDesc != null)
		{
			Map<String, BlackList> blackLists = new LinkedHashMap<>();
			BlackListLoader loader = new BlackListLoader();
			for (BlacklistDesc blDesc : blsDesc.getBlacklists())
			{
				String name = blDesc.getName();
				Path path = findConfigFile(blDesc.getFile(), configsDirPath);
				BlackList blackList = loader.load(name, path);
				blackLists.put(name, blackList);
			}
			return blackLists;
		}
		else 
			return emptyMap();
	}
	
	
	private void loadTables(InitialData initialData,
	                        TablesBlockDesc tablesBlockDesc,
	                        DaemonContext context,
	                        Path configsDirPath,
	                        Map<String, BlackList> blacklists) throws WoodpeckerException
	{
		for (InitialDataTableDesc tableDesc : tablesBlockDesc.getTableDescs())
		{
			InitialDataTable table = loadTable(initialData, tableDesc, context, configsDirPath, blacklists);
			initialData.addTable(tableDesc.getName(), table);
		}
	}
	
	private InitialDataTable loadTable(InitialData initialData,
	                                   InitialDataTableDesc tableDesc,
	                                   DaemonContext context,
	                                   Path configsDirPath,
	                                   Map<String, BlackList> blacklists) throws WoodpeckerException
	{
		String tableName = tableDesc.getName();

		String blackListName = tableDesc.getBlacklist();
		BlackList blackList = isNotBlank(blackListName) ? findBlackList(blackListName, tableName, blacklists) : null;

		String type = tableDesc.getTableType();
		TableDataLoaderFactory dataLoaderFactory = tableDataLoaderFactoriesByType.get(type);
		if (dataLoaderFactory == null)
			throw new WoodpeckerInitialDataException("Table '%s' has unsupported type '%s'.", tableName, type);

		TableDataLoader dataLoader = dataLoaderFactory.create(tableDesc, context, configsDirPath, blackList);
		List<String> indexKeys = tableDesc.getIndexKeys();
		TreeIndexBuilder indexBuilder = isNotEmpty(indexKeys) ? new TreeIndexBuilder(tableName, indexKeys) : null;

		InitialDataTable table = loadInitialDataTable(tableName, daemonName, dataLoader, indexBuilder);
		
		if (tableDesc.getUpdatePeriod() > 0)
		{
			UpdateTableTask updateTableTask = new UpdateTableTask(table);
			initialData.scheduleUpdates(updateTableTask, 
					tableDesc.getUpdatePeriod(), tableDesc.getUpdatePeriodUnit());
		}
		
		return table;
	}

	protected InitialDataTable loadInitialDataTable(String tableName, String daemonName, TableDataLoader dataLoader, TreeIndexBuilder indexBuilder) throws WoodpeckerException
	{
		return InitialDataTable.load(tableName, daemonName, dataLoader, indexBuilder);
	}
	
	private BlackList findBlackList(String blackListName, String tableName, 
	                                Map<String, BlackList> blacklists) throws WoodpeckerException
	{
		BlackList blackList = blacklists.get(blackListName);
		if (blackList != null)
			return blackList;
		else 
			throw whenBlackListNotFound(blackListName, tableName, blacklists.keySet());
	}


	private void loadQueries(InitialData initialData,
	                         QueriesBlockDesc queriesBlockDesc,
	                         Path configsDirPath,
	                         DaemonContext context) throws WoodpeckerException
	{
		for (InitialDataQueryDesc queryDesc : queriesBlockDesc.getQueryDescs())
		{
			String type = queryDesc.getQueryType();
			InitialDataQueryFactory queryFactory = queryFactoriesByType.get(type);

			InitialDataQuery query = queryFactory.create(queryDesc, configsDirPath, context);
			
			initialData.addQuery(queryDesc.getName(), query);
		}
	}
}
