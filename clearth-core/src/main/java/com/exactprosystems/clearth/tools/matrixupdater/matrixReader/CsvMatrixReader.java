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

import com.exactprosystems.clearth.automation.generator.CsvActionReader;
import com.exactprosystems.clearth.tools.matrixupdater.model.Block;
import com.exactprosystems.clearth.tools.matrixupdater.model.Matrix;
import com.exactprosystems.clearth.tools.matrixupdater.model.Row;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class CsvMatrixReader extends MatrixFileReader
{
	private static Logger logger = LoggerFactory.getLogger(CsvMatrixReader.class);
	private boolean hasNextLine = true;
	
	@Override
	public Matrix readMatrix(File matrixFile) throws IOException
	{
		Matrix matrix = new Matrix();

		try (CsvActionReader reader = new CsvActionReader(matrixFile.getAbsolutePath(), true))
		{
			if (!reader.readNextLine()) return matrix;

			String[] header = isHeader(reader.getRawLine()) ? reader.getValues() : null;
			Map<Integer, String> duplicatedFields = new LinkedHashMap<>();

			if (header == null)
			{
				matrix.addBlock(readInitialBlock(reader));

				if (isHeader(reader.getRawLine()))
					header = reader.getValues();
				else
					return matrix; // There are no more lines after initial block except header
			}

			Block block;
			Set<String> headerSet = removeDuplicateFieldsHeader(header, duplicatedFields);

			if (!duplicatedFields.isEmpty())
				matrix.addDuplicatedFields(reader.getRowIndex(), new ArrayList<>(duplicatedFields.values()));

			while ((block = readBlock(reader, headerSet, duplicatedFields)) != null)
			{
				if (!block.getActions().isEmpty())
					matrix.addBlock(block);

				if (isHeader(reader.getRawLine()))
					headerSet = removeDuplicateFieldsHeader(reader.getValues(), duplicatedFields);
				else if (reader.isEmptyLine() || reader.isCommentLine())
					headerSet.clear();

				if (!duplicatedFields.isEmpty())
					matrix.addDuplicatedFields(reader.getRowIndex(), new ArrayList<>(duplicatedFields.values()));
			}
		}

		return matrix;
	}

	protected Block readInitialBlock(CsvActionReader reader) throws IOException
	{
		Block block = new Block(new TableHeader<>(new HashSet<>()));

		block.addAction(createRow(null, Arrays.asList(reader.getValues())));

		while (reader.readNextLine() && !isHeader(reader.getRawLine()))
		{
			block.addAction(createRow(null, Arrays.asList(reader.getValues())));
		}

		return block;
	}

	protected Block readBlock(CsvActionReader reader, Set<String> header, Map<Integer, String> duplicatedFields) throws IOException
	{
		/** empty lines or comments */
		if (reader.isEmptyLine() || reader.isCommentLine())
		{
			Block block = new Block(new TableHeader<>(new HashSet<>()));

			while (hasNextLine && (reader.isCommentLine() || reader.isEmptyLine()))
			{
				block.addAction(createRow(null, Arrays.asList(reader.getValues())));
				hasNextLine = reader.readNextLine();
			}

			return block.getActions().isEmpty() ? null : block;
		}

		Block block = new Block(new TableHeader<>(header));

		/** the other blocks */
		while (reader.readNextLine() && !(reader.isEmptyLine() || reader.isCommentLine() || isHeader(reader.getRawLine())))
		{
			List<String> values = removeDuplicatedFieldsValues(Arrays.asList(reader.getValues()), duplicatedFields);
			block.addAction(createRow(block.getHeader(), values));
		}

		duplicatedFields.clear();
		return block.getActions().isEmpty() ? null : block;
	}

	/**
	 * Checks if a record is a header.
	 *
	 * Removes quotation marks from the first value
	 * and checks if it starts with hash sign ('#')
	 * followed at least by one letter symbol.
	 *
	 * Else considers the record just as line.
	 *
	 * @param record raw-record from matrix.
	 * @return true if this record is a header.
	 */
	protected boolean isHeader(String record)
	{
		String[] values = record.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

		if (values.length == 0)
			return false;

		String value = values[0];

		if (value.startsWith("\""))
			value = value.replaceAll("\"", "");

		return value.matches("(#)(\\w+)(.*)");
	}

	protected Row createRow(TableHeader<String> header, List<String> valueList)
	{
		return header == null
				? new Row(valueList)
				: new Row(header, valueList);
	}
}
