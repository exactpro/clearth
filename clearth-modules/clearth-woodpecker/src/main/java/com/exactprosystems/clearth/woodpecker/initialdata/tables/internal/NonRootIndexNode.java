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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.repeat;

public class NonRootIndexNode extends IndexNode
{
	private final List<Integer> rowNums = new ArrayList<>();

	@Override
	public int getRowNum(int index)
	{
		return rowNums.get(index);
	}

	@Override
	public int getCountOfReferencedRows()
	{
		return rowNums.size();
	}

	public List<Integer> getRowNums()
	{
		return rowNums;
	}

	public void addRowNum(int rowNum)
	{
		rowNums.add(rowNum);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (!(obj instanceof NonRootIndexNode))
			return false;
		
		return super.equals(obj) && Objects.equals(this.rowNums, ((NonRootIndexNode) obj).rowNums);
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
		return lb.add(repeat("\t", level)).add("rowNums: ").append(rowNums)
				.add(toStringSubNodes(level))
				.toString();
	}
}
