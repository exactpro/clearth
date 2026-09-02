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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.internal;

import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerInitialDataException;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.exactprosystems.clearth.utils.CollectionUtils.join;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class TreeIndexBuilder
{
	private final String tableName;
	private final List<String> keysSequence;

	
	public TreeIndexBuilder(String tableName, List<String> keysSequence)
	{
		this.tableName = tableName;
		this.keysSequence = keysSequence;
	}


	public TreeIndex build(StringTableData tableData) throws WoodpeckerException
	{
		checkKeysPresence(tableData);		
		IndexNode rootNode = new RootIndexNode(tableData.size());
		for (int rowNum = 0; rowNum < tableData.size(); rowNum++)
		{
			processRow(tableData.getRow(rowNum), rowNum, rootNode);
		}
		return new TreeIndex(keysSequence, rootNode);
	}
	
	private void processRow(TableRow<String, String> currentRow,
	                        int currentRowNum,
	                        IndexNode parentNode) throws WoodpeckerException
	{
		checkKeysNotBlank(currentRow, currentRowNum);
		processKey(keysSequence.iterator(), currentRow, currentRowNum, parentNode);
	}
	
	private void processKey(Iterator<String> keysIterator,
	                        TableRow<String, String> currentRow,
	                        int currentRowNum,
	                        IndexNode parentNode)
	{
		String keyName = keysIterator.next();
		String keyValue = currentRow.getValue(keyName);
		
		IndexNode node = parentNode.getSubNode(keyValue);
		if (node == null)
		{
			node = new NonRootIndexNode();
			parentNode.addSubNode(keyValue, node);
		}
		
		((NonRootIndexNode) node).addRowNum(currentRowNum);

		if (keysIterator.hasNext())
			processKey(keysIterator, currentRow, currentRowNum, node);
	}


	
	private void checkKeysPresence(StringTableData tableData) throws WoodpeckerException
	{
		TableHeader<String> header = tableData.getHeader();
		Set<String> missed = null;
		for (String key : keysSequence)
		{
			if (header.columnIndex(key) == -1)
			{
				if (missed == null)
					missed = new LinkedHashSet<>();
				missed.add(key);
			}
		}

		if (missed != null)
			throw new WoodpeckerInitialDataException(format("Unable to build index for table '%s' by keys %s. " +
							"The following key(s) missing in table: %s.",
					tableName, join(keysSequence), join(missed)));
	}

	private void checkKeysNotBlank(TableRow<String, String> row, int rowNum) throws WoodpeckerException
	{
		Set<String> blank = null;
		for (String key : keysSequence)
		{
			if (isBlank(row.getValue(key)))
			{
				if (blank == null)
					blank = new LinkedHashSet<>();
				blank.add(key);
			}
		}

		if (blank != null)
			throw new WoodpeckerInitialDataException(format("Unable to build index for table '%s' by keys %s. " +
							"The following key column(s) contain(s) null or blank values at row #%d: %s. Row: %s.",
					tableName, join(keysSequence), rowNum, join(blank), rowToString(row)));
	}
	
	private String rowToString(TableRow<String, String> row)
	{
		CommaBuilder cb = new CommaBuilder();
		for (String column : row.getHeader())
		{
			cb.append(format("%s='%s'", column, row.getValue(column)));
		}
		return cb.toString();
	}
}
