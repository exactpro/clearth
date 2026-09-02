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

package com.exactprosystems.clearth.woodpecker.initialdata.tables.internal;

import com.exactprosystems.clearth.utils.tabledata.writers.CsvDataWriter;
import com.exactprosystems.clearth.woodpecker.configuration.start.InitialDataTableDesc;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.utils.tabledata.StringTableData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.exactprosystems.clearth.ClearThCore.tempPath;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public abstract class TableDataLoader
{
	private static final Logger log = LoggerFactory.getLogger(TableDataLoader.class);
	
	private static final DateTimeFormatter FILE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss_SSS");
	
	protected final InitialDataTableDesc description;
	
	protected abstract StringTableData loadData() throws WoodpeckerException;
	
	protected TableDataLoader(InitialDataTableDesc description)
	{
		this.description = description;
	}
	
	public final StringTableData load() throws WoodpeckerException
	{
		StringTableData data = loadData();
		
		if (description.isWriteDebugFile())
			writeDebugFile(data);
		
		return data;
	}
	
	private void writeDebugFile(StringTableData data)
	{
		Path path = Paths.get(tempPath(), "woodpecker", createDebugFileName());
		try
		{
			createDirectories(path.getParent());
			CsvDataWriter.write(data, newBufferedWriter(path, CREATE, TRUNCATE_EXISTING), true);
		}
		catch (IOException e)
		{
			log.warn("Error while writing data from table {} to debug file '{}'.", 
					new Object[]{description.getName(), path, e});
		}
	}
	
	private String createDebugFileName()
	{
		return format("%s_%s.csv", description.getName(), FILE_DATE_TIME_FORMATTER.format(LocalDateTime.now()));
	}
}
