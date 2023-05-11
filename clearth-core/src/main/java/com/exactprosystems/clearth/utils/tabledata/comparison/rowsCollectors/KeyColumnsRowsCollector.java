/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.utils.tabledata.comparison.rowsCollectors;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.primarykeys.PrimaryKey;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Class that collects rows to some storage by their indexes (primary keys).
 * Could be very useful for detecting duplicated rows.
 * Rows being collected to the storage (cache) by this class should be converted to String object first.
 * @param <A> class of header members.
 * @param <B> class of values in table rows.
 * @param <C> class of primary key.
 */
public abstract class KeyColumnsRowsCollector<A, B, C extends PrimaryKey> implements AutoCloseable
{
	protected static final String KEY_ROW_DELIMITER = "=", COLUMN_VALUE_DELIMITER = ",";
	
	protected final Set<A> keyColumns;
	protected final LinkedHashMap<C, List<String>> rowsCache;
	protected final int maxCacheSize;
	
	protected final File rowsFile;
	protected final BufferedWriter buffWriter;
	
	public KeyColumnsRowsCollector(Set<A> keyColumns, int maxCacheSize, File rowsFile) throws IOException
	{
		this.keyColumns = keyColumns;
		rowsCache = new LinkedHashMap<>();
		this.maxCacheSize = maxCacheSize;
		
		this.rowsFile = rowsFile;
		buffWriter = new BufferedWriter(new FileWriter(this.rowsFile, false));
	}
	
	public KeyColumnsRowsCollector(Set<A> keyColumns) throws IOException
	{
		this(keyColumns, 10000, File.createTempFile("rows_cache", "tmp", new File(ClearThCore.tempPath())));
	}
	
	
	public void addRow(TableRow<A, B> row, C primaryKey, String rowName) throws IOException
	{
		if (getCacheSize() > maxCacheSize)
		{
			// Remove eldest entry in cache
			Map.Entry<C, List<String>> eldestEntry = rowsCache.entrySet().iterator().next();
			List<String> batch = eldestEntry.getValue();
			batch.remove(0);
			if (batch.isEmpty())
				rowsCache.remove(eldestEntry.getKey());
		}
		
		String rowString = tableRowToString(row, rowName);
		List<String> batch = rowsCache.computeIfAbsent(primaryKey, key -> new ArrayList<>());
		batch.add(rowString);
		buffWriter.write(primaryKey + KEY_ROW_DELIMITER + rowString + Utils.EOL);
		buffWriter.flush();
	}
	
	public String checkForDuplicatedRow(TableRow<A, B> rowToCheck, C primaryKey,
			BiFunction<TableRow<A, B>, TableRow<A, B>, Boolean> secondaryMatchFunc) throws IOException
	{
		// Trying to find duplicated row in cache first
		List<String> rowsBatch = rowsCache.get(primaryKey);
		if (rowsBatch != null)
		{
			for (String rowString : rowsBatch)
			{
				TableRow<A, B> possibleRow = stringToTableRow(rowString);
				if (secondaryMatchFunc.apply(rowToCheck, possibleRow))
					return getCachedRowName(possibleRow);
			}
		}
		
		// If not found yet, search in written file
		String primaryKeyStr = primaryKey == null ? null : primaryKey.toString();
		try (BufferedReader buffReader = new BufferedReader(new FileReader(rowsFile)))
		{
			String line;
			while ((line = buffReader.readLine()) != null)
			{
				int delimiter = line.indexOf(KEY_ROW_DELIMITER);
				if (delimiter < 0)
					continue;
				
				String possiblePrimaryKey = line.substring(0, delimiter);
				if (!checkStringKeys(primaryKeyStr, possiblePrimaryKey) ||
						!additionalKeysCheck(primaryKey, possiblePrimaryKey))
					continue;
				
				TableRow<A, B> possibleRow = stringToTableRow(line.substring(delimiter + 1));
				if (secondaryMatchFunc.apply(rowToCheck, possibleRow))
					return getCachedRowName(possibleRow);
			}
		}
		return null;
	}

	protected boolean checkStringKeys(String primaryKeyStr, String possiblePrimaryKey)
	{
		return Objects.equals(primaryKeyStr, possiblePrimaryKey);
	}

	protected boolean additionalKeysCheck(C primaryKeyToCheck, String possiblePrimaryKey)
	{
		return true;
	}

	@Override
	public void close() throws IOException
	{
		Utils.closeResource(buffWriter);
		FileUtils.deleteQuietly(rowsFile);
	}

	protected abstract String tableRowToString(TableRow<A, B> row, String rowName);
	
	protected abstract TableRow<A, B> stringToTableRow(String rowString);
	protected abstract String getCachedRowName(TableRow<A, B> cachedRow);
	
	protected int getCacheSize()
	{
		int size = 0;
		for (List<String> batch : rowsCache.values())
			size += batch.size();
		return size;
	}
}
