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
			BUFFER_SIZE_PARAM = "BufferSize";

	public static final int BUF_SIZE_DEF_VALUE = 100;

	protected GlobalContext globalContext;
	protected SrcFormat srcFormat;
	protected DstFormat dstFormat;
	protected String source, destination;
	protected String srcConnectionName, dstConnectionName;
	protected int bufferSize;
	protected Connection dstConnection, srcConnection;
	protected BasicTableDataReader<String, String, StringTableData> dataReader;
	protected TableDataWriter<String, String> dataWriter;

	protected void initParameters()
	{
		getLogger().debug("Initializing special action parameters");
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		srcFormat = handler.getReqiuredEnum(SOURCE_FORMAT_PARAM, SrcFormat.class);
		source = handler.getRequiredString(SOURCE_PARAM);
		dstFormat = handler.getReqiuredEnum(DESTINATION_FORMAT_PARAM, DstFormat.class);
		destination = handler.getRequiredString(DESTINATION_PARAM);
		bufferSize = handler.getInteger(BUFFER_SIZE_PARAM, BUF_SIZE_DEF_VALUE);

		if (srcFormat == SrcFormat.QUERY || srcFormat == SrcFormat.QUERYFILE)
			srcConnectionName = handler.getRequiredString(SRC_CON_NAME_PARAM);
		if (dstFormat == DstFormat.DB)
			dstConnectionName = handler.getRequiredString(DST_CON_NAME_PARAM);
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
				String errMessage = "Error while getting data from reader";
				logger.error(errMessage, e);
				throw new ResultException(errMessage, e);
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
				String errMsg = "Error while exporting data";
				logger.error(errMsg, e);
				throw new ResultException(errMsg, e);
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
		File dstFile = new File(ClearThCore.rootRelative(destination));
		try
		{
			return new CsvDataWriter(header, dstFile, true, false);
		}
		catch (IOException e)
		{
			String errMessage = "Error while CsvDataWriter from file '"+destination+"' creation";
			logger.error(errMessage, e);
			throw new ResultException(errMessage, e);
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
			String errMessage = "Error on getting connection with name '" + dstConnectionName + "'";
			logger.error(errMessage, e);
			throw new ResultException(errMessage, e);
		}

		try
		{
			return new DbDataWriter(header, dstConnection, destination);
		}
		catch (SQLException e)
		{
			String errMessage = String.format("Error on DbDataWriter instance creation error. Connection: '%s'; Table name: '%s'",
											  dstConnectionName, destination);
			logger.error(errMessage, e);
			throw new ResultException(errMessage, e);
		}
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
		File srcFile = new File(ClearThCore.rootRelative(source));
		try
		{
			return new CsvDataReader(srcFile);
		}
		catch (FileNotFoundException e)
		{
			String errMessage = "Cannot read from file '"+source+"': file not found";
			logger.error(errMessage, e);
			throw new ResultException(errMessage, e);
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
			String errMessage = "Error while getting connection with name '" + srcConnectionName + "'";
			logger.error(errMessage, e);
			throw new ResultException(errMessage, e);
		}

		String query;
		try
		{
			query = srcFormat == SrcFormat.QUERY ? source : SQLUtils.loadQuery(ClearThCore.rootRelative(source));
		}
		catch (IOException e)
		{
			String errMessage = "Error while loading query from file '" + source + "'";
			logger.error(errMessage, e);
			throw new ResultException(errMessage, e);
		}

		ParametrizedQuery paramQuery;
		try
		{
			paramQuery = SQLUtils.parseSQLTemplate(query);
		}
		catch (SQLException e)
		{
			String errMessage = "Error while query parsing'";
			logger.error(errMessage, e);
			throw new ResultException(errMessage, e);
		}


		PreparedStatement statement;
		try
		{
			statement = paramQuery.createPreparedStatement(srcConnection, inputParams);
		}
		catch (SQLException e)
		{
			String errMessage = "Error while prepared statement creating";
			logger.error(errMessage, e);
			throw new ResultException(errMessage, e);
		}

		return new DbDataReader(statement);
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