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

package com.exactprosystems.clearth.automation.actions.db;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.actions.db.checkers.DefaultRecordChecker;
import com.exactprosystems.clearth.automation.actions.db.checkers.RecordChecker;
import com.exactprosystems.clearth.automation.actions.db.resultWriters.SqlResultWriter;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.report.results.DefaultTableResultDetail;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.utils.*;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import com.exactprosystems.clearth.utils.sql.DefaultSQLValueTransformer;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.sql.conversion.ConversionSettings;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMapping;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.exactprosystems.clearth.automation.actions.db.SelectSQLAction.MAPPING_FILE;

public abstract class SQLAction extends Action
{

	private GlobalContext globalContext;
	private MatrixContext matrixContext;
	private StepContext stepContext;

	protected IValueTransformer valueTransformer;
	protected ObjectToStringTransformer objectToStringTransformer;
	protected Map<String,String> queryParams;
	protected RecordChecker recordChecker;

	protected static final String SAVE_QUERY_RESULT = "SaveQueryResult",
			OUT_QUERY_RESULT_PATH = "OutResultPath",
			OUT_ROWS_COUNT = "RowsCount",
			PARAM_FILE_DIR = "FileDir",
			PARAM_FILE_NAME = "FileName",
			PARAM_DELIMITER = "Delimiter",
			PARAM_GENERATE_IF_EMPTY = "GenerateIfEmpty",
			PARAM_USE_QUOTES = "UseQuotes",
			PARAM_COMPRESS_RESULT = "CompressResult",
			MAX_DISPLAYED_ROWS_COUNT = "MaxDisplayedRowsCount";

	private File fileDir;
	private String fileName;
	private String delimiterString;
	private boolean generateIfEmpty;
	private boolean useQuotes;
	private boolean compressResult;
	private int maxDisplayedRows;

	@Override
	public Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws FailoverException
	{
		this.globalContext = globalContext;
		this.matrixContext = matrixContext;
		this.stepContext = stepContext;

		this.valueTransformer = createValueTransformer();
		this.recordChecker = createRecordChecker();

		queryParams = new LinkedHashMap<>(inputParams);

		try
		{
			init();

			return executeQuery();
		}
		catch (Exception e)
		{
			getLogger().error("Error while running action", e);
			if (SQLUtils.isConnectException(e))
				throw new FailoverException("Error while running action", e, FailoverReason.CONNECTION_ERROR, "database");
			return DefaultResult.failed(e);
		}
	}

	protected void init() throws IOException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		fileDir = handler.getFile(PARAM_FILE_DIR, ClearThCore.tempPath());
		fileName = handler.getString(PARAM_FILE_NAME);
		delimiterString = handler.getString(PARAM_DELIMITER, ",");
		generateIfEmpty = handler.getBoolean(PARAM_GENERATE_IF_EMPTY, false);
		useQuotes = handler.getBoolean(PARAM_USE_QUOTES, false);
		compressResult = handler.getBoolean(PARAM_COMPRESS_RESULT, false);
		maxDisplayedRows = handler.getInteger(MAX_DISPLAYED_ROWS_COUNT, 50);
		handler.check();

		delimiterString = delimiterString.replace("\\\\t", "\t");
		if (delimiterString.length() > 1)
			throw ResultException.failed("Invalid format of CSV values delimiter specified in parameter '" + PARAM_DELIMITER
					+ "': it should consist of only one character or string '\\\\t' (TAB sign).");
	}

	protected String getQuery() throws Exception
	{
		String query = getGlobalContext().getLoadedContext(getQueryName());

		if(query == null)
		{
			prepare();
			query = getGlobalContext().getLoadedContext(getQueryName());
		}

		return query;
	}

	protected Result writeQueryResult(ResultSet resultSet) throws SQLException
	{
		return writeQueryResult(resultSet, -1);
	}

	protected Result writeQueryResult(ResultSet resultSet, int limit) throws SQLException
	{
		if (!resultSet.next() && !generateIfEmpty)
			return DefaultResult.passed("Query returned empty table data result. Nothing to write.");

		try (SqlResultWriter sqlResultWriter = getSqlResultWriter())
		{
			ConversionSettings settings = getConversionSettings();

			TableResult result = new TableResult("Table rows from result of the query", null, false);
			writeHeader(resultSet, result, sqlResultWriter, settings);

			int recordsCount = 0;
			if (resultSet.getRow() != 0)
				recordsCount = writeRecords(result, resultSet, settings, sqlResultWriter, limit);

			File savedOutputFile = sqlResultWriter.getOutputFile();
			addOutputParams(savedOutputFile, recordsCount);

			result.appendComment(String.format("Query has been successfully executed and returned %d columns and %d rows."
							+ Utils.EOL + "Written file: '%s'", result.getColumns().size(), recordsCount, savedOutputFile));

			return result;
		}
		catch (IOException e)
		{
			return DefaultResult.failed("Couldn't process writing query result to the output file.", e);
		}
	}

	protected void addOutputParams(File outputFile, int rowsCount) throws IOException
	{
		addOutputParam(OUT_ROWS_COUNT, String.valueOf(rowsCount));
		addOutputParam(OUT_QUERY_RESULT_PATH, outputFile.toString());
	}

	protected void writeHeader(ResultSet resultSet, TableResult result, SqlResultWriter sqlResultWriter, ConversionSettings settings) throws SQLException, IOException
	{
		List<DBFieldMapping> mapping = getMapping();

		List<String> columns = SQLUtils.getColumnNames(resultSet.getMetaData());
		for (int i = 0; i < columns.size(); i++)
			columns.set(i, settings.getTableHeader(columns.get(i)));

		result.setColumns(columns);
		sqlResultWriter.writeHeader(columns);

		recordChecker.checkRecord(result, new HashSet<>(columns), mapping);
	}

	protected int writeRecords(TableResult result, ResultSet resultSet, ConversionSettings settings, SqlResultWriter sqlResultWriter, int limit) throws SQLException, IOException
	{
		int rowsCount = 0;
		do
		{
			rowsCount++;
			List<String> columns = result.getColumns();
			List<String> values = new LinkedList<>();
			for (String column : columns)
			{
				String value = getDbValue(resultSet, settings.getDBHeader(column));
				values.add(value);
			}
			sqlResultWriter.writeRecord(values);

			if (rowsCount <= maxDisplayedRows)
				result.addDetail(new DefaultTableResultDetail(values));

		}
		while (rowsCount != limit && resultSet.next());

		return rowsCount;
	}

	protected Map<String, String> getQueryParams()
	{
		return queryParams;
	}

	protected String getDbValue(ResultSet resultSet, String column) throws SQLException
	{
		String value = objectToStringTransformer != null ?
				SQLUtils.getDbValue(resultSet, column, objectToStringTransformer) : SQLUtils.getDbValue(resultSet, column);
		return valueTransformer != null ? valueTransformer.transform(value) : value;
	}

	protected abstract Result executeQuery() throws Exception;

	protected abstract Connection getDBConnection() throws Exception;

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

	protected boolean isNeedSaveToFile()
	{
		return InputParamsUtils.getBooleanOrDefault(inputParams, SAVE_QUERY_RESULT, false);
	}

	protected List<DBFieldMapping> getMapping() throws IOException
	{
		return SQLUtils.loadVerificationMapping(ClearThCore.rootRelative(getInputParam(MAPPING_FILE)));
	}

	protected ConversionSettings getConversionSettings() throws IOException
	{
		return ConversionSettings.loadFromCSVFile(new File(ClearThCore.rootRelative(getInputParam(MAPPING_FILE))));
	}


	protected SqlResultWriter getSqlResultWriter() throws IOException
	{
		return new SqlResultWriter(fileDir, fileName, compressResult, false, delimiterString.charAt(0), useQuotes);
	}

	protected File createOutputFile() throws IOException
	{
		Files.createDirectories(fileDir.toPath());
		return StringUtils.isEmpty(fileName) ?
				File.createTempFile(getClass().getSimpleName() + "_export_", ".csv", fileDir) : new File(fileDir, fileName);
	}

	@Override
	public int getActionType()
	{
		return ActionType.DB;
	}

	public GlobalContext getGlobalContext()
	{
		return globalContext;
	}
	
	public MatrixContext getMatrixContext()
	{
		return matrixContext;
	}

	public StepContext getStepContext()
	{
		return stepContext;
	}

	protected IValueTransformer createValueTransformer ()
	{
		return new DefaultSQLValueTransformer();
	}

	protected RecordChecker createRecordChecker()
	{
		return new DefaultRecordChecker();
	}

	protected void prepare() throws Exception
	{
		getGlobalContext().setLoadedContext(getQueryName(), SQLUtils.loadQuery(ClearThCore.rootRelative(getQueryFileName())));
	}

	protected abstract String getQueryName();
	protected abstract String getQueryFileName();
}

