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

package com.exactprosystems.clearth.tools.matrixupdater.model;

import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReader;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Addition
{
	private File additionFile;

	private boolean before;

	public Addition(String filePath, boolean before)
	{
		this.additionFile = new File(filePath);
		this.before = before;
	}

	public Addition(File additionFile, boolean before)
	{
		this.additionFile = additionFile;
		this.before = before;
	}

	public List<Map<String, String>> listAdditions() throws IOException
	{
		try (ClearThCsvReader reader = new ClearThCsvReader(new FileReader(additionFile), createCsvReaderConfig()))
		{
			String[] header = null;
			List<Map<String, String>> additionsList = new ArrayList<>();

			while (reader.hasNext())
			{
				if (reader.getRawRecord().contains("#"))
				{
					header = reader.getValues();
					continue;
				}

				if (header == null || header.length == 0)
					continue;

				Map<String, String> entry = new LinkedHashMap<>();
				String[] record = reader.getValues();
				int i = 0;

				for (String column : header)
					entry.put(column, record[i++]);

				additionsList.add(entry);
			}

			return additionsList;
		}
	}

	private ClearThCsvReaderConfig createCsvReaderConfig()
	{
		ClearThCsvReaderConfig config = new ClearThCsvReaderConfig();
		config.setWithTrim(true);
		config.setFirstLineAsHeader(false);
		return config;
	}

	public File getAdditionFile()
	{
		return additionFile;
	}

	public void setAdditionFile(File additionFile)
	{
		this.additionFile = additionFile;
	}

	public void setBefore(boolean before)
	{
		this.before = before;
	}

	public boolean isBefore()
	{
		return before;
	}
}
