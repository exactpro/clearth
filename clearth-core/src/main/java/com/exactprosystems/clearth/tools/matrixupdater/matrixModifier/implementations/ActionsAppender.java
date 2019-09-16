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
import com.exactprosystems.clearth.tools.matrixupdater.model.Addition;
import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Change;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Update;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.*;

public class ActionsAppender extends MatrixModifier
{
	protected Addition addition;

	protected static int prefix = 0;
	protected static final String ID_MARKER_TEMP = "_%du";

	public ActionsAppender(Update update) throws MatrixUpdaterException
	{
		super(update);

		Change change = update.getSettings().getChange();
		this.addition = new Addition(change.getAddition(), change.isBefore());
	}

	@Override
	protected void validateChanges(Update update) throws MatrixUpdaterException
	{
		File addition = update.getSettings().getChange().getAdditionFile();

		if (addition == null || addition.length() == 0L)
			throw new MatrixUpdaterException("Invalid addition file in '" + update.getName() + "' update");
	}

	@Override
	public void processMatrix(Matrix matrix) throws Exception
	{
		List<Map<String, String>> additions = addition.listAdditions();
		List<Block> blocks = new ArrayList<>(matrix.getBlocks());
		matrix.clear();

		for (Block block : blocks)
		{
			if (!checkBlockHeader(block.getHeader()))
			{
				matrix.addBlock(block);
				continue;
			}

			Block subBlock = new Block(block.getHeader());

			for (Row record : block.getActions())
			{
				if (!checkRecord(record, block.getHeader()))
				{
					subBlock.addAction(record);
					continue;
				}

				if (addition.isBefore())
				{
					if (!subBlock.getActions().isEmpty())
						matrix.addBlock(subBlock);

					matrix.addBlocks(saveNewActions(additions));
					subBlock = new Block(block.getHeader());
					subBlock.addAction(record);
				}
				else
				{
					subBlock.addAction(record);

					if (!subBlock.getActions().isEmpty())
						matrix.addBlock(subBlock);

					subBlock = new Block(block.getHeader());
					matrix.addBlocks(saveNewActions(additions));
				}
			}

			if (subBlock.getActions() != null && !subBlock.getActions().isEmpty())
				matrix.addBlock(subBlock);
		}
	}

	protected List<Block> saveNewActions(List<Map<String, String>> actionsToAdd)
	{
		List<Block> blocks = new ArrayList<>();

		String[] header = actionsToAdd.get(0).keySet().toArray(new String[0]);
		Block block = new Block(new TableHeader<>(new LinkedHashSet<>(Arrays.asList(header))));

		for (Map<String, String> action : actionsToAdd)
		{
			if (update.getSettings().getChange().isUpdateIDs())
				unifyId(action);

			String[] actionHeader = action.keySet().toArray(new String[0]);

			if (!equalsHeader(header, actionHeader))
			{
				header = actionHeader;
				blocks.add(block);

				block = new Block(new TableHeader<>(new LinkedHashSet<>(Arrays.asList(actionHeader))));
			}

			block.addAction(new Row(block.getHeader(), new ArrayList<>(Arrays.asList(action.values().toArray(new String[0])))));
		}

		blocks.add(block);

		prefix++;

		return blocks;
	}

	protected void unifyId(Map<String, String> row)
	{
		String prev = String.format(ID_MARKER_TEMP, prefix - 1);
		String newPrefix = String.format(ID_MARKER_TEMP, prefix);

		for (Map.Entry<String, String> cell : row.entrySet())
		{
			String value = cell.getValue();
			if (StringUtils.containsIgnoreCase(value, "id"))
			{
				if (value.contains(prev))
				{
					cell.setValue(value.replaceAll(prev, newPrefix));
				}
				else
				{
					cell.setValue(value.replaceAll("Id", "Id" + newPrefix)
							.replaceAll("id", "id" + newPrefix));
				}
			}
		}
	}
}
