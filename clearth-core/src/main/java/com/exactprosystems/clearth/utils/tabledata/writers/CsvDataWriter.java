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

package com.exactprosystems.clearth.utils.tabledata.writers;

import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.*;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriterConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;


/**
 * Writer of table-like data to CSV format
 * @author vladimir.panarin
 */
public class CsvDataWriter extends TableDataWriter<String, String> implements TableHeaderWriter
{
	protected final ClearThCsvWriter writer;
	protected boolean needHeader;
	protected int rowIndex = -1;
	
	public CsvDataWriter(TableHeader<String> header, File f, boolean needHeader, boolean append) throws IOException
	{
		super(header);
		writer = createWriter(f, append);
		this.needHeader = needHeader;
	}
	
	public CsvDataWriter(TableHeader<String> header, Writer writer, boolean needHeader) throws IOException
	{
		super(header);
		this.writer = createWriter(writer);
		this.needHeader = needHeader;
	}
	
	
	/**
	 * Writes whole table to given file, closing writer after that
	 * @param table to write data from
	 * @param f file to write data to
	 * @param needHeader flag which indicates if file needs CSV header
	 * @param append flag which indicates if existing file should be appended or overwritten
	 * @throws IOException if error occurs while writing data
	 */
	public static void write(StringTableData table, File f, boolean needHeader, boolean append) throws IOException
	{
		CsvDataWriter writer = null;
		try
		{
			writer = new CsvDataWriter(table.getHeader(), f, needHeader, append);
			writer.write(table.getRows());
		}
		finally
		{
			Utils.closeResource(writer);
		}
	}
	
	/**
	 * Writes whole table to given writer, closing CsvDataWriter after that
	 * @param table to write data from
	 * @param writer destination of data
	 * @param needHeader flag which indicates if CSV header needs to be written
	 * @throws IOException if error occurs while writing data
	 */
	public static void write(StringTableData table, Writer writer, boolean needHeader) throws IOException
	{
		CsvDataWriter csvWriter = null;
		try
		{
			csvWriter = new CsvDataWriter(table.getHeader(), writer, needHeader);
			csvWriter.write(table.getRows());
		}
		finally
		{
			Utils.closeResource(csvWriter);
		}
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
	public void close() throws IOException
	{
		writer.close();
	}

	@Override
	protected int writeRow(TableRow<String, String> row) throws IOException
	{
		writeNeededHeader();
		doWriteRow(row);
		rowIndex++;
		return rowIndex;
	}
	
	@Override
	protected int writeRows(Collection<TableRow<String, String>> rows) throws IOException
	{
		writeNeededHeader();
		for (TableRow<String, String> r : rows)
		{
			doWriteRow(r);
			rowIndex++;
		}
		return rowIndex;
	}

	protected ClearThCsvWriter createWriter(File f, boolean append) throws IOException
	{
		return new ClearThCsvWriter(new FileWriter(f, append), createCsvWriterConfig());
	}

	protected ClearThCsvWriter createWriter(Writer writer) throws IOException
	{
		return new ClearThCsvWriter(writer, createCsvWriterConfig());
	}

	protected ClearThCsvWriterConfig createCsvWriterConfig()
	{
		ClearThCsvWriterConfig writerConfig = new ClearThCsvWriterConfig();
		writerConfig.setWithTrim(true); // keeping legacy behavior
		return writerConfig;
	}

	protected void writeHeaderRow() throws IOException
	{
		for (String h : header)
			writer.write(h);
		writer.endRecord();
	}
	
	protected void writeNeededHeader() throws IOException
	{
		if (!needHeader)
			return;
		
		writeHeaderRow();
		needHeader = false;
	}
	
	protected void doWriteRow(TableRow<String, String> row) throws IOException
	{
		for (String v : row)
			writer.write(v);
		writer.endRecord();
	}
}
