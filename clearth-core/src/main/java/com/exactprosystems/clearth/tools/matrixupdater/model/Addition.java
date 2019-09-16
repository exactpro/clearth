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

package com.exactprosystems.clearth.tools.matrixupdater.model;

import com.csvreader.CsvReader;
import com.exactprosystems.clearth.utils.Utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		CsvReader reader = null;

		try
		{
			reader = new CsvReader(new FileReader(additionFile));
			reader.setTrimWhitespace(true);
			reader.readHeaders();

			String[] header = reader.getHeaders();

			List<Map<String, String>> additionsList = new ArrayList<Map<String, String>>();

			while (reader.readRecord())
			{
				if (reader.getRawRecord().contains("#"))
				{
					header = reader.getValues();
					reader.setHeaders(header);
					continue;
				}

				Map<String, String> entry = new LinkedHashMap<String, String>();

				for (String column : header)
					entry.put(column, reader.get(column));

				additionsList.add(entry);
			}

			return additionsList;
		}
		finally
		{
			Utils.closeResource(reader);
		}
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
