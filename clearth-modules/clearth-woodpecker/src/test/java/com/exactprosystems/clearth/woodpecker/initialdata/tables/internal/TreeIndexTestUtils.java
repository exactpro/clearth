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

import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

import java.util.*;
import java.util.stream.Collectors;

import static com.exactprosystems.clearth.utils.tabledata.RowsListFactories.arrayListFactory;
import static java.util.Arrays.asList;

/**
 * 25 October 2018
 */
public class TreeIndexTestUtils
{
	public static StringTableData table(String[][] data)
	{
		Set<String> headerSet = new LinkedHashSet<>();
		Collections.addAll(headerSet, data[0]);
		TableHeader<String> header = new TableHeader<>(headerSet);
		
		StringTableData tableData = new StringTableData(header, arrayListFactory());
		for (int r = 1; r < data.length; r++)
		{
			tableData.add(new TableRow<>(header, asList(data[r])));
		}
		return tableData;
	}
	
	public static Map<String, IndexNode> subNodes(Object... nodes)
	{
		Map<String, IndexNode> map = new LinkedHashMap<>();
		for (int i = 0; i < nodes.length; i += 2)
		{
			map.put((String)nodes[i], (IndexNode)nodes[i + 1]);
		}
		return map;
	}
	
	public static IndexNode root(int tableSize, Map<String, IndexNode> subNodes)
	{
		IndexNode node = new RootIndexNode(tableSize);
		for (Map.Entry<String, IndexNode> e : subNodes.entrySet())
		{
			node.addSubNode(e.getKey(), e.getValue());
		}
		return node;
	}

	public static IndexNode node(List<Integer> rowNums)
	{
		return node(rowNums, null);
	}
	
	public static IndexNode node(List<Integer> rowNums, Map<String, IndexNode> subNodes)
	{
		NonRootIndexNode node = new NonRootIndexNode();
		for (Integer rowNum : rowNums)
		{
			node.addRowNum(rowNum);
		}
		if (subNodes != null)
		{
			for (Map.Entry<String, IndexNode> e : subNodes.entrySet())
			{
				node.addSubNode(e.getKey(), e.getValue());
			}
		}
		return node;
	}
	
	public static List<Integer> rowNums(int... nums)
	{
		return Arrays.stream(nums)
				.boxed()
				.collect(Collectors.toList());
	}
	
	public static KeyCondition eq(String value)
	{
		return new KeyEquals(value);
	}
	
	public static KeyCondition notEq(String value)
	{
		return new KeyNotEquals(value);
	}
	
	public static KeyCondition in(String... values)
	{
		return new KeyIn(asList(values));
	}
	
	public static KeyCondition notIn(String... values)
	{
		Set<String> set = new HashSet<>();
		Collections.addAll(set, values);
		return new KeyNotIn(set);
	}
	
	public static Map<String, KeyCondition> query(Object... conditions)
	{
		Map<String, KeyCondition> query = new LinkedHashMap<>();
		for (int i = 0; i < conditions.length; i += 2)
		{
			query.put((String)conditions[i], (KeyCondition) conditions[i + 1]);
		}
		return query;
	}
}
