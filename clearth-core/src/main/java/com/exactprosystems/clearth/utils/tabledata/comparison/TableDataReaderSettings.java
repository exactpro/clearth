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

package com.exactprosystems.clearth.utils.tabledata.comparison;

import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import com.exactprosystems.clearth.utils.inputparams.ParametersHandler;
import com.exactprosystems.clearth.utils.sql.DbConnectionSupplier;

import java.util.HashMap;
import java.util.Map;

public class TableDataReaderSettings
{
	public static final String SOURCE_TYPE = "Format", SOURCE_DATA = "Source",
			COMMON_PARAM = "Common", EXPECTED_PARAM = "Expected", ACTUAL_PARAM = "Actual",
			
			CSV_DELIMITER = "CsvDelimiter", SCRIPT_FILE_PARAMS = "ScriptFileParams",
			SCRIPT_SHELL_NAME = "ScriptShellName", SCRIPT_SHELL_OPTION = "ScriptShellOption",
			EXPECTED_CONNECTION_PARAM_NAME = "ExpectedConnectionName",
			ACTUAL_CONNECTION_PARAM_NAME = "ActualConnectionName";
	
	// Default parameters which should be available for all types of readers
	protected final boolean forExpectedData;
	protected String sourceType, sourceData;
	
	// Source type dependent readers' parameters
	protected char csvDelimiter;
	protected String scriptFileParams, shellName, shellOption;
	protected Map<String, String> sqlQueryParams;
	protected DbConnectionSupplier dbConnectionSupplier;
	
	public TableDataReaderSettings(Map<String, String> params, boolean forExpectedData, DbConnectionSupplier dbConnectionSupplier)
			throws ParametersException
	{
		this.forExpectedData = forExpectedData;
		sqlQueryParams = new HashMap<>(params);
		this.dbConnectionSupplier = dbConnectionSupplier;
		
		ParametersHandler handler = new ParametersHandler(params);
		loadSettings(handler);
		handler.check();
	}

	public DbConnectionSupplier getDbConnectionSupplier()
	{
		return dbConnectionSupplier;
	}

	public boolean isNeedCloseDbConnection()
	{
		return false;
	}

	protected void loadSettings(ParametersHandler handler) throws ParametersException
	{
		sourceType = handler.getRequiredString((forExpectedData ? EXPECTED_PARAM : ACTUAL_PARAM) + SOURCE_TYPE);
		if (isSourceDataRequired(sourceType))
			sourceData = handler.getRequiredString((forExpectedData ? EXPECTED_PARAM : ACTUAL_PARAM) + SOURCE_DATA);

		
		// Next properties are custom ones for certain types of table data readers.
		// It could be applied for expected and actual readers separately
		// or for both at once (depending on parameter suffix: 'Expected', 'Actual' or 'Common').
		// Common property has higher priority than others and should be applied to setting anyway if found
		
		String csvDelimiterString = handler.getString(CSV_DELIMITER + COMMON_PARAM,
				handler.getString(CSV_DELIMITER + (forExpectedData ? EXPECTED_PARAM : ACTUAL_PARAM), ",")).replace("\\\\t", "\t");
		if (csvDelimiterString.length() != 1)
		{
			throw new ParametersException("Specified CSV delimiter '" + csvDelimiterString
					+ "' has invalid format: it should be 1 character in length or represent TAB sign by '\\\\t'.");
		}
		csvDelimiter = csvDelimiterString.charAt(0);
		
		scriptFileParams = handler.getString(SCRIPT_FILE_PARAMS + COMMON_PARAM,
				handler.getString(SCRIPT_FILE_PARAMS + (forExpectedData ? EXPECTED_PARAM : ACTUAL_PARAM), ""));
		shellName = handler.getString(SCRIPT_SHELL_NAME + COMMON_PARAM,
				handler.getString(SCRIPT_SHELL_NAME + (forExpectedData ? EXPECTED_PARAM : ACTUAL_PARAM), "bash"));
		shellOption = handler.getString(SCRIPT_SHELL_OPTION + COMMON_PARAM,
				handler.getString(SCRIPT_SHELL_OPTION + (forExpectedData ? EXPECTED_PARAM : ACTUAL_PARAM), "-c"));
	}

	public String getDbConName()
	{
		return InputParamsUtils.getRequiredString(getSqlQueryParams(),
				isForExpectedData() ? EXPECTED_CONNECTION_PARAM_NAME : ACTUAL_CONNECTION_PARAM_NAME);
	}

	protected boolean isSourceDataRequired(String sourceType) {
		return true;
	}

	public boolean isForExpectedData()
	{
		return forExpectedData;
	}
	
	public String getSourceType()
	{
		return sourceType;
	}
	
	public String getSourceData()
	{
		return sourceData;
	}
	
	public char getCsvDelimiter()
	{
		return csvDelimiter;
	}
	
	public String getScriptFileParams()
	{
		return scriptFileParams;
	}
	
	public String getShellName()
	{
		return shellName;
	}
	
	public String getShellOption()
	{
		return shellOption;
	}
	
	public Map<String, String> getSqlQueryParams()
	{
		return sqlQueryParams;
	}
}
