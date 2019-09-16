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
import com.exactprosystems.clearth.utils.tabledata.TableHeader;

public class ActionCellModifier extends MatrixModifier
{
	public ActionCellModifier(Update update) throws MatrixUpdaterException
	{
		super(update);
	}

	@Override
	public void processMatrix(Matrix matrix) throws Exception
	{
		for (Block block : matrix.getBlocks())
		{
			TableHeader<String> header = block.getHeader();

			if (!checkBlockHeader(header))
				continue;

			for (Row record : block.getActions())
			{
				if (checkRecord(record, header))
				{
					processRecord(record, header);
				}
			}
		}
	}

	protected void processRecord(Row record, TableHeader<String> header)
	{
		for (Cell cell : update.getSettings().getChange().getCells())
		{
			String column = cell.getColumn();
			int index = header.columnIndex(column);

			if (index != -1)
				record.getValues().set(index, cell.getValue());
		}
	}
}
