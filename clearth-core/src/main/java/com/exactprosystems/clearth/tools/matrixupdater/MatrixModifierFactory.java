/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.tools.matrixupdater;

import com.exactprosystems.clearth.tools.matrixupdater.matrixModifier.MatrixModifier;
import com.exactprosystems.clearth.tools.matrixupdater.matrixModifier.implementations.ActionCellAppender;
import com.exactprosystems.clearth.tools.matrixupdater.matrixModifier.implementations.ActionCellModifier;
import com.exactprosystems.clearth.tools.matrixupdater.matrixModifier.implementations.ActionRemover;
import com.exactprosystems.clearth.tools.matrixupdater.matrixModifier.implementations.ActionsAppender;
import com.exactprosystems.clearth.tools.matrixupdater.settings.Update;

import java.nio.file.Path;

class MatrixModifierFactory
{

	MatrixModifier initActionsAppender(Update update, Path pathToFiles) throws MatrixUpdaterException
	{
		return new ActionsAppender(update, pathToFiles);
	}

	MatrixModifier initActionCellAppender(Update update) throws MatrixUpdaterException
	{
		return new ActionCellAppender(update);
	}

	MatrixModifier initActionCellModifier(Update update) throws MatrixUpdaterException
	{
		return new ActionCellModifier(update);
	}

	MatrixModifier initActionRemover(Update update) throws MatrixUpdaterException
	{
		return new ActionRemover(update);
	}
}
