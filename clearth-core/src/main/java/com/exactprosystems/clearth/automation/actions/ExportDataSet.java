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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.sql.ParametrizedQuery;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.tabledata.*;
import com.exactprosystems.clearth.utils.tabledata.readers.CsvDataReader;
import com.exactprosystems.clearth.utils.tabledata.readers.DbDataReader;
import com.exactprosystems.clearth.utils.tabledata.writers.CsvDataWriter;
import com.exactprosystems.clearth.utils.tabledata.writers.DbDataWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/*
	When SOURCE_FORMAT_PARAM is:
	QUERY - then SOURCE_PARAM contains query string and SRC_CON_NAME_PARAM is necessary;
	QUERYFILE - then SOURCE_PARAM contains query file path and SRC_CON_NAME_PARAM is necessary;
	CSV - then SOURCE_PARAM contains CSV file path and SRC_CON_NAME_PARAM is unnecessary.

	When DESTINATION_FORMAT_PARAM is:
	DB - then DESTINATION_PARAM contains table name for export and DST_CON_NAME_PARAM is necessary;
	CSV - then DESTINATION_PARAM contains CSV file path and DST_CON_NAME_PARAM is unnecessary;

	bufferSize - optional parameter. It contains number of line written at time
	
	CreateTableQuery or CreateTableQueryFile can be used to create destination DB table if it doesn't exist.
	If table doesn't exist and creation query isn't specified, ResultException will be thrown.
*/

public abstract class ExportDataSet extends Action
{
	public static final String SOURCE_FORMAT_PARAM = "SourceFormat",
			SOURCE_PARAM = "Source",
			SRC_CON_NAME_PARAM = "SrcConnectionName",
			DESTINATION_FORMAT_PARAM = "DestinationFormat",
			DESTINATION_PARAM = "Destination",
			DST_CON_NAME_PARAM = "DstConnectionName",
			CREATE_TABLE_QUERY_PARAM = "CreateTableQuery",
			CREATE_TABLE_QUERY_FILE_PARAM = "CreateTableQueryFile",
			BUFFER_SIZE_PARAM = "BufferSize";

	public static final int BUF_SIZE_DEF_VALUE = 100;

	protected GlobalContext globalContext;
	protected SrcFormat srcFormat;
	protected DstFormat dstFormat;
	protected String source, destination;
	protected String srcConnectionName, dstConnectionName;
	protected String createTableQuery;
	protected String createTableQueryFile;
	protected int bufferSize;
	protected Connection dstConnection, srcConnection;
	protected BasicTableDataReader<String, String, StringTableData> dataReader;
	protected TableDataWriter<String, String> dataWriter;

	protected void initParameters()
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		srcFormat = handler.getReqiuredEnum(SOURCE_FORMAT_PARAM, SrcFormat.class);
		source = handler.getRequiredString(SOURCE_PARAM);
		dstFormat = handler.getReqiuredEnum(DESTINATION_FORMAT_PARAM, DstFormat.class);
		destination = handler.getRequiredString(DESTINATION_PARAM);
		bufferSize = handler.getInteger(BUFFER_SIZE_PARAM, BUF_SIZE_DEF_VALUE);

		if (srcFormat == SrcFormat.QUERY || srcFormat == SrcFormat.QUERYFILE)
			srcConnectionName = handler.getRequiredString(SRC_CON_NAME_PARAM);
		if (dstFormat == DstFormat.DB)
		{
			dstConnectionName = handler.getRequiredString(DST_CON_NAME_PARAM);
			createTableQuery = handler.getString(CREATE_TABLE_QUERY_PARAM);
			createTableQueryFile = handler.getString(CREATE_TABLE_QUERY_FILE_PARAM);
		}
		handler.check();
	}

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		this.globalContext = globalContext;
		initParameters();

		try
		{
			dataReader = getDataReader();
			TableHeader<String> header;
			try
			{
				dataReader.start();
				header = dataReader.getTableData().getHeader();
			}
			catch (IOException e)
			{
				throw new ResultException("Error while getting data from reader", e);
			}

			dataWriter = getDataWriter(header);
			try
			{
				DataExporter<String, String, StringTableData> exporter =
						new DataExporter<>(dataReader, dataWriter, bufferSize, createTableRowConverter());
				exporter.export();
			}
			catch (IOException | InterruptedException e)
			{
				throw new ResultException("Error while exporting data", e);
			}
		}
		finally
		{
			closeResources();
		}
		return DefaultResult.passed("Export successfully done.");
	}

	protected void closeResources()
	{
		Utils.closeResource(dataReader);
		Utils.closeResource(dataWriter);
		if (isNeedCloseDbConnection())
		{
			Utils.closeResource(srcConnection);
			Utils.closeResource(dstConnection);
		}
	}

	protected TableDataWriter<String, String> getDataWriter(TableHeader<String> header)
	{
		switch (dstFormat)
		{
			case DB:
				return createDbDataWriter(header);
			case CSV:
				return createCsvDataWriter(header);
			default:
				throw new IllegalArgumentException("Illegal destination format");
		}
	}

	protected TableRowConverter<String, String> createTableRowConverter()
	{
		return null;
	}

	protected TableDataWriter<String, String> createCsvDataWriter(TableHeader<String> header)
	{
		File dstFile = new File(rootRelative(destination));
		try
		{
			return new CsvDataWriter(header, dstFile, true, false);
		}
		catch (IOException e)
		{
			throw new ResultException(format("Error while creating writer for CSV file '%s'", destination), e);
		}
	}

	protected TableDataWriter<String, String> createDbDataWriter(TableHeader<String> header)
	{
		try
		{
			dstConnection = getConnection(dstConnectionName);
		}
		catch (SQLException e)
		{
			throw new ResultException(format("Error while getting connection with name '%s'", dstConnectionName), e);
		}

		checkDestinationTable(dstConnection, destination);
		
		try
		{
			return new DbDataWriter(header, dstConnection, destination);
		}
		catch (SQLException e)
		{
			throw new ResultException(format("Error while creating writer for DB data. Connection: '%s'; Table name: '%s'",
											  dstConnectionName, destination), e);
		}
	}
	
	protected void checkDestinationTable(Connection connection, String tableName)
	{
		if (tableExists(connection, tableName))
			return;
		
		String createTableQuery = getCreateTableQuery();
		
		try (Statement statement = connection.createStatement())
		{
			statement.executeUpdate(createTableQuery);
		}
		catch (SQLException e)
		{
			throw ResultException.failed("Unable to create destination table.", e);
		}
	}
	
	protected boolean tableExists(Connection connection, String tableName)
	{
		try
		{
			return SQLUtils.tableExists(connection, tableName);
		}
		catch (SQLException e)
		{
			throw ResultException.failed(format("Error while checking if table '%s' exists.", tableName), e);
		}
	}
	
	protected String getCreateTableQuery()
	{
		if (isNotEmpty(createTableQuery))
			return createTableQuery;
		else if (isNotEmpty(createTableQueryFile))
		{
			try
			{
				return SQLUtils.loadQuery(rootRelative(createTableQueryFile));
			}
			catch (IOException e)
			{
				throw ResultException.failed(format("Error while loading query for table creation from file '%s'.",
						createTableQueryFile), e);
			}
		}
		else 
			throw ResultException.failed(format("Destination table doesn't exist. " +
					"Please specify '%s' or '%s' parameter to create table.",
					CREATE_TABLE_QUERY_PARAM, CREATE_TABLE_QUERY_FILE_PARAM));
	}

	protected BasicTableDataReader<String, String, StringTableData> getDataReader()
	{
		switch (srcFormat)
		{
			case QUERY:
			case QUERYFILE:
				return createDbDataReader();
			case CSV:
				return createCsvDataReader();
			default:
				throw new IllegalArgumentException("Illegal source format");
		}
	}

	protected CsvDataReader createCsvDataReader()
	{
		File srcFile = new File(rootRelative(source));
		try
		{
			return new CsvDataReader(srcFile);
		}
		catch (FileNotFoundException e)
		{
			throw new ResultException(format("Cannot read from file '%s': file not found", source), e);
		}
	}

	/**
	 * Override this method to return true in implementations with DB Connection Pool.
	 * Connection will be returned to pool by calling Connection.close().
	 *
	 * @return true if connection should be closed after using.
	 */
	protected boolean isNeedCloseDbConnection()
	{
		return false;
	}

	protected DbDataReader createDbDataReader()
	{
		try
		{
			srcConnection = getConnection(srcConnectionName);
		}
		catch (SQLException e)
		{
			throw new ResultException(format("Error while getting connection with name '%s'", srcConnectionName), e);
		}

		String query;
		try
		{
			query = srcFormat == SrcFormat.QUERY ? source : SQLUtils.loadQuery(rootRelative(source));
		}
		catch (IOException e)
		{
			throw new ResultException(format("Error while loading query from file '%s'", source), e);
		}

		ParametrizedQuery paramQuery;
		try
		{
			paramQuery = SQLUtils.parseSQLTemplate(query);
		}
		catch (SQLException e)
		{
			throw new ResultException("Error while parsing query", e);
		}


		PreparedStatement statement;
		try
		{
			statement = paramQuery.createPreparedStatement(srcConnection, inputParams);
		}
		catch (SQLException e)
		{
			throw new ResultException("Error while creating prepared statement", e);
		}

		return new DbDataReader(statement);
	}

	@Override
	public void dispose()
	{
		super.dispose();
		globalContext = null;
		source = null;
		destination = null;
		srcConnectionName = null;
		dstConnectionName = null;
		createTableQuery = null;
		createTableQueryFile = null;
		dstConnection = null;
		srcConnection = null;
		dataReader = null;
		dataWriter = null;
	}

	protected abstract Connection getConnection(String connectionName) throws SQLException;
}

enum SrcFormat
{
	QUERY,
	QUERYFILE,
	CSV
}

enum DstFormat
{
	DB,
	CSV
}