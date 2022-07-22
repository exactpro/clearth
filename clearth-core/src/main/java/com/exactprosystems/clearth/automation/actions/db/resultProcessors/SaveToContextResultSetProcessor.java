/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.actions.db.resultProcessors;

import com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings.SaveToContextRSProcessorSettings;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.exactprosystems.clearth.automation.actions.db.SQLAction.OUT_TABLE_DATA;

public class SaveToContextResultSetProcessor extends ResultSetProcessor
{
	protected final Map<String, Object> mvelVars;
	protected final List<Map<String, String>> records;
	protected int limit;
	protected boolean isRowWritten;

	public SaveToContextResultSetProcessor(SaveToContextRSProcessorSettings settings)
	{
		super(settings);
		this.mvelVars = settings.getMvelVars();
		this.isRowWritten = false;
		this.records = new ArrayList<>();
		this.mvelVars.put(OUT_TABLE_DATA, records);
	}

	@Override
	public int processRecords(ResultSet resultSet, int limit) throws SQLException, IOException
	{
		this.limit = limit;
		return super.processRecords(resultSet, limit);
	}

	@Override
	protected void processRecord(Map<String, String> record) throws IOException
	{
		if (limit == 1)
		{
			if (!isRowWritten)
			{
				outputParams.putAll(record);
				isRowWritten = true;
			}
		}
		else
			records.add(record);
	}

	@Override
	protected void processValues(Collection<String> values) throws IOException
	{

	}

	@Override
	public void close() throws Exception
	{

	}
}
