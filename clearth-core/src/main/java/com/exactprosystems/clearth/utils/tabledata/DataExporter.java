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

package com.exactprosystems.clearth.utils.tabledata;

import java.io.IOException;

public class DataExporter<A, B, C extends BasicTableData<A, B>>
{
	private TableRowConverter<A, B> tableRowConverter = null;
	private final BasicTableDataReader<A, B, C> dataReader;
	private final TableDataWriter<A, B> dataWriter;

	private int bufferSize = 100;

	public DataExporter(BasicTableDataReader<A, B, C> dataReader,
						TableDataWriter<A, B> dataWriter)
	{
		this.dataReader = dataReader;
		this.dataWriter = dataWriter;
	}

	public DataExporter(BasicTableDataReader<A, B, C> dataReader,
						TableDataWriter<A, B> dataWriter, int bufferSize)
	{
		this(dataReader, dataWriter);
		this.bufferSize = bufferSize;
	}

	public DataExporter(BasicTableDataReader<A, B, C> dataReader,
						TableDataWriter<A, B> dataWriter, int bufferSize,
						TableRowConverter<A, B> tableRowConverter)
	{
		this(dataReader, dataWriter, bufferSize);
		this.tableRowConverter = tableRowConverter;
	}

	public void export() throws IOException, InterruptedException
	{
		Buffer<A, B> buffer = new Buffer<>(dataWriter, bufferSize);
		while (dataReader.hasMoreData())
		{
			if (Thread.interrupted())
				throw new InterruptedException("Data exporting is interrupted");

			TableRow<A, B> row = dataReader.readRow();
			if (tableRowConverter == null)
				buffer.add(row);
			else
				buffer.add(tableRowConverter.convert(row));
		}
		buffer.flush();
	}

	private static class Buffer<A, B>
	{
		private final int bufferSize;
		private final TableData<A, B> tableData;
		private final TableDataWriter<A, B> dataWriter;

		private int counter;

		public Buffer(TableDataWriter<A, B> dataWriter, int bufferSize)
		{
			tableData = new TableData<>(dataWriter.getHeader());
			this.bufferSize = bufferSize;
			this.dataWriter = dataWriter;
		}

		public void add(TableRow<A, B> row) throws IllegalArgumentException, IOException
		{
			tableData.add(row);
			if (++counter >= bufferSize)
			{
				flush();
			}
		}

		public void flush() throws IOException
		{
			if (!tableData.isEmpty())
			{
				dataWriter.writeRows(tableData.getRows());
				tableData.clear();
				counter = 0;
			}
		}

		public boolean isEmpty()
		{
			return tableData.isEmpty();
		}
	}
}
