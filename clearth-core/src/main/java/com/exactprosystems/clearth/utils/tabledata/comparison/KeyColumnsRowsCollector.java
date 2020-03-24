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
package com.exactprosystems.clearth.utils.tabledata.comparison;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;

public class KeyColumnsRowsCollector implements Closeable
{
	protected static final String ROW_NAME_COLUMN = "## ROW_NAME ##",
			KEY_ROW_DELIMITER = "=", COLUMN_VALUE_DELIMITER = ",";
	
	protected final Set<String> keyColumns;
	protected LinkedHashMap<String, List<String>> rowsCache;
	protected File rowsFile;
	protected BufferedWriter buffWriter;
	
	public KeyColumnsRowsCollector(Set<String> keyColumns) throws IOException
	{
		this.keyColumns = keyColumns;
		rowsCache = new LinkedHashMap<>();
		rowsFile = File.createTempFile("rows_cache", "tmp", new File(ClearThCore.tempPath()));
		buffWriter = new BufferedWriter(new FileWriter(rowsFile, false));
	}
	
	public void addRow(String rowName, String primaryKey, TableRow<String, String> row) throws IOException
	{
		if (getCacheSize() > 10000)
		{
			// Remove eldest entry in cache
			String eldestKey = rowsCache.keySet().iterator().next();
			List<String> batch = rowsCache.get(eldestKey);
			batch.remove(0);
			if (batch.isEmpty())
				rowsCache.remove(eldestKey);
		}
		
		String rowString = tableRowToString(rowName, row);
		List<String> batch = rowsCache.computeIfAbsent(primaryKey, key -> new ArrayList<>());
		batch.add(rowString);
		buffWriter.write(primaryKey + KEY_ROW_DELIMITER + rowString + Utils.EOL);
		buffWriter.flush();
	}
	
	public String checkForDuplicatedRow(String primaryKey, TableRow<String, String> rowToCheck,
			BiFunction<TableRow<String, String>, TableRow<String, String>, Boolean> secondaryMatchFunc) throws IOException
	{
		// Try to find duplicated row in cache first
		List<String> rowsBatch = rowsCache.get(primaryKey);
		if (rowsBatch != null)
		{
			for (String rowString : rowsBatch)
			{
				TableRow<String, String> possibleRow = stringToTableRow(rowString);
				if (secondaryMatchFunc.apply(rowToCheck, possibleRow))
					return possibleRow.getValue(ROW_NAME_COLUMN);
			}
		}
		
		// If not found yet, search in written file
		try (BufferedReader buffReader = new BufferedReader(new FileReader(rowsFile)))
		{
			String line;
			while ((line = buffReader.readLine()) != null)
			{
				int delimiter = line.indexOf(KEY_ROW_DELIMITER);
				if (delimiter < 0 || !StringUtils.equals(primaryKey, line.substring(0, delimiter)))
					continue;
				
				TableRow<String, String> possibleRow = stringToTableRow(line.substring(delimiter + 1));
				if (secondaryMatchFunc.apply(rowToCheck, possibleRow))
					return possibleRow.getValue(ROW_NAME_COLUMN);
			}
		}
		return null;
	}
	
	@Override
	public void close() throws IOException
	{
		Utils.closeResource(buffWriter);
		FileUtils.deleteQuietly(rowsFile);
	}
	
	
	protected String tableRowToString(String rowName, TableRow<String, String> row)
	{
		CommaBuilder builder = new CommaBuilder(COLUMN_VALUE_DELIMITER);
		builder.append(ROW_NAME_COLUMN).add(KEY_ROW_DELIMITER).add(rowName);
		for (String column : row.getHeader())
		{
			if (keyColumns.contains(column)) // Collect only row name and key values
				builder.append(column).add(KEY_ROW_DELIMITER).add(row.getValue(column));
		}
		return builder.toString();
	}
	
	protected TableRow<String, String> stringToTableRow(String rowString)
	{
		Set<String> columns = new LinkedHashSet<>();
		List<String> values = new ArrayList<>();
		
		if (StringUtils.isNotBlank(rowString))
		{
			String[] columnsValuesArray = rowString.split(COLUMN_VALUE_DELIMITER);
			for (String columnValue : columnsValuesArray)
			{
				String[] columnValueArray = columnValue.split(KEY_ROW_DELIMITER, 2);
				columns.add(columnValueArray[0]);
				values.add(columnValueArray[1]);
			}
		}
		return new TableRow<>(new TableHeader<>(columns), values);
	}
	
	protected int getCacheSize()
	{
		int size = 0;
		for (List<String> batch : rowsCache.values())
			size += batch.size();
		return size;
	}
}
