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

package com.exactprosystems.clearth.utils.tabledata.writers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import com.csvreader.CsvWriter;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import com.exactprosystems.clearth.utils.tabledata.TableDataWriter;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

/**
 * Writer of table-like data to CSV format
 * @author vladimir.panarin
 */
public class CsvDataWriter extends TableDataWriter<String, String>
{
	protected final CsvWriter writer;
	protected boolean needHeader;
	protected int rowIndex = -1;
	
	public CsvDataWriter(TableHeader<String> header, File f, boolean needHeader, boolean append) throws IOException
	{
		super(header);
		writer = createWriter(f, append);
		this.needHeader = needHeader;
	}
	
	public CsvDataWriter(TableHeader<String> header, Writer writer, boolean needHeader)
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
	
	
	protected CsvWriter createWriter(File f, boolean append) throws IOException
	{
		return new CsvWriter(new FileWriter(f, append), ',');
	}
	
	protected CsvWriter createWriter(Writer writer)
	{
		return new CsvWriter(writer, ',');
	}
	
	
	protected void writeHeader() throws IOException
	{
		for (String h : header)
			writer.write(h);
		writer.endRecord();
	}
	
	protected void writeNeededHeader() throws IOException
	{
		if (!needHeader)
			return;
		
		writeHeader();
		needHeader = false;
	}
	
	protected void doWriteRow(TableRow<String, String> row) throws IOException
	{
		for (String v : row)
			writer.write(v);
		writer.endRecord();
	}
}
