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

package com.exactprosystems.clearth.automation.actions.db;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.Preparable;
import com.exactprosystems.clearth.automation.SchedulerStatus;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMapping;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.exactprosystems.clearth.utils.sql.SQLUtils.loadVerificationMapping;
import static com.exactprosystems.clearth.utils.sql.SQLUtils.resultSetToTable;

public abstract class SingleSelectSQLAction extends SelectSQLAction implements Preparable
{
	protected IValueTransformer dbValueTransformer = createDbValueTransformer();

	protected IValueTransformer createDbValueTransformer()
	{
		return null;
	}
	
	protected String getOutputMappingName()
	{
		return getName() + MAPPING;
	}
	
	protected String getOutputMappingFileName()
	{
		return getInputParam(MAPPING_FILE);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<DBFieldMapping> getVerificationMapping()
	{
		// Actually this action doesn't do any verification
		return (List<DBFieldMapping>) getGlobalContext().getLoadedContext(getOutputMappingName());
	}

	@Override
	protected String getQueryName()
	{
		return getName() + QUERY;
	}

	@Override
	protected String getQueryFileName()
	{
		return getInputParam(QUERY_FILE);
	}

	@Override
	protected Result processResultSet(ResultSet rs, String[] keys) throws ResultException
	{
		try
		{
			List<DBFieldMapping> mapping = getVerificationMapping();
			
			List<Map<String, String>> data = resultSetToTable(rs, mapping, dbValueTransformer);
			if (CollectionUtils.isEmpty(data))
				return DefaultResult.failed("The query returned empty result");
			
			Map<String, String> record = data.get(0);
			saveOutputParams(record);
			
			Result result = new DefaultResult();
			checkRecord(result, record, mapping);
			
			if (data.size() > 1)
				processExtraRecords(result, data);
			
			return result;
		}
		catch (SQLException e)
		{
			return DefaultResult.failed("An error occurred while processing the query's result", e);
		}
	}
	
	protected void saveOutputParams(Map<String, String> dbRecord)
	{
		if (dbRecord instanceof LinkedHashMap)
			setOutputParams((LinkedHashMap<String, String>) dbRecord);
		else 
			setOutputParams(new LinkedHashMap<String, String>(dbRecord));
	}
	
	protected void checkRecord(Result result, Map<String, String> record, List<DBFieldMapping> mapping)
	{
		List<DBFieldMapping> absentFields = null;
		for (DBFieldMapping fm : mapping)
		{
			if (!record.containsKey(fm.getSrcField()))
			{
				if (absentFields == null)
					absentFields = new ArrayList<DBFieldMapping>();
				absentFields.add(fm);
			}
		}
		if (absentFields != null)
		{
			result.setSuccess(false);
			CommaBuilder cb = new CommaBuilder();
			for (DBFieldMapping fm : absentFields)
			{
				cb.append(fm.getSrcField()).add(" (").add(fm.getDestField()).add(')');
			}
			result.appendComment(String.format("Query result doesn't contain the following fields: %s.", cb));
		}
	}
	
	protected void processExtraRecords(Result result, List<Map<String, String>> data)
	{
		result.appendComment(String.format("Query returned %d rows instead of one. " +
				"You can find extra rows in logs (level = info).", data.size()));
		if (logger.isInfoEnabled())
		{
			logger.info("Action {} returned extra rows:", getName());
			for (int i = 1; i < data.size(); i++)
			{
				logger.info("row {}: {}", i + 1, data.get(i));
			}
		}
	}

	@Override
	public void prepare(GlobalContext globalContext, SchedulerStatus status) throws Exception
	{
		globalContext.setLoadedContext(getQueryName(), SQLUtils.loadQuery(ClearThCore.rootRelative(getQueryFileName())));
		globalContext.setLoadedContext(getOutputMappingName(),
				loadVerificationMapping(ClearThCore.rootRelative(getOutputMappingFileName())));
	}
}
