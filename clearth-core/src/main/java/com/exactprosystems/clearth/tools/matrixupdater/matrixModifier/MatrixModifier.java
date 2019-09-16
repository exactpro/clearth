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

package com.exactprosystems.clearth.tools.matrixupdater.matrixModifier;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.tools.matrixupdater.MatrixUpdaterException;
import com.exactprosystems.clearth.tools.matrixupdater.model.Cell;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Change;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Condition;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Update;
import com.exactprosystems.clearth.utils.ComparisonUtils;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;

import java.util.Arrays;
import java.util.List;

public abstract class MatrixModifier
{
	protected Update update;

	protected ComparisonUtils comparisonUtils;

	public abstract void processMatrix(Matrix matrix) throws Exception;

	public MatrixModifier(Update update) throws MatrixUpdaterException
	{
		validateUpdate(update);
		this.update = update;

		this.comparisonUtils = ClearThCore.comparisonUtils();
	}

	protected void validateUpdate(Update update) throws MatrixUpdaterException
	{
		validateConditions(update);
		validateChanges(update);
	}

	protected void validateConditions(Update update) throws MatrixUpdaterException
	{
		if (update.getSettings().getConditions().isEmpty())
			throw new MatrixUpdaterException("No conditions specified in '" + update.getName() + "' update");
	}

	protected void validateChanges(Update update) throws MatrixUpdaterException
	{
		Change change = update.getSettings().getChange();

		if (change == null || change.getCells() == null || change.getCells().isEmpty())
			throw new MatrixUpdaterException("No changes specified in '" + update.getName() + "' update");
	}

	protected boolean checkBlockHeader(TableHeader<String> header)
	{
		boolean valid = false;

		for (Condition condition : update.getSettings().getConditions())
		{
			valid = true;

			for (Cell cell : condition.getCells())
			{
				if (header.columnIndex(cell.getColumn()) < 0)
				{
					valid = false;
					break;
				}
			}

			if (valid) break;
		}

		return valid;
	}

	protected boolean checkRecord(Row record, TableHeader<String> header) throws ParametersException
	{
		boolean isEqual = false;

		for (Condition condition : update.getSettings().getConditions())
		{
			for (Cell cell : condition.getCells())
			{
				int index = header.columnIndex(cell.getColumn());

				if (index < 0)
				{
					isEqual = false;
					break;
				}

				String actualValue = record.getValues().get(index);
				String expectedValue = cell.getValue();

				if (comparisonUtils.compareValues(expectedValue, actualValue, false))
				{
					isEqual = true;
				}
				else
				{
					isEqual = false;
					break;
				}
			}

			if (isEqual) break;
		}

		return isEqual;
	}

	protected boolean equalsHeader(String[] header1, String[] header2)
	{
		return equalsHeader(header1, Arrays.asList(header2));
	}

	protected boolean equalsHeader(String[] header1, List<String> header2)
	{
		if (header1.length != header2.size())
			return false;

		for (String value : header1)
		{
			if (!header2.contains(value))
				return false;
		}

		return true;
	}
}
