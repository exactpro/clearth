/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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
package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.utils.LimitedLinkedHashMap;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RowsNumberExecutor implements AutoCloseable
{
	private static final String FILE_NAME = "rowsnumbers";
	private static final Path DIR_NAME = Paths.get(ClearThCore.rootRelative("temp"));
	private static final String EXT_NAME = "txt";
	private Path pathToRowsNumberFile;
	private BufferedWriter bw;
	private LimitedLinkedHashMap<String, String> rowsNumberCache;

	public RowsNumberExecutor() throws IOException
	{
		pathToRowsNumberFile = File.createTempFile(FILE_NAME, EXT_NAME, DIR_NAME.toFile()).toPath();
		rowsNumberCache = new LimitedLinkedHashMap<>(10000);
		bw = new BufferedWriter(new FileWriter(pathToRowsNumberFile.toFile(),true));
	}

	public String processCurrentRow(String rowKey, int rowsCount) throws IOException
	{
		String duplicateFromCache = rowsNumberCache.get(rowKey);
		if(duplicateFromCache == null || duplicateFromCache.isEmpty())
		{
			String rowNumber = "Row #" + rowsCount;
			rowsNumberCache.put(rowKey, rowNumber);
			String duplicateFromFile = getNumberOfDuplicateString(rowKey);

			if(duplicateFromFile == null || duplicateFromFile.isEmpty())
			{
				writeRowKeyToFile(rowKey, rowNumber);
				return null;
			}
			else
			{
				return duplicateFromFile;
			}
		}
		return duplicateFromCache;
	}
	
	private void writeRowKeyToFile(String key, String number) throws IOException
	{
		bw.write(String.format("%s %s", key, number));
		bw.newLine();
		bw.flush();
	}

	private String getNumberOfDuplicateString(String key) throws IOException
	{
		try(BufferedReader br = new BufferedReader(new FileReader(pathToRowsNumberFile.toFile())))
		{
			String line;
			while((line = br.readLine()) != null)
			{
				int index = line.indexOf(' ');
				if(index == -1)
					continue;
				String lineKey = line.substring(0,index);
				if(key.equals(lineKey))
				{
					return line.substring(index);
				}
			}
		}
		return null;
	}

	@Override
	public void close() throws IOException
	{
		if(bw != null)
			bw.close();
		Files.deleteIfExists(pathToRowsNumberFile);
	}
}

