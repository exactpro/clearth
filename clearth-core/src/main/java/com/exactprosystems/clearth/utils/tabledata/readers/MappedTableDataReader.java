/*******************************************************************************
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

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.BasicTableData;
import com.exactprosystems.clearth.utils.tabledata.RowsListFactory;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

import java.io.IOException;
import java.util.Set;

public class MappedTableDataReader<A, B, C extends BasicTableData<A, B>> extends BasicTableDataReader<A, B, C>
{
	private final BasicTableDataReader<A, B, C> aggregatedReader;
	private final HeaderMapper<A> headerMapper;

	public MappedTableDataReader(BasicTableDataReader<A, B, C> aggregatedReader, HeaderMapper<A> headerMapper)
	{
		this.aggregatedReader = aggregatedReader;
		this.headerMapper = headerMapper;
	}
	
	@Override
	public void setRowsListFactory(RowsListFactory<A, B> rowsListFactory)
	{
		rowsListFactory = aggregatedReader.rowsListFactory;
		aggregatedReader.setRowsListFactory(rowsListFactory);
	}

	@Override
	protected Set<A> readHeader() throws IOException
	{
		// Header should be read only once, and aggregated reader does it
		return aggregatedReader.getTableData().getHeader().toSet();
	}

	@Override
	public boolean hasMoreData() throws IOException
	{
		return aggregatedReader.hasMoreData();
	}

	@Override
	protected void fillRow(TableRow<A, B> row) throws IOException
	{
		TableRow<A, B> rawRow = aggregatedReader.readRow();
		for (int i = 0; i < row.size(); i++)
			row.setValue(i, rawRow.getValue(i));
	}

	@Override
	public boolean filter() throws IOException
	{
		return aggregatedReader.filter();
	}

	@Override
	public C start() throws IOException
	{
		aggregatedReader.start();

		tableData = createTableData(aggregatedReader.getTableData().getHeader().toSet(), aggregatedReader.rowsListFactory);
		return tableData;
	}

	@Override
	protected C createTableData(Set<A> header, RowsListFactory<A, B> rowsListFactory)
	{
		Set<A> headerConverted = headerMapper.convert(header);
		return aggregatedReader.createTableData(headerConverted, rowsListFactory);
	}

	@Override
	public void close() throws IOException
	{
		Utils.closeResource(aggregatedReader);
	}
}