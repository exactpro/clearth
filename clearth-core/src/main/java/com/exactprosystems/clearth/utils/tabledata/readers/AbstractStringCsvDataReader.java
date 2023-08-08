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

package com.exactprosystems.clearth.utils.tabledata.readers;

import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.tabledata.BasicTableData;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

public abstract class AbstractStringCsvDataReader<C extends BasicTableData<String, String>> extends AbstractCsvDataReader<String, String, C>
{
	public AbstractStringCsvDataReader(File f) throws IOException
	{
		super(f);
	}

	public AbstractStringCsvDataReader(Reader reader) throws IOException
	{
		super(reader);
	}

	public AbstractStringCsvDataReader(File f, ClearThCsvReaderConfig config) throws IOException
	{
		super(f, config);
	}

	public AbstractStringCsvDataReader(Reader reader, ClearThCsvReaderConfig config) throws IOException
	{
		super(reader, config);
	}

	@Override
	protected void fillRow(TableRow<String, String> row) throws IOException
	{
		for (String h : row.getHeader())
			row.setValue(h, reader.get(h));
	}

	@Override
	protected Set<String> readHeader() throws IOException
	{
		if (!reader.readHeader())
			throw new IOException("Could not read CSV header");

		return reader.getHeader();
	}
}
