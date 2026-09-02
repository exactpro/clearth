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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.csv;

import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvRowFilter;
import com.exactprosystems.clearth.woodpecker.configuration.blacklist.BlackList;
import com.exactprosystems.clearth.woodpecker.configuration.start.CsvTableDesc;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerInitialDataException;
import com.exactprosystems.clearth.woodpecker.initialdata.tables.internal.TableDataLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static com.exactprosystems.clearth.utils.tabledata.RowsListFactories.arrayListFactory;
import static com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerInitialDataException.fromIOException;
import static com.exactprosystems.clearth.woodpecker.utils.WoodpeckerFileUtils.newBufferedReaderIgnoresBom;

public class CsvTableDataLoader extends TableDataLoader
{
	private final String tableName;
	private final Path filePath;
	private final BlackList blackList;

	
	public CsvTableDataLoader(CsvTableDesc description, Path filePath, BlackList blackList)
	{
		super(description);
		this.tableName = description.getName();
		this.filePath = filePath;
		this.blackList = blackList;
	}


	@Override
	protected StringTableData loadData() throws WoodpeckerException
	{
		try (CsvDataReader reader = new CsvDataReader(newBufferedReaderIgnoresBom(filePath)))
		{
			reader.setRowsListFactory(arrayListFactory());
			
			BlackListRowFilter filter = (blackList != null) ? new BlackListRowFilter(blackList) : null;
			if (filter != null)
				reader.setCsvRowFilter(filter);

			StringTableData tableData = reader.readAllData();
			checkNotEmpty(tableData, filter);
			return tableData;
		}
		catch (IOException e)
		{
			throw fromIOException(e, "Error while loading table '%s' from file '%s'", tableName, filePath);
		}
	}
	
	
	private void checkNotEmpty(StringTableData tableData, BlackListRowFilter filter) throws WoodpeckerException
	{
		if (tableData.isEmpty())
		{
			if ((filter != null) && filter.isInvoked())
				throw new WoodpeckerInitialDataException("Unable to load CSV table '%s' from file '%s': " +
						"all rows are excluded by Black List '%s'.", tableName, filePath, blackList.getName());
			else
				throw new WoodpeckerInitialDataException("Unable to load CSV table '%s' from file '%s': file is empty.",
						tableName, filePath);
		}
	}
	
	
	private static class BlackListRowFilter implements CsvRowFilter
	{
		private final BlackList blackList;
		private boolean invoked;

		private BlackListRowFilter(BlackList blackList)
		{
			this.blackList = blackList;
		}

		@Override
		public boolean filter(Map<String, String> record) throws IOException
		{
			invoked = true;
			for (Map.Entry<String, String> entry : record.entrySet())
			{
				if (blackList.isBlocked(entry.getKey(), entry.getValue()))
					return false;
			}
			return true;
		}

		public boolean isInvoked()
		{
			return invoked;
		}
	}
}
