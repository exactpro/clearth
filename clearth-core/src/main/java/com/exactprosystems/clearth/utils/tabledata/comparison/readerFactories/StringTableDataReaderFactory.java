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

package com.exactprosystems.clearth.utils.tabledata.comparison.readerFactories;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.utils.ClearThEnumUtils;
import com.exactprosystems.clearth.utils.LineBuilder;
import com.exactprosystems.clearth.utils.scripts.ScriptResult;
import com.exactprosystems.clearth.utils.scripts.ScriptUtils;
import com.exactprosystems.clearth.utils.sql.DefaultSQLValueTransformer;
import com.exactprosystems.clearth.utils.sql.ParametrizedQuery;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.tabledata.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.comparison.SourceType;
import com.exactprosystems.clearth.utils.tabledata.comparison.TableDataReaderSettings;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StringTableDataReaderFactory implements TableDataReaderFactory<String, String>
{
	@Override
	public BasicTableDataReader<String, String, ?> createTableDataReader(TableDataReaderSettings settings,
			Supplier<Connection> dbConnectionSupplier) throws Exception
	{
		switch (settings.getSourceType())
		{
			case DbQuery:
			case DbQueryFile:
				return createDbDataReader(settings, dbConnectionSupplier);
			case CsvFile:
				return createCsvDataReader(settings);
			case Script:
			case ScriptFile:
				return createScriptDataReader(settings);
			default:
				return createCustomTableDataReader(settings);
		}
	}
	
	protected BasicTableDataReader<String, String, ?> createCustomTableDataReader(TableDataReaderSettings settings) throws Exception
	{
		throw ResultException.failed("Unsupported format '" + settings.getSourceType() + "' has been used to initialize "
				+ (settings.isForExpectedData() ? "expected" : "actual") + " data reader. Acceptable ones are: "
				+ ClearThEnumUtils.enumToTextValues(SourceType.class).stream().collect(Collectors.joining("', '", "'", "'")) + ".");
	}
	
	
	protected DbDataReader createDbDataReader(TableDataReaderSettings settings,
			Supplier<Connection> dbConnectionSupplier) throws IOException, SQLException
	{
		String source = settings.getSourceData();
		ParametrizedQuery query = settings.getSourceType() == SourceType.DbQuery ? SQLUtils.parseSQLTemplate(source)
				: SQLUtils.parseSQLTemplate(new File(ClearThCore.rootRelative(source)));
		PreparedStatement statement = query.createPreparedStatement(dbConnectionSupplier.get(), settings.getSqlQueryParams());
		
		DbDataReader dbDataReader = new DbDataReader(statement);
		dbDataReader.setQueryDescription("for " + (settings.isForExpectedData() ? "expected" : "actual") + " data");
		dbDataReader.setValueTransformer(new DefaultSQLValueTransformer());
		return dbDataReader;
	}
	
	protected CsvDataReader createCsvDataReader(TableDataReaderSettings settings) throws IOException
	{
		CsvDataReader csvDataReader = new CsvDataReader(new BufferedReader(new FileReader(ClearThCore.rootRelative(settings.getSourceData()))));
		csvDataReader.setDelimiter(settings.getCsvDelimiter());
		return csvDataReader;
	}
	
	protected CsvDataReader createScriptDataReader(TableDataReaderSettings settings) throws IOException
	{
		boolean forExpectedData = settings.isForExpectedData();
		String source = settings.getSourceData(), scriptResult = settings.getSourceType() == SourceType.ScriptFile ?
				executeScriptFile(ClearThCore.rootRelative(source), settings.getScriptFileParams(), forExpectedData)
				: executeScriptCommands(source, settings.getShellName(), settings.getShellOption(), forExpectedData);
		
		CsvDataReader scriptResultReader = new CsvDataReader(new StringReader(scriptResult));
		scriptResultReader.setDelimiter(settings.getCsvDelimiter());
		return scriptResultReader;
	}
	
	
	protected String executeScriptFile(String scriptPath, String args, boolean forExpectedData) throws IOException
	{
		return processScriptResult(ScriptUtils.executeScript(scriptPath + " " + args, null), forExpectedData);
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
}
