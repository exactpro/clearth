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

import com.exactprosystems.clearth.connectivity.flat.FlatMessageDesc;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageDictionary;
import com.exactprosystems.clearth.connectivity.flat.FlatMessageFieldDesc;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.tabledata.TableDataWriter;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRow;

import java.io.*;
import java.util.Collection;

public class FlatFileWriter extends TableDataWriter<String, String>
{
	protected BufferedWriter writer;
	protected FlatMessageDesc messageDesc;
	protected int rowIndex = -1;
	
	public FlatFileWriter(TableHeader<String> header, File file, boolean append, FlatMessageDesc messageDesc) throws IOException
	{
		super(header);
		this.messageDesc = messageDesc;
		writer = createWriter(file, append);
	}
	
	public FlatFileWriter(TableHeader<String> header, File file, FlatMessageDesc messageDesc) throws IOException
	{
		this(header, file, false, messageDesc);
	}
	
	public FlatFileWriter(TableHeader<String> header, Writer writer, FlatMessageDesc messageDesc)
	{
		super(header);
		this.messageDesc = messageDesc;
		this.writer = createWriter(writer);
	}
	
	
	@Override
	protected int writeRow(TableRow<String, String> row) throws IOException
	{
		StringBuilder stringRow = new StringBuilder();
		int currentPosition = 0;
		for (FlatMessageFieldDesc fieldDesc : messageDesc.getFieldDesc())
		{
			// Obtain value to write and its length by dictionary
			String value = row.getValue(fieldDesc.getName());
			int valueLengthDesc = fieldDesc.getLength();
			if (value == null)
				value = "";
			else if (value.length() > valueLengthDesc)
				value = value.substring(0, valueLengthDesc);
			
			// Fill with spaces to get proper 'cursor' location (if some fields are not written)
			int positionDesc = fieldDesc.getPosition() - 1;
			appendSpaces(stringRow, positionDesc - currentPosition);
			
			// Append value to builder by necessary alignment (left by default)
			if (FlatMessageDictionary.RIGHT_ALIGNMENT.equalsIgnoreCase(fieldDesc.getAlignment()))
			{
				appendSpaces(stringRow, valueLengthDesc - value.length());
				stringRow.append(value);
			}
			else
			{
				stringRow.append(value);
				appendSpaces(stringRow, valueLengthDesc - value.length());
			}
			currentPosition = positionDesc + valueLengthDesc;
		}
		
		writer.write(stringRow.toString() + Utils.EOL);
		writer.flush();
		return ++rowIndex;
	}
	
	protected void appendSpaces(StringBuilder strBuilder, int count)
	{
		for (int i = 0; i < count; i++)
			strBuilder.append(' ');
	}
	
	@Override
	protected int writeRows(Collection<TableRow<String, String>> tableRows) throws IOException
	{
		for (TableRow<String, String> row : tableRows)
			writeRow(row);
		return rowIndex;
	}
	
	@Override
	public void close() throws IOException
	{
		if (writer != null)
			writer.close();
	}
	
	
	protected BufferedWriter createWriter(File file, boolean append) throws IOException
	{
		return new BufferedWriter(new FileWriter(file, append));
	}
	
	protected BufferedWriter createWriter(Writer writer)
	{
		return new BufferedWriter(writer);
	}
}
