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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.internal;

import com.exactprosystems.clearth.woodpecker.utils.AtomicCyclicIntCounter;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.woodpecker.configuration.WoodpeckerConfigFile;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerInitialDataException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isEmpty;

public class Table
{
	private final String tableName;
	
	private final StringTableData tableData;
	private final TreeIndex index;

	private final AtomicCyclicIntCounter mainSequence;

	// Map<Conditions, RowNumSequence>
	private final Map<Map<String, KeyCondition>, RowNumSequence> sequenceByConditions = new ConcurrentHashMap<>();


	public Table(String tableName, StringTableData tableData, TreeIndex index)
	{
		this.tableName = tableName;
		this.tableData = tableData;
		this.index = index;
		this.mainSequence = new AtomicCyclicIntCounter(tableData.size());
	}


	public int selectRandomRowNumber() throws WoodpeckerException
	{
		return selectRandomRowNumber(null);
	}

	public int selectRandomRowNumber(Map<String, KeyCondition> keyConditions) throws WoodpeckerException
	{
		ThreadLocalRandom random = ThreadLocalRandom.current();
		if (isEmpty(keyConditions))
			return random.nextInt(tableData.size());
		else
		{
			IndexNode node = findNode(keyConditions);
			int i = random.nextInt(node.getCountOfReferencedRows());
			return node.getRowNum(i);
		}
	}


	public int selectNextRowNumber()
	{
		return mainSequence.next();
	}

	public int selectNextRowNumber(Map<String, KeyCondition> keyConditions) throws WoodpeckerException
	{
		if (isEmpty(keyConditions))
			return selectNextRowNumber();

		RowNumSequence sequence = findSequence(keyConditions);
		return sequence.nextRowNum();
	}


	public String selectColumnValueByRowNum(int rowNum, String columnName) throws WoodpeckerException
	{
		if (!tableData.getHeader().containsColumn(columnName))
			throw new WoodpeckerInitialDataException(format("Table '%s' doesn't contain column '%s'.",
					tableName, columnName));

		if (rowNum >= tableData.size())
			throw new WoodpeckerInitialDataException(format("Unable to get row #%d. " +
							"Table size is %d rows (first number is 0).",
					rowNum, tableData.size()));

		return tableData.getRow(rowNum).getValue(columnName);
	}


	private IndexNode findNode(Map<String, KeyCondition> keyConditions) throws WoodpeckerException
	{
		checkTableIndexed();

		IndexNode node = index.find(keyConditions);
		if (node != null)
			return node;
		else
			throw whenDataNotFoundByKeys(keyConditions);
	}

	private List<IndexNode> findAllNodes(Map<String, KeyCondition> keyConditions) throws WoodpeckerException
	{
		checkTableIndexed();

		List<IndexNode> nodes = index.findAll(keyConditions);
		if (isNotEmpty(nodes))
			return nodes;
		else
			throw whenDataNotFoundByKeys(keyConditions);
	}

	private RowNumSequence findSequence(Map<String, KeyCondition> keyConditions) throws WoodpeckerException
	{
		RowNumSequence sequence = sequenceByConditions.get(keyConditions);
		if (sequence == null)
		{
			synchronized (sequenceByConditions)
			{
				sequence = sequenceByConditions.get(keyConditions);
				if (sequence == null)
				{
					List<IndexNode> nodes = findAllNodes(keyConditions);
					sequence = RowNumSequence.fromIndexNodes(nodes);
					sequenceByConditions.put(keyConditions, sequence);
				}
			}
		}
		return sequence;
	}


	private void checkTableIndexed() throws WoodpeckerException
	{
		if (index == null)
			throw new WoodpeckerInitialDataException(format("Unable to select data by keys from not indexed table '%s'. " +
							"Please specify index in %s.", tableName,
					WoodpeckerConfigFile.START_FILE.fileName()));
	}

	private WoodpeckerInitialDataException whenDataNotFoundByKeys(Map<String, KeyCondition> keyConditions)
	{
		return new WoodpeckerInitialDataException(format("No data found in table '%s' by keys %s.",
				tableName, conditionsToString(keyConditions)));
	}

	private String conditionsToString(Map<String, KeyCondition> keyConditions)
	{
		return keyConditions.entrySet().stream()
				.map(e -> e.getKey() + " " + e.getValue())
				.collect(Collectors.joining(", "));
	}
}
