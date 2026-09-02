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

import com.exactprosystems.clearth.utils.LineBuilder;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.repeat;

public class RootIndexNode extends IndexNode
{
	private final int tableSize;

	public RootIndexNode(int tableSize)
	{
		this.tableSize = tableSize;
	}

	@Override
	public int getRowNum(int index)
	{
		if ((index < 0) || (index >= tableSize))
			throw new IndexOutOfBoundsException(format("index: %d, tableSize: %d", index, tableSize));
		return index;
	}

	@Override
	public int getCountOfReferencedRows()
	{
		return tableSize;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (!(obj instanceof RootIndexNode))
			return false;
		
		return super.equals(obj) && (this.tableSize == ((RootIndexNode)obj).tableSize);
	}

	@Override
	public String toString()
	{
		return toString(0);
	}

	@Override
	protected String toString(int level)
	{
		LineBuilder lb = new LineBuilder();
		return lb.add(repeat("\t", level)).add("tableSize: ").append(tableSize)
				.add(toStringSubNodes(level))
				.toString();
	}
}
