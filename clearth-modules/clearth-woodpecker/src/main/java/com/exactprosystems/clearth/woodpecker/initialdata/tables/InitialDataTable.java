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

package com.exactprosystems.clearth.woodpecker.initialdata.tables;

import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerInitialDataException;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.*;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;
import static java.lang.String.format;

public class InitialDataTable
{
	private static final Logger log = LoggerFactory.getLogger(InitialDataTable.class);

	private final String tableName;
	private final String daemonName;

	private final TableDataLoader dataLoader;
	private final TreeIndexBuilder indexBuilder;

	private final WoodpeckerNotifications notifications;

	private volatile Table table;
	
	
	public Table getInternalTable()
	{
		return table;
	}
	
	public void updateInternalTable()
	{
		log.info("Trying to update table '{}'...", tableName);
		try
		{
			table = loadInternalTable(tableName, dataLoader, indexBuilder);
		}
		catch (Exception e)
		{
			notifications.addWarning(daemonName, log, e,
					format("Unable to update table '%s'. Old data will be used.", tableName));
		}
	}
	
	
	public static InitialDataTable load(String tableName,
	                                    String daemonName,
	                                    TableDataLoader dataLoader,
	                                    TreeIndexBuilder indexBuilder) throws WoodpeckerException
	{
		Table internal = loadInternalTable(tableName, dataLoader, indexBuilder);
		return new InitialDataTable(tableName, daemonName, dataLoader, indexBuilder, internal);
	}

	private static Table loadInternalTable(String tableName,
	                                       TableDataLoader dataLoader,
	                                       TreeIndexBuilder indexBuilder) throws WoodpeckerException
	{
		StringTableData tableData = dataLoader.load();
		if (tableData.size() == 0)
			throw new WoodpeckerInitialDataException(format("Table '%s' is empty.", tableName));

		TreeIndex index = (indexBuilder != null) ? indexBuilder.build(tableData) : null;
		
		return new Table(tableName, tableData, index);
	}
	
	protected InitialDataTable(String tableName,
	                         String daemonName,
	                         TableDataLoader dataLoader,
	                         TreeIndexBuilder indexBuilder,
	                         Table table)
	{
		this.tableName = tableName;
		this.daemonName = daemonName;
		this.dataLoader = dataLoader;
		this.indexBuilder = indexBuilder;
		this.table = table;
		this.notifications = woodpecker().getNotifications();
	}
}
