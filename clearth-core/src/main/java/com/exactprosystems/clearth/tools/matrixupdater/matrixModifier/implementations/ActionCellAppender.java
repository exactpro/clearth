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

package com.exactprosystems.clearth.tools.matrixupdater.matrixModifier.implementations;

import com.exactprosystems.clearth.tools.matrixupdater.MatrixUpdaterException;
import com.exactprosystems.clearth.tools.matrixupdater.matrixModifier.MatrixModifier;
import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Cell;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Update;
import com.exactprosystems.clearth.tools.matrixupdater.utils.MatrixUpdaterUtils;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;

import java.util.*;

public class ActionCellAppender extends MatrixModifier
{
	public ActionCellAppender(Update update) throws MatrixUpdaterException
	{
		super(update);
	}

	@Override
	public void processMatrix(Matrix matrix) throws Exception
	{
		List<Block> blocks = new ArrayList<>(matrix.getBlocks());
		matrix.clear();

		for (Block block : blocks)
		{
			TableHeader<String> header = block.getHeader();

			if (!checkBlockHeader(header))
			{
				matrix.addBlock(block);
				continue;
			}

			TableHeader<String> newHeader = createNewHeader(block.getHeader());
			Block subBlock = new Block(header);

			for (Row record : block.getActions())
			{
				if (!checkRecord(record, record.getHeader()))
				{
					if (!compareHeaders(subBlock.getHeader(), header))
					{
						matrix.addBlock(subBlock);
						subBlock = new Block(header);
					}

					subBlock.addAction(record);
					continue;
				}

				if (!compareHeaders(subBlock.getHeader(), newHeader))
				{
					matrix.addBlock(subBlock);
					subBlock = new Block(newHeader);
				}

				processRecord(record, newHeader);
				subBlock.addAction(record);
			}
			matrix.addBlock(subBlock);
		}
	}

	protected TableHeader<String> createNewHeader(TableHeader<String> oldHeader)
	{
		List<String> currentHeader = MatrixUpdaterUtils.headerToList(oldHeader);
		List<String> newCells = MatrixUpdaterUtils.getListHeader(update.getSettings().getChange().getCells());

		int nextColIndex = currentHeader.indexOf("");

		if (nextColIndex > -1)
			currentHeader.addAll(nextColIndex, newCells);
		else
			currentHeader.addAll(newCells);

		return new TableHeader<>(new LinkedHashSet<>(currentHeader));
	}

	protected boolean compareHeaders(TableHeader<String> prev, TableHeader<String> next)
	{
		return prev.size() == next.size();
	}

	protected void processRecord(Row record, TableHeader<String> newHeader)
	{
		record.setHeader(newHeader);
		for (Cell cell : update.getSettings().getChange().getCells())
		{
			int columnIndex = record.getHeader().columnIndex(cell.getColumn());
			record.getValues().set(columnIndex, cell.getValue());
		}
	}
}
