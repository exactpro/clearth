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

package com.exactprosystems.clearth.tools.matrixupdater.matrixWriter;

import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.utils.StringOperationUtils;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;

public class CsvMatrixWriter implements MatrixWriter
{
	private static final String DELIMITER = ",", 
			SEPARATOR = System.lineSeparator();

	@Override
	public File writeMatrix(File file, Matrix matrix) throws IOException
	{
		FileUtils.deleteQuietly(file.getCanonicalFile());
		File result = new File(file.getCanonicalPath());

		try(BufferedWriter writer = new BufferedWriter(new FileWriter(result)))
		{
			for (Block block : matrix.getBlocks())
			{
				List<Row> records = block.getActions();
				if (records == null || records.isEmpty())
					continue;

				TableHeader<String> header = block.getHeader();
				writer.write(headerToLine(header));

				for (Row record : records)
				{
					if (header.size() < 1)
					{
						writer.write(valuesToLineForEmptyHeader(record.getValues()));
						continue;
					}
					writer.write(valuesToLine(header, record));
				}
			}
		}
		return result;
	}

	private String valuesToLine(TableHeader<String> header, Row record)
	{
		List<String> values = record.getValues();
		StringJoiner joiner = new StringJoiner(DELIMITER);
		for (String s : header)
			joiner.add(StringOperationUtils.quote(values.get(header.columnIndex(s))));

		return joiner.toString() + SEPARATOR;
	}

	private String valuesToLineForEmptyHeader(List<String> records)
	{
		StringJoiner joiner = new StringJoiner(DELIMITER);
		for (String s : records)
			joiner.add(StringOperationUtils.quote(s));

		return joiner.toString() + SEPARATOR;
	}

	private String headerToLine(TableHeader<String> header)
	{
		StringJoiner joiner = new StringJoiner(DELIMITER);
		for (String s : header)
			joiner.add(StringOperationUtils.quote(s));

		return joiner.toString() + SEPARATOR;
	}
}
