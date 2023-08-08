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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.actions.exportdata.DstFormat;
import com.exactprosystems.clearth.automation.actions.exportdata.SrcFormat;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.sql.ParametrizedQuery;
import com.exactprosystems.clearth.utils.sql.QueryTextProcessor;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.tabledata.DataExporter;
import com.exactprosystems.clearth.utils.tabledata.TableDataWriter;
import com.exactprosystems.clearth.utils.tabledata.TableHeader;
import com.exactprosystems.clearth.utils.tabledata.TableRowConverter;
import com.exactprosystems.clearth.utils.tabledata.readers.BasicTableDataReader;
import com.exactprosystems.clearth.utils.tabledata.typing.*;
import com.exactprosystems.clearth.utils.tabledata.typing.converter.DbTypesConverter;
import com.exactprosystems.clearth.utils.tabledata.typing.converter.GenericDbTypesConverter;
import com.exactprosystems.clearth.utils.tabledata.typing.converter.SqliteTypesConverter;
import com.exactprosystems.clearth.utils.tabledata.typing.reader.TypedCsvDataReader;
import com.exactprosystems.clearth.utils.tabledata.typing.reader.TypedDbDataReader;
import com.exactprosystems.clearth.utils.tabledata.typing.writer.TypedCsvDataWriter;
import com.exactprosystems.clearth.utils.tabledata.typing.writer.TypedDbDataWriter;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static com.exactprosystems.clearth.ClearThCore.rootRelative;
import static java.lang.String.format;

/*
	When SOURCE_FORMAT_PARAM is:
	QUERY - then SOURCE_PARAM contains query string and SRC_CON_NAME_PARAM is necessary;
	QUERYFILE - then SOURCE_PARAM contains query file path and SRC_CON_NAME_PARAM is necessary;
	CSV - then SOURCE_PARAM contains CSV file path and SRC_CON_NAME_PARAM is unnecessary.

	When DESTINATION_FORMAT_PARAM is:
	DB - then DESTINATION_PARAM contains table name for export and DST_CON_NAME_PARAM is necessary;
	CSV - then DESTINATION_PARAM contains CSV file path and DST_CON_NAME_PARAM is unnecessary;

	bufferSize - optional parameter. It contains number of line written at time
*/

public abstract class ExportDataSet extends Action
{
	public static final String SOURCE_FORMAT_PARAM = "SourceFormat",
			SOURCE_PARAM = "Source",
			SRC_CON_NAME_PARAM = "SrcConnectionName",
			DESTINATION_FORMAT_PARAM = "DestinationFormat",
			DESTINATION_PARAM = "Destination",
			DST_CON_NAME_PARAM = "DstConnectionName",
			MULTI_PARAMS_DELIMITER = "MultiParamsDelimiter",
			BUFFER_SIZE_PARAM = "BufferSize";

	public static final int BUF_SIZE_DEF_VALUE = 100;

	protected GlobalContext globalContext;
	protected SrcFormat srcFormat;
	protected String customSrcFormat; 
	protected DstFormat dstFormat;
	protected String customDstFormat;
	protected String source, destination, multiParamsDelimiter;
	protected String srcConnectionName, dstConnectionName;
	protected int bufferSize;
	protected Connection dstConnection, srcConnection;
	protected BasicTableDataReader<TypedTableHeaderItem, Object, TypedTableData> dataReader;
	protected TableDataWriter<TypedTableHeaderItem, Object> dataWriter;

	
	protected abstract Connection getConnection(String connectionName) throws SQLException;
	
	protected abstract SqlSyntax getSqlSyntax(Connection connection) throws SQLException;

	
	protected void initCustomParameters(@SuppressWarnings("unused") InputParamsHandler handler)
	{
		/*Nothing to init by default*/
	}

	protected QueryTextProcessor getQueryPreprocessor()
	{
		return null;
	}

	protected BasicTableDataReader<TypedTableHeaderItem, Object, TypedTableData> createDbDataReader(PreparedStatement statement, 
																									DbTypesConverter dbTypesConverter)
	{
		return new TypedDbDataReader(statement, dbTypesConverter);
	}
	
	protected BasicTableDataReader<TypedTableHeaderItem, Object, TypedTableData> getCustomDataReader(String format)
	{
		throw ResultException.failed(format("Unsupported %s = '%s'.", SOURCE_FORMAT_PARAM, format));
	}

	protected TableDataWriter<TypedTableHeaderItem, Object> createDbDataWriter(TableHeader<TypedTableHeaderItem> header, 
	                                                                           Connection connection, String tableName)
			throws SQLException
	{
		return new TypedDbDataWriter(header, connection, tableName);
	}

	protected TableDataWriter<TypedTableHeaderItem, Object> getCustomDataWriter(@SuppressWarnings("unused") TableHeader<TypedTableHeaderItem> header,
	                                                                            String format)
	{
		throw ResultException.failed(format("Unsupported %s = '%s'.", DESTINATION_FORMAT_PARAM, format));
	}
	
	protected CreateTableQueryGenerator getCreateTableQueryGenerator(Connection connection) throws SQLException
	{
		return new DefaultCreateTableQueryGenerator(getSqlSyntax(connection));
	}
	
	protected DataExporter<TypedTableHeaderItem, Object, TypedTableData> createDataExporter()
	{
		return new DataExporter<>(dataReader, dataWriter, bufferSize, createTableRowConverter());
	}
	
	protected TableRowConverter<TypedTableHeaderItem, Object> createTableRowConverter()
	{
		return null;
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
	

	private void initParameters()
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		srcFormat = handler.getEnumOrDefault(SOURCE_FORMAT_PARAM, SrcFormat.class, SrcFormat.CUSTOM);
		if (srcFormat == SrcFormat.CUSTOM)
			customSrcFormat = handler.getRequiredString(SOURCE_FORMAT_PARAM);
		source = handler.getRequiredString(SOURCE_PARAM);
		dstFormat = handler.getEnumOrDefault(DESTINATION_FORMAT_PARAM, DstFormat.class, DstFormat.CUSTOM);
		if (dstFormat == DstFormat.CUSTOM)
			customDstFormat = handler.getRequiredString(DESTINATION_FORMAT_PARAM);
		destination = handler.getRequiredString(DESTINATION_PARAM);
		bufferSize = handler.getInteger(BUFFER_SIZE_PARAM, BUF_SIZE_DEF_VALUE);
		multiParamsDelimiter = handler.getString(MULTI_PARAMS_DELIMITER, ",");

		if (srcFormat == SrcFormat.QUERY || srcFormat == SrcFormat.QUERYFILE)
			srcConnectionName = handler.getRequiredString(SRC_CON_NAME_PARAM);
		
		if (dstFormat == DstFormat.DB)
			dstConnectionName = handler.getRequiredString(DST_CON_NAME_PARAM);
		
		initCustomParameters(handler);
		handler.check();
	}

	@Override
	protected final Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
		this.globalContext = globalContext;
		initParameters();
		
		try
		{
			dataReader = getDataReader();
			TypedTableHeader header;
			try
			{
				dataReader.start();
				header = (TypedTableHeader) dataReader.getTableData().getHeader();
			}
			catch (IOException e)
			{
				throw new ResultException("Error while getting data from reader", e);
			}

			dataWriter = getDataWriter(header);
			try
			{
				DataExporter<TypedTableHeaderItem, Object, TypedTableData> exporter = createDataExporter();
				exporter.export();
				return DefaultResult.passed("Export successfully done. "+exporter.getRowCounter()+" row(s) exported.");
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

	private void createTableIfNotExists(TableHeader<TypedTableHeaderItem> header, Connection connection, String tableName)
			throws SQLException
	{
		if (SQLUtils.tableExists(connection, tableName))
			return;

		CreateTableQueryGenerator queryGenerator = getCreateTableQueryGenerator(connection);
		String query = queryGenerator.generateQuery(header, tableName);
		logger.debug("Query to create destination table:{}{}", Utils.EOL, query);
		
		try (Statement statement = connection.createStatement())
		{
			statement.executeUpdate(query);
		}
	}

	private TableDataWriter<TypedTableHeaderItem, Object> getDataWriter(TableHeader<TypedTableHeaderItem> header)
	{
		switch (dstFormat)
		{
			case DB:
				return createDbDataWriter(header);
			case CSV:
				return createCsvDataWriter(header);
			default:
				return getCustomDataWriter(header, customDstFormat);
		}
	}

	protected TableDataWriter<TypedTableHeaderItem, Object> createCsvDataWriter(TableHeader<TypedTableHeaderItem> header)
	{
		File dstFile = new File(rootRelative(destination));
		try
		{
			return new TypedCsvDataWriter(header, dstFile, true, false);
		}
		catch (IOException e)
		{
			throw new ResultException(format("Error while creating writer for CSV file '%s'", destination), e);
		}
	}

	private TableDataWriter<TypedTableHeaderItem, Object> createDbDataWriter(TableHeader<TypedTableHeaderItem> header)
	{
		try
		{
			dstConnection = getConnection(dstConnectionName);
		}
		catch (SQLException e)
		{
			throw new ResultException(format("Error while getting connection with name '%s'", dstConnectionName), e);
		}

		try
		{
			createTableIfNotExists(header, dstConnection, destination);
		}
		catch (SQLException e)
		{
			throw new ResultException(
					format("Error while creating table: '%s', connection: '%s'", destination, dstConnectionName), e);
		}

		try
		{
			return createDbDataWriter(header, dstConnection, destination);
		}
		catch (SQLException e)
		{
			throw new ResultException(format("Error while creating writer for DB data. Connection: '%s'; Table name: '%s'",
											  dstConnectionName, destination), e);
		}
	}
	
	private BasicTableDataReader<TypedTableHeaderItem, Object, TypedTableData> getDataReader()
	{
		switch (srcFormat)
		{
			case QUERY:
			case QUERYFILE:
				return createDbDataReader();
			case CSV:
				return createCsvDataReader();
			default:
				return getCustomDataReader(customSrcFormat);
		}
	}

	protected BasicTableDataReader<TypedTableHeaderItem, Object, TypedTableData> createCsvDataReader()
	{
		File srcFile = new File(rootRelative(source));
		try
		{
			return new TypedCsvDataReader(srcFile);
		}
		catch (IOException e)
		{
			throw new ResultException(format("Cannot read from file '%s': file not found", source), e);
		}
	}
	
	private BasicTableDataReader<TypedTableHeaderItem, Object, TypedTableData> createDbDataReader()
	{
		DbTypesConverter dbTypesConverter;
		try
		{
			srcConnection = getConnection(srcConnectionName);
			dbTypesConverter = getDbTypesConverter(srcConnection);
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

		PreparedStatement statement;
		try
		{
			ParametrizedQuery paramQuery = SQLUtils.parseSQLTemplate(query, multiParamsDelimiter, getQueryPreprocessor());
			statement = paramQuery.createPreparedStatement(srcConnection, inputParams);
		}
		catch (SQLException e)
		{
			throw new ResultException("Error while creating prepared statement", e);
		}

		return createDbDataReader(statement, dbTypesConverter);
	}

	protected DbTypesConverter getDbTypesConverter(Connection connection) throws SQLException
	{
		String url = connection.getMetaData().getURL();
		if (StringUtils.startsWith(url,"jdbc:sqlite"))
			return new SqliteTypesConverter();

		return new GenericDbTypesConverter();
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
		dstConnection = null;
		srcConnection = null;
		dataReader = null;
		dataWriter = null;
	}
}