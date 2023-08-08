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

package com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.utils.IValueTransformer;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.utils.csv.readers.ClearThCsvReaderConfig;
import com.exactprosystems.clearth.utils.scripts.ScriptResult;
import com.exactprosystems.clearth.utils.scripts.ScriptUtils;
import com.exactprosystems.clearth.utils.sql.DbConnectionSupplier;
import com.exactprosystems.clearth.utils.sql.ParametrizedQuery;
import com.exactprosystems.clearth.utils.sql.QueryTextProcessor;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.tabledata.TableDataException;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;
import com.exactprosystems.clearth.utils.tabledata.readers.AbstractCsvDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringTableDataReaderFactory implements TableDataReaderFactory<String, String>
{
	public static final String DB_QUERY = "Query", DB_QUERY_FILE = "QueryFile",
			CSV_FILE = "CsvFile", SCRIPT = "Script", SCRIPT_FILE = "ScriptFile";
	private final List<String> availableSourceTypes = new ArrayList<>(Arrays.asList(DB_QUERY, DB_QUERY_FILE, CSV_FILE, SCRIPT, SCRIPT_FILE));
	
	@Override
	public BasicTableDataReader<String, String, ?> createTableDataReader(TableDataReaderSettings settings)
					throws TableDataException
	{
		try
		{
			String sourceType = settings.getSourceType();
			if (sourceType.equalsIgnoreCase(DB_QUERY) || sourceType.equalsIgnoreCase(DB_QUERY_FILE))
				return createDbDataReader(settings);
			else if (sourceType.equalsIgnoreCase(CSV_FILE))
				return createCsvDataReader(settings);
			else if (sourceType.equalsIgnoreCase(SCRIPT) || sourceType.equalsIgnoreCase(SCRIPT_FILE))
				return createScriptDataReader(settings);
			else
				return createCustomTableDataReader(settings);
		}
		catch (Exception e)
		{
			throw new TableDataException("Couldn't create " + (settings.isForExpectedData() ?
					"expected" : "actual") + " table data reader.", e);
		}
	}
	
	protected BasicTableDataReader<String, String, ?> createCustomTableDataReader(TableDataReaderSettings settings) throws Exception
	{
		throw new IllegalArgumentException("Unsupported format '" + settings.getSourceType() + "' has been used to initialize "
				+ (settings.isForExpectedData() ? "expected" : "actual") + " data reader. Acceptable ones are: "
				+ getAvailableSourceTypes().stream().collect(Collectors.joining("', '", "'", "'")) + ".");
	}

	protected DbDataReader createDbDataReader(TableDataReaderSettings settings)
					throws IOException, SQLException, ConnectivityException, SettingsException
	{
		boolean forExpectedData = settings.isForExpectedData();
		String connectionName = settings.getDbConName();
		DbConnectionSupplier dbSupplier = settings.getDbConnectionSupplier();
		String source = settings.getSourceData();
		
		Connection connection = null;
		try
		{
			connection = dbSupplier.getConnection(connectionName);
			ParametrizedQuery query = settings.getSourceType().equalsIgnoreCase(DB_QUERY) ? SQLUtils.parseSQLTemplate(source,
				getQueryPreprocessor()) : SQLUtils.parseSQLTemplate(new File(ClearThCore.rootRelative(source)), getQueryPreprocessor());
			PreparedStatement statement = query.createPreparedStatement(connection, settings.getSqlQueryParams());
			
			return createDbDataReader(settings, statement, forExpectedData);
		}
		catch (Exception e)
		{
			if (connection != null && settings.isNeedCloseDbConnection())
			{
				try
				{
					connection.close();
				}
				catch (SQLException ex)
				{
					e.addSuppressed(ex);
				}
			}
			throw e;
		}
	}

	protected  DbDataReader createDbDataReader(TableDataReaderSettings settings, PreparedStatement statement, boolean forExpectedData)
	{
		DbDataReader dbDataReader = new DbDataReader(statement, settings.isNeedCloseDbConnection());
		dbDataReader.setQueryDescription("for " + (forExpectedData ? "expected" : "actual") + " data");
		dbDataReader.setValueTransformer(getValueTransformer());
		return dbDataReader;
	}
	
	protected IValueTransformer getValueTransformer()
	{
		return null;
	}

	protected CsvDataReader createCsvDataReader(TableDataReaderSettings settings) throws IOException
	{
		CsvDataReader csvDataReader = new CsvDataReader(new BufferedReader(new FileReader(ClearThCore
				.rootRelative(settings.getSourceData()))), createCsvReaderConfig(settings));
		return csvDataReader;
	}
	
	protected CsvDataReader createScriptDataReader(TableDataReaderSettings settings) throws IOException
	{
		boolean forExpectedData = settings.isForExpectedData();
		String source = settings.getSourceData(), scriptResult = settings.getSourceType().equalsIgnoreCase(SCRIPT_FILE) ?
				executeScriptFile(ClearThCore.rootRelative(source), settings.getScriptFileParams(), forExpectedData)
				: executeScriptCommands(source, settings.getShellName(), settings.getShellOption(), forExpectedData);
		
		CsvDataReader scriptResultReader = new CsvDataReader(new StringReader(scriptResult), createCsvReaderConfig(settings));
		return scriptResultReader;
	}

	protected ClearThCsvReaderConfig createCsvReaderConfig(TableDataReaderSettings settings)
	{
		ClearThCsvReaderConfig config = AbstractCsvDataReader.defaultCsvReaderConfig();
		config.setDelimiter(settings.getCsvDelimiter());
		return config;
	}
	
	protected String executeScriptFile(String scriptPath, String args, boolean forExpectedData) throws IOException
	{
		return processScriptResult(ScriptUtils.executeScript(scriptPath + " " + args), forExpectedData);
	}
	
	protected String executeScriptCommands(String commands, String shellName, String shellOption, boolean forExpectedData)
			throws IOException
	{
		return processScriptResult(ScriptUtils.executeScript(commands, shellName, shellOption, null, null), forExpectedData);
	}
	
	protected String processScriptResult(ScriptResult scriptResult, boolean forExpectedData)
	{
		if (scriptResult.result == 0)
			return scriptResult.outStr;
		
		LineBuilder builder = new LineBuilder();
		builder.add("Error occurred while executing ").add(forExpectedData ? "expected" : "actual").append(" script.");
		builder.add("Exit code: ").append(scriptResult.result);
		builder.add("Output: ").append(scriptResult.outStr);
		builder.add("Error text: ").append(scriptResult.errStr);
		throw ResultException.failed(builder.toString());
	}
	
	
	protected List<String> getAvailableSourceTypes()
	{
		return availableSourceTypes;
	}
	
	protected QueryTextProcessor getQueryPreprocessor()
	{
		return null;
	}
}
