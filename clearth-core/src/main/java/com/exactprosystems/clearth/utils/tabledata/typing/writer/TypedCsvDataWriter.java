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

package com.exactprosystems.clearth.utils.tabledata.typing.writer;

import com.exactprosystems.clearth.utils.tabledata.TableDataWriter;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableHeaderWriter;
import com.exactprosystems.clearth.utils.tabledata.TableRow;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableHeaderItem;
import com.exactprosystems.clearth.utils.tabledata.typing.TypedTableRow;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriterConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

public class TypedCsvDataWriter extends TableDataWriter<TypedTableHeaderItem, Object> implements TableHeaderWriter
{
	protected final ClearThCsvWriter writer;
	protected boolean needHeader;
	protected int rowIndex = -1;

	public TypedCsvDataWriter(TableHeader<TypedTableHeaderItem> header, File f, boolean needHeader, boolean append) throws IOException
	{
		super(header);
		writer = createWriter(f, append);
		this.needHeader = needHeader;
	}

	public TypedCsvDataWriter(TableHeader<TypedTableHeaderItem> header, Writer writer, boolean needHeader) throws IOException
	{
		super(header);
		this.writer = createWriter(writer);
		this.needHeader = needHeader;
	}

	@Override
	public final void writeHeader() throws IOException, IllegalStateException
	{
		if (needHeader)
			writeNeededHeader();
		else
			throw TableHeaderWriter.cantWriteHeaderError();
	}

	@Override
	protected int writeRow(TableRow<TypedTableHeaderItem, Object> row) throws IOException
	{
		writeNeededHeader();
		doWriteRow(row);
		rowIndex++;
		return rowIndex;
	}

	@Override
	protected int writeRows(Collection<TableRow<TypedTableHeaderItem, Object>> tableRows) throws IOException
	{
		writeNeededHeader();
		for (TableRow<TypedTableHeaderItem, Object> r : tableRows)
		{
			doWriteRow(r);
			rowIndex++;
		}
		return rowIndex;
	}

	@Override
	public void close() throws IOException
	{
		writer.close();
	}


	protected ClearThCsvWriter createWriter(File f, boolean append) throws IOException
	{
		return new ClearThCsvWriter(new FileWriter(f, append), createCsvWriterConfig());
	}

	protected ClearThCsvWriter createWriter(Writer writer) throws IOException
	{
		return new ClearThCsvWriter(writer, createCsvWriterConfig());
	}

	protected void writeHeaderRow() throws IOException
	{
		for (TypedTableHeaderItem h : header)
			writer.write(h.getName());
		writer.endRecord();
	}

	protected void writeNeededHeader() throws IOException
	{
		if (!needHeader)
			return;

		writeHeaderRow();
		needHeader = false;
	}

	protected void doWriteRow(TableRow<TypedTableHeaderItem, Object> row) throws IOException
	{
		for (TypedTableHeaderItem item : header)
		{
			writer.write(((TypedTableRow)row).getString(item.getName()));
		}
		writer.endRecord();
	}

	protected ClearThCsvWriterConfig createCsvWriterConfig()
	{
		return new ClearThCsvWriterConfig();
	}
}
