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

import java.util.List;

public class RowNumSequence
{
	private final int[] rowNums;
	private final AtomicCyclicIntCounter counter;

	public int nextRowNum()
	{
		return rowNums[counter.next()];
	}
	
	public static RowNumSequence fromIndexNodes(List<IndexNode> nodes)
	{
		int size = 0;
		for (IndexNode node : nodes)
		{
			size += node.getCountOfReferencedRows();
		}
		
		int[] rowNums = new int[size];
		int index = 0;
		for (IndexNode node : nodes)
		{
			for (int i = 0; i < node.getCountOfReferencedRows(); i++)
			{
				rowNums[index] = node.getRowNum(i);
				index++;
			}
		}		
		return new RowNumSequence(rowNums);
	}
	
	private RowNumSequence(int[] rowNums)
	{
		this.rowNums = rowNums;
		this.counter = new AtomicCyclicIntCounter(rowNums.length);
	}
}
