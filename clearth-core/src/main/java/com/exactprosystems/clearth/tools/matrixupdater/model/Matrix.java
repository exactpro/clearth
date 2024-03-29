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

package com.exactprosystems.clearth.tools.matrixupdater.model;

import java.util.*;

public class Matrix
{
	protected final Map<Integer, List<String>> blockDuplicatedFields;
	protected List<Block> blocks;

	public Matrix()
	{
		blocks = new ArrayList<>();
		blockDuplicatedFields = new LinkedHashMap<>();
	}

	public void addBlock(Block block)
	{
		blocks.add(block);
	}

	public void addBlocks(List<Block> blocks)
	{
		this.blocks.addAll(blocks);
	}

	public void clear()
	{
		blockDuplicatedFields.clear();
		blocks.clear();
	}

	public List<Block> getBlocks()
	{
		return blocks;
	}

	public Map<Integer, List<String>> getBlockDuplicatedFields()
	{
		return Collections.unmodifiableMap(blockDuplicatedFields);
	}

	public void addDuplicatedFields(Integer rowIndex, List<String> duplicatedFields)
	{
		blockDuplicatedFields.put(rowIndex, duplicatedFields);
	}
}
