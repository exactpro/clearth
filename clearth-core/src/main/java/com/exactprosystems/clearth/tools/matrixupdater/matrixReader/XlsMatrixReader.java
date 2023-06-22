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

package com.exactprosystems.clearth.tools.matrixupdater.matrixReader;

import com.exactprosystems.clearth.automation.generator.XlsActionReader;
import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.tools.matrixupdater.model.XlsMatrix;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class XlsMatrixReader extends MatrixFileReader
{
	@Override
	public Matrix readMatrix(File matrixFile) throws IOException
	{
		try (XlsActionReader reader = new XlsActionReader(matrixFile.getAbsolutePath(), true))
		{
			Matrix matrix = new XlsMatrix(reader.getSheet().getSheetName());
			String[] header = null;
			Block block = emptyBlock();
			Map<Integer, String> duplicatedFields = new LinkedHashMap<>();

			while (reader.readNextLine())
			{
				if (header == null)
				{
					if (reader.isCommentLine())
					{
						block.addAction(new Row(new TableHeader<>(new HashSet<>()), reader.parseLine(false)));
					}
				}

				if (reader.isCommentLine())
					continue;

				if (reader.isHeaderLine())
				{
					duplicatedFields.clear();
					header = reader.parseLine(false).toArray(new String[0]);

					if (!block.getActions().isEmpty())
						matrix.addBlock(block);

					block = new Block(new TableHeader<>(removeDuplicateFieldsHeader(header, duplicatedFields)));
					if (!duplicatedFields.isEmpty())
						matrix.addDuplicatedFields(reader.getRowIndex(), new ArrayList<>(duplicatedFields.values()));

					continue;
				}

				if (reader.isEmptyLine())
					continue;

				if (header != null)
				{
					List<String> listValues = removeDuplicatedFieldsValues(reader.parseLine(false), duplicatedFields);
					block.addAction(new Row(block.getHeader(), listValues));
				}
			}

			if (!block.getActions().isEmpty())
				matrix.addBlock(block);

			return matrix;
		}
	}

	protected Block emptyBlock()
	{
		return new Block(new TableHeader<>(new HashSet<>()));
	}
}
