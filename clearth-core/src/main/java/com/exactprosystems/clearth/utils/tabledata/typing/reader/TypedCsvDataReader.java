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

package com.exactprosystems.clearth.utils.tabledata.typing.reader;

import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.readers.AbstractCsvDataReader;
import com.exactprosystems.clearth.utils.tabledata.typing.TableDataType;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableData;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeaderItem;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.Set;

public class TypedCsvDataReader extends AbstractCsvDataReader<TypedTableHeaderItem, Object, TypedTableData>
{

	public TypedCsvDataReader(File f) throws IOException
	{
		super(f);
	}

	public TypedCsvDataReader(Reader reader) throws IOException
	{
		super(reader);
	}


	@Override
	protected Set<TypedTableHeaderItem> readHeader() throws IOException
	{
		if (!reader.hasHeader())
			throw new IOException("Could not read CSV header");

		LinkedHashSet<TypedTableHeaderItem> result = new LinkedHashSet<>();
		for (String h : reader.getHeader())
			result.add(new TypedTableHeaderItem(h, TableDataType.STRING));

		return result;
	}

	@Override
	protected void fillRow(TableRow<TypedTableHeaderItem, Object> row) throws IOException
	{
		for (TypedTableHeaderItem item : row.getHeader())
			row.setValue(item, reader.get(item.getName()));
	}

	@Override
	protected TypedTableData createTableData(Set<TypedTableHeaderItem> header,
			RowsListFactory<TypedTableHeaderItem, Object> rowsListFactory)
	{
		return new TypedTableData(header, rowsListFactory);
	}
}
