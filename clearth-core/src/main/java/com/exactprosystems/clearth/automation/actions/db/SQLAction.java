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

package com.exactprosystems.clearth.automation.actions.db;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.actions.db.checkers.DefaultRecordChecker;
import com.exactprosystems.clearth.automation.actions.db.checkers.RecordChecker;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.ResultSetProcessor;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings.ResultSetProcessorSettings;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.SaveToFileResultSetProcessor;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.SaveToContextResultSetProcessor;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings.SaveToContextRSProcessorSettings;
import com.exactprosystems.clearth.automation.actions.db.resultProcessors.settings.SaveToFileRSProcessorSettings;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.utils.*;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.inputparams.InputParamsUtils;
import com.exactprosystems.clearth.utils.sql.StubValueTransformer;
import com.exactprosystems.clearth.utils.sql.QueryTextProcessor;
import com.exactprosystems.clearth.utils.sql.SQLUtils;
import com.exactprosystems.clearth.utils.sql.conversion.ConversionSettings;
import com.exactprosystems.clearth.utils.sql.conversion.DBFieldMapping;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class SQLAction extends Action implements Preparable
{

	private GlobalContext globalContext;
	private MatrixContext matrixContext;
	private StepContext stepContext;

	protected IValueTransformer valueTransformer;
	protected ObjectToStringTransformer objectToStringTransformer;
	protected Map<String,String> queryParams;
	protected RecordChecker recordChecker;

	protected static final String SAVE_QUERY_RESULT = "SaveQueryResult",
			PARAM_FILE_DIR = "FileDir",
			PARAM_FILE_NAME = "FileName",
			PARAM_DELIMITER = "Delimiter",
			PARAM_GENERATE_IF_EMPTY = "GenerateIfEmpty",
			PARAM_USE_QUOTES = "UseQuotes",
			PARAM_COMPRESS_RESULT = "CompressResult",
			MAX_DISPLAYED_ROWS_COUNT = "MaxDisplayedRowsCount",
			QUERY_FILE = "QueryFile",
			QUERY = "Query",
			MAPPING_FILE = "MappingFile";

	public static final String OUT_QUERY_RESULT_PATH = "OutResultPath",
				OUT_ROWS_COUNT = "RowsCount",
				OUT_TABLE_DATA = "tabledata";

	protected File fileDir;
	protected String fileName;
	protected String delimiterString;
	protected boolean saveToFile;
	protected boolean generateIfEmpty;
	protected boolean useQuotes;
	protected boolean compressResult;
	protected int maxDisplayedRows;

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
		saveToFile = handler.getBoolean(SAVE_QUERY_RESULT, false);
		fileDir = handler.getFile(PARAM_FILE_DIR, ClearThCore.tempPath());
		fileName = handler.getString(PARAM_FILE_NAME);
		delimiterString = handler.getString(PARAM_DELIMITER, ",");
		generateIfEmpty = handler.getBoolean(PARAM_GENERATE_IF_EMPTY, false);
		useQuotes = handler.getBoolean(PARAM_USE_QUOTES, false);
		compressResult = handler.getBoolean(PARAM_COMPRESS_RESULT, false);
		maxDisplayedRows = handler.getInteger(MAX_DISPLAYED_ROWS_COUNT, 50);
		handler.check();

		String queryString = getQueryString();
		String queryFileName = getQueryFileName();

		if (StringUtils.isEmpty(queryString) && StringUtils.isEmpty(queryFileName))
			throw ResultException.failed(String.format("Unable to get query. #%s or #%s must be specified", QUERY, QUERY_FILE));

		delimiterString = delimiterString.replace("\\\\t", "\t");
		if (delimiterString.length() > 1)
			throw ResultException.failed("Invalid format of CSV values delimiter specified in parameter '" + PARAM_DELIMITER
					+ "': it should consist of only one character or string '\\\\t' (TAB sign).");
	}

	protected String getQuery() throws Exception
	{
		String query = getQueryString();

		if(StringUtils.isEmpty(query))
		{
			String queryFileName = getQueryFileName();
			query = globalContext.getLoadedContext(queryFileName);

			if (StringUtils.isEmpty(query))
			{
				query = loadQueryFromFile(queryFileName);
				globalContext.setLoadedContext(queryFileName, query);
			}
		}
		
		QueryTextProcessor queryPreprocessor = getQueryPreprocessor();
		if (queryPreprocessor != null)
			query = queryPreprocessor.process(query);
		
		return query;
	}
	
	protected QueryTextProcessor getQueryPreprocessor()
	{
		return null;
	}

	protected Result processQueryResult(ResultSet resultSet) throws SQLException
	{
		return processQueryResult(resultSet, -1);
	}

	protected Result processQueryResult(ResultSet resultSet, int limit) throws SQLException
	{
		boolean hasNext = resultSet.next();
		if (!hasNext && !generateIfEmpty)
			return DefaultResult.passed("Query returned empty table data result. Nothing to process");

		TableResult result = new TableResult("Table rows from result of the query", null, false);

		try(ResultSetProcessor processor = getResultSetProcessor(result))
		{
			processor.processHeader(resultSet, getVerificationMapping());
			int recordsCount = processor.processRecords(resultSet, limit);

			Map<String, String> outputParams = processor.getOutputParams();
			addOutputParams(outputParams);

			result.appendComment(String.format("Query has been successfully executed and returned %d columns and %d rows."
					+ Utils.EOL, result.getColumns().size(), recordsCount));

			return result;
		}
		catch (Exception e)
		{
			return DefaultResult.failed("Couldn't process writing query result to the output file.", e);
		}
	}

	protected void addOutputParams(Map<String, String> outputParams)
	{
		for (Map.Entry<String, String> entry : outputParams.entrySet())
			addOutputParam(entry.getKey(), entry.getValue());
	}

	protected Map<String, String> getQueryParams()
	{
		return queryParams;
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

	protected String getMappingPath()
	{
		return InputParamsUtils.getStringOrDefault(inputParams, MAPPING_FILE, null);
	}

	protected String getQueryFileName()
	{
		return getInputParam(QUERY_FILE);
	}

	protected String getQueryString()
	{
		return InputParamsUtils.getStringOrDefault(inputParams, QUERY, null);
	}

	protected List<DBFieldMapping> getVerificationMapping()
	{
		ConversionSettings conversionSettings = getConversionSettings();
		return conversionSettings != null ? conversionSettings.getMappings() : null;
	}

	protected ConversionSettings getConversionSettings()
	{
		String mappingPath = getMappingPath();
		ConversionSettings settings = globalContext.getLoadedContext(mappingPath);
		if (settings == null && StringUtils.isNotEmpty(mappingPath))
		{
			try
			{
				settings = loadConversionSettingsFromFile(mappingPath);
				globalContext.setLoadedContext(mappingPath, settings);
			}
			catch (IOException e)
			{
				throw ResultException.failed(String.format("Unable to load mapping '%s'", mappingPath), e);
			}
		}

		return settings;
	}

	protected ResultSetProcessor getResultSetProcessor(TableResult result) throws IOException
	{
		if (saveToFile)
			return createSaveToFileRSProcessor(result);

		return createSaveToContextRSProcessor(result);
	}

	protected ResultSetProcessor createSaveToFileRSProcessor(TableResult result) throws IOException
	{
		SaveToFileRSProcessorSettings fileRSProcessorSettings = new SaveToFileRSProcessorSettings();
		setProcessorCommonSettings(fileRSProcessorSettings, result);
		fileRSProcessorSettings.setCompressResult(compressResult);
		fileRSProcessorSettings.setFileDir(fileDir);
		fileRSProcessorSettings.setFileName(fileName);
		fileRSProcessorSettings.setDelimiter(delimiterString.charAt(0));
		fileRSProcessorSettings.setUseQuotes(useQuotes);
		fileRSProcessorSettings.setAppend(false);

		return new SaveToFileResultSetProcessor(fileRSProcessorSettings);
	}

	protected ResultSetProcessor createSaveToContextRSProcessor(TableResult result)
	{
		//this is necessary in order to be able to store multiple records and access them by index
		Map<String, Object> mvelVars = (Map<String, Object>) getMatrix().getMvelVars().get(idInMatrix);
		SaveToContextRSProcessorSettings contextRSProcessorSettings = new SaveToContextRSProcessorSettings();
		setProcessorCommonSettings(contextRSProcessorSettings, result);
		contextRSProcessorSettings.setMvelVars(mvelVars);

		return new SaveToContextResultSetProcessor(contextRSProcessorSettings);
	}

	protected void setProcessorCommonSettings(ResultSetProcessorSettings settings, TableResult result)
	{
		settings.setResult(result);
		settings.setConversionSettings(getConversionSettings());
		settings.setObjectToStringTransformer(objectToStringTransformer);
		settings.setValueTransformer(valueTransformer);
		settings.setMaxDisplayedRows(maxDisplayedRows);
		settings.setRecordChecker(recordChecker);
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
		return StubValueTransformer.getInstance();
	}

	protected RecordChecker createRecordChecker()
	{
		return new DefaultRecordChecker();
	}

	protected String loadQueryFromFile(String queryFileName) throws IOException
	{
		return SQLUtils.loadQuery(ClearThCore.rootRelative(queryFileName));
	}

	protected ConversionSettings loadConversionSettingsFromFile(String mappingPath) throws IOException
	{
		return ConversionSettings.loadFromCSVFile(new File(ClearThCore.rootRelative(mappingPath)));
	}

	@Override
	public void prepare(GlobalContext globalContext, SchedulerStatus status) throws Exception
	{
		String queryString = getQueryString();

		if (StringUtils.isEmpty(queryString))
		{
			String queryFileName = getQueryFileName();

			if (StringUtils.isEmpty(queryFileName))
			{
				//None of the #Query and #QueryFile parameters were specified.
				//Don't throw exception here to allow other actions to run. Instead of it will be thrown in init()
			}
			else
				globalContext.setLoadedContext(queryFileName, loadQueryFromFile(queryFileName));
		}

		String mappingPath = getMappingPath();
		if (StringUtils.isNotEmpty(mappingPath))
			globalContext.setLoadedContext(mappingPath, loadConversionSettingsFromFile(mappingPath));
	}
}

