/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.automation.persistence.db;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.persistence.ExecutorStateException;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateInfo;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateObjects;
import com.exactprosystems.clearth.automation.persistence.MatrixState;
import com.exactprosystems.clearth.automation.persistence.StepState;
import com.exactprosystems.clearth.utils.CollectionUtils;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import com.exactprosystems.clearth.xmldata.XmlReportsConfig;
import com.exactprosystems.clearth.automation.MatrixData;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.persistence.ActionState;

import static com.exactprosystems.clearth.automation.persistence.db.DbStateOperator.*;

public class DbStateSaver
{
	private static final Logger logger = LoggerFactory.getLogger(DbStateSaver.class);
	private static final String TYPE_TEXT = "text",
			TYPE_INT = "integer",
			TYPE_LONG = "long",
			TYPE_BOOLEAN = "bool",
			TYPE_BLOB = "blob";
	
	private final QueryHelper helper;
	private final Class[] allowedClasses;
	
	private PreparedStatement insertInfo,
			insertReport,
			insertFixedIds,
			insertStep,
			insertStepContext,
			insertMatrix,
			insertStepSuccess,
			insertStepStatusComments,
			insertAction;
	
	public DbStateSaver(QueryHelper helper, Class[] allowedClasses)
	{
		this.helper = helper;
		this.allowedClasses = allowedClasses;
	}
	
	
	public DbStateContext save(ExecutorStateInfo stateInfo, ExecutorStateObjects stateObjects) throws ExecutorStateException
	{
		dropTables();
		createTables();
		
		prepareQueries();
		
		int infoId = saveStateInfoProperties(stateInfo);
		DbStateContext context = new DbStateContext(infoId);
		
		saveMatricesReportsInfo(stateInfo.getReportsInfo().getMatrices(), context);
		saveSteps(stateInfo.getSteps(), context);
		saveFixedIds(stateObjects.getFixedIDs(), context.getInfoId());
		saveMatrices(stateObjects.getMatrices(), context);
		saveStepContexts(stateInfo.getSteps(), context);
		
		return context;
	}
	
	
	private void dropTables() throws ExecutorStateException
	{
		logger.debug("Dropping tables");
		
		String[] tables = {TABLE_MATRICES, TABLE_STEP_SUCCESS, TABLE_STEP_STATUS_COMMENTS, TABLE_ACTIONS,
				TABLE_STEPS, TABLE_STEP_CONTEXTS, TABLE_FIXED_IDS, TABLE_INFOS, TABLE_REPORTS};
		try
		{
			for (String t : tables)
			{
				helper.dropIndex(t);
				helper.dropTable(t);
			}
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Error while dropping tables", e);
		}
	}
	
	private void createTables() throws ExecutorStateException
	{
		logger.debug("Creating tables");
		
		try
		{
			helper.createTable(TABLE_MATRICES, new Pair<>(COLUMN_MATRIX_ID, TYPE_INT), CollectionUtils.map(COLUMN_INFO_ID, TYPE_INT,
					"fileName", TYPE_TEXT, "name", TYPE_TEXT, "description", TYPE_TEXT, "constants", TYPE_BLOB,
					"mvelVars", TYPE_BLOB,
					"started", TYPE_TEXT, "actionsDone", TYPE_INT, "successfulMatrix", TYPE_BOOLEAN,
					"matrixContext", TYPE_BLOB,
					"uploadDate", TYPE_TEXT, "execute", TYPE_BOOLEAN, "trimSpaces", TYPE_BOOLEAN, "link", TYPE_TEXT, "linkType", TYPE_TEXT, "autoReload", TYPE_BOOLEAN));
			helper.createIndex(TABLE_MATRICES, COLUMN_INFO_ID);
			
			helper.createTable(TABLE_STEP_SUCCESS, new Pair<>(COLUMN_RECORD_ID, TYPE_INT), CollectionUtils.map(COLUMN_MATRIX_ID, TYPE_INT, COLUMN_STEP_ID, TYPE_INT,
					"success", TYPE_BOOLEAN));
			helper.createIndex(TABLE_STEP_SUCCESS, COLUMN_MATRIX_ID);
			
			helper.createTable(TABLE_STEP_STATUS_COMMENTS, new Pair<>(COLUMN_RECORD_ID, TYPE_INT), CollectionUtils.map(COLUMN_MATRIX_ID, TYPE_INT, COLUMN_STEP_ID, TYPE_INT,
					"comments", TYPE_BLOB));
			helper.createIndex(TABLE_STEP_STATUS_COMMENTS, COLUMN_MATRIX_ID);
			
			helper.createTable(TABLE_ACTIONS, new Pair<>(COLUMN_ACTION_ID, TYPE_INT), CollectionUtils.map(COLUMN_MATRIX_ID, TYPE_INT,
					"actionClass", TYPE_TEXT,
					"matrixInputParams", TYPE_BLOB, "inputParams", TYPE_BLOB, "specialParams", TYPE_BLOB,
					"subActionsData", TYPE_BLOB,
					"idInMatrix", TYPE_TEXT, "comment", TYPE_TEXT, "name", TYPE_TEXT, COLUMN_STEP_ID, TYPE_INT, "idInTemplate", TYPE_TEXT,
					"executable", TYPE_BOOLEAN, "inverted", TYPE_BOOLEAN,
					"done", TYPE_BOOLEAN, "passed", TYPE_BOOLEAN, "suspendIfFailed", TYPE_BOOLEAN,
					"formulaExecutable", TYPE_TEXT, "formulaInverted", TYPE_TEXT, "formulaComment", TYPE_TEXT, "formulaTimeout", TYPE_TEXT, "formulaIdInTemplate", TYPE_TEXT,
					"timeout", TYPE_LONG,
					"result", TYPE_BLOB,
					"started", TYPE_TEXT, "finished", TYPE_TEXT));
			helper.createIndex(TABLE_ACTIONS, COLUMN_MATRIX_ID);
			
			helper.createTable(TABLE_STEPS, new Pair<>(COLUMN_STEP_ID, TYPE_INT), CollectionUtils.map(COLUMN_INFO_ID, TYPE_INT,
					"name", TYPE_TEXT, "kind", TYPE_TEXT, "startAt", TYPE_TEXT, "parameter", TYPE_TEXT,
					"askForContinue", TYPE_BOOLEAN, "askIfFailed", TYPE_BOOLEAN, "execute", TYPE_BOOLEAN,
					"startAtType", TYPE_TEXT, "waitNextDay", TYPE_BOOLEAN,
					"comment", TYPE_TEXT,
					"started", TYPE_TEXT, "finished", TYPE_TEXT,
					"actionsSuccessful", TYPE_INT, "actionsDone", TYPE_INT, "anyActionFailed", TYPE_BOOLEAN, "failedDueToError", TYPE_BOOLEAN,
					"statusComment", TYPE_TEXT, "error", TYPE_BLOB));
			helper.createIndex(TABLE_STEPS, COLUMN_INFO_ID);
			
			helper.createTable(TABLE_STEP_CONTEXTS, new Pair<>(COLUMN_RECORD_ID, TYPE_INT), CollectionUtils.map(COLUMN_STEP_ID, TYPE_INT, COLUMN_MATRIX_ID, TYPE_INT,
					"context", TYPE_BLOB));
			helper.createIndex(TABLE_STEP_CONTEXTS, COLUMN_STEP_ID);
			
			helper.createTable(TABLE_FIXED_IDS, new Pair<>(COLUMN_RECORD_ID, TYPE_INT), CollectionUtils.map(COLUMN_INFO_ID, TYPE_INT, "fixedIds", TYPE_BLOB));
			helper.createIndex(TABLE_FIXED_IDS, COLUMN_INFO_ID);
			
			helper.createTable(TABLE_INFOS, new Pair<>(COLUMN_INFO_ID, TYPE_INT), CollectionUtils.map("version", TYPE_TEXT,
					"weekendHoliday", TYPE_BOOLEAN, "holidays", TYPE_BLOB, "businessDay", TYPE_TEXT,
					"startedByUser", TYPE_TEXT, "started", TYPE_TEXT, "ended", TYPE_TEXT,
					"reportsPath", TYPE_TEXT, "actionReportsPath", TYPE_TEXT,
					"completeHtmlReport", TYPE_BOOLEAN, "failedHtmlReport", TYPE_BOOLEAN, "completeJsonReport", TYPE_BOOLEAN));
			
			//This table is filled with data about reports made. Such reports are created only when execution state is explicitly saved.
			//Thus, it is not updated after each action or global step
			helper.createTable(TABLE_REPORTS, new Pair<>(COLUMN_REPORT_ID, TYPE_INT), CollectionUtils.map(COLUMN_INFO_ID, TYPE_INT,
					"fileName", TYPE_TEXT, "name", TYPE_TEXT,
					"actionsDone", TYPE_INT, "successful", TYPE_BOOLEAN));
			helper.createIndex(TABLE_REPORTS, COLUMN_INFO_ID);
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Error while creating tables", e);
		}
	}
	
	protected void prepareQueries() throws ExecutorStateException
	{
		logger.debug("Preparing queries");
		
		try
		{
			insertInfo = helper.prepareInsert(TABLE_INFOS, Arrays.asList("version",
					"weekendHoliday", "holidays", "businessDay",
					"startedByUser", "started", "ended",
					"reportsPath", "actionReportsPath",
					"completeHtmlReport", "failedHtmlReport", "completeJsonReport"));
			
			insertReport = helper.prepareInsert(TABLE_REPORTS, Arrays.asList(COLUMN_INFO_ID, "fileName", "name", "actionsDone", "successful"));
			
			insertFixedIds = helper.prepareInsert(TABLE_FIXED_IDS, Arrays.asList(COLUMN_INFO_ID, "fixedIds"));
			
			insertStep = helper.prepareInsert(TABLE_STEPS, Arrays.asList(COLUMN_INFO_ID,
					"name", "kind", "startAt", "parameter",
					"askForContinue", "askIfFailed", "execute",
					"startAtType", "waitNextDay",
					"comment",
					"started", "finished",
					"actionsSuccessful", "actionsDone", "anyActionFailed", "failedDueToError",
					"statusComment", "error"));
			
			insertStepContext = helper.prepareInsert(TABLE_STEP_CONTEXTS, Arrays.asList(COLUMN_STEP_ID, COLUMN_MATRIX_ID, "context"));
			
			insertMatrix = helper.prepareInsert(TABLE_MATRICES, Arrays.asList(COLUMN_INFO_ID, "fileName", "name", "description", "constants",
					"mvelVars",
					"started", "actionsDone", "successfulMatrix",
					"matrixContext",
					"uploadDate", "execute", "trimSpaces", "link", "linkType", "autoReload"));
			
			insertStepSuccess = helper.prepareInsert(TABLE_STEP_SUCCESS, Arrays.asList(COLUMN_MATRIX_ID, COLUMN_STEP_ID, "success"));
			insertStepStatusComments = helper.prepareInsert(TABLE_STEP_STATUS_COMMENTS, Arrays.asList(COLUMN_MATRIX_ID, COLUMN_STEP_ID, "comments"));
			
			insertAction = helper.prepareInsert(TABLE_ACTIONS, Arrays.asList(COLUMN_MATRIX_ID, "actionClass",
					"matrixInputParams", "inputParams", "specialParams", "subActionsData",
					"idInMatrix", "comment", "name", COLUMN_STEP_ID, "idInTemplate",
					"executable", "inverted", "done", "passed", "suspendIfFailed",
					"formulaExecutable", "formulaInverted", "formulaComment", "formulaTimeout", "formulaIdInTemplate",
					"timeout", "result", "started", "finished"));
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Error while preparing queries", e);
		}
	}
	
	private int saveStateInfoProperties(ExecutorStateInfo stateInfo) throws ExecutorStateException
	{
		logger.debug("Saving state info properties");
		
		try
		{
			DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
			ReportsInfo reportsInfo = stateInfo.getReportsInfo();
			XmlReportsConfig reportsConfig = reportsInfo.getXmlReportsConfig();
			
			QueryParameterSetter.newInstance(insertInfo)
					.setString("1.0")
					.setBoolean(stateInfo.isWeekendHoliday())
					.setBytes(DbStateUtils.saveToXml(stateInfo.getHolidays(), allowedClasses))
					.setString(DbStateUtils.formatTimestamp(stateInfo.getBusinessDay(), DbStateUtils.createDateFormat()))
					.setString(stateInfo.getStartedByUser())
					.setString(DbStateUtils.formatTimestamp(stateInfo.getStarted(), timestampFormat))
					.setString(DbStateUtils.formatTimestamp(stateInfo.getEnded(), timestampFormat))
					.setString(reportsInfo.getPath())
					.setString(reportsInfo.getActionReportsPath())
					.setBoolean(reportsConfig.isCompleteHtmlReport())
					.setBoolean(reportsConfig.isFailedHtmlReport())
					.setBoolean(reportsConfig.isCompleteJsonReport());
			
			return helper.insert(insertInfo);
		}
		catch (Exception e)
		{
			throw savingException("state info properties", e);
		}
	}
	
	private void saveMatricesReportsInfo(List<XmlMatrixInfo> matricesReports, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Saving matrices reports info");
		
		int infoId = context.getInfoId();
		for (XmlMatrixInfo mi: matricesReports)
			saveMatrixReportInfo(mi, infoId);
	}
	
	private void saveMatrixReportInfo(XmlMatrixInfo matrixReportInfo, int infoId) throws ExecutorStateException
	{
		try
		{
			QueryParameterSetter.newInstance(insertReport)
					.setInt(infoId)
					.setString(matrixReportInfo.getFileName())
					.setString(matrixReportInfo.getName())
					.setInt(matrixReportInfo.getActionsDone())
					.setBoolean(matrixReportInfo.isSuccessful());
			
			helper.insert(insertReport);
		}
		catch (Exception e)
		{
			throw savingException("report info of matrix '"+matrixReportInfo.getName()+"'", e);
		}
	}
	
	private void saveSteps(List<StepState> steps, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Saving steps");
		
		int infoId = context.getInfoId();
		for (StepState step : steps)
		{
			int stepId = saveStepProperties(step, infoId);
			String stepName = step.getName();
			context.setStepId(stepName, stepId);
		}
	}
	
	private int saveStepProperties(StepState step, int infoId) throws ExecutorStateException
	{
		try
		{
			DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
			QueryParameterSetter.newInstance(insertStep)
					.setInt(infoId)
					.setString(step.getName())
					.setString(step.getKind())
					.setString(step.getStartAt())
					.setString(step.getParameter())
					.setBoolean(step.isAskForContinue())
					.setBoolean(step.isAskIfFailed())
					.setBoolean(step.isExecute())
					.setString(step.getStartAtType().getStringType())
					.setBoolean(step.isWaitNextDay())
					.setString(step.getComment())
					.setString(DbStateUtils.formatTimestamp(step.getStarted(), timestampFormat))
					.setString(DbStateUtils.formatTimestamp(step.getFinished(), timestampFormat))
					.setInt(step.getExecutionProgress().getSuccessful())
					.setInt(step.getExecutionProgress().getDone())
					.setBoolean(step.hasAnyActionFailed())
					.setBoolean(step.isFailedDueToError())
					.setString(step.getStatusComment())
					.setBytes(DbStateUtils.saveToXml(step.getError(), allowedClasses));
			return helper.insert(insertStep);
		}
		catch (Exception e)
		{
			throw savingException("step '"+step.getName()+"'", e);
		}
	}
	
	private void saveFixedIds(Map<String, String> fixedIds, int infoId) throws ExecutorStateException
	{
		logger.debug("Saving fixed IDs");
		
		try
		{
			QueryParameterSetter.newInstance(insertFixedIds)
					.setInt(infoId)
					.setBytes(DbStateUtils.saveToXml(fixedIds, allowedClasses));
			insertFixedIds.execute();
		}
		catch (Exception e)
		{
			throw savingException("fixed IDs", e);
		}
	}
	
	private void saveMatrices(List<MatrixState> matrices, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Saving matrices");
		
		int infoId = context.getInfoId();
		DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
		for (MatrixState matrix : matrices)
		{
			int matrixId = saveMatrixProperties(matrix, timestampFormat, infoId);
			String matrixName = matrix.getName();
			context.setMatrixId(matrixName, matrixId);
			
			saveStepSuccess(matrix.getStepSuccess(), matrixName, matrixId, context);
			saveStepStatusComments(matrix.getStepStatusComments(), matrixName, matrixId, context);
			saveActions(matrix.getActions(), matrixName, matrixId, context);
		}
	}
	
	private int saveMatrixProperties(MatrixState matrix, DateFormat timestampFormat, int infoId) throws ExecutorStateException
	{
		try
		{
			MatrixData md = matrix.getMatrixData();
			
			QueryParameterSetter.newInstance(insertMatrix)
					.setInt(infoId)
					.setString(matrix.getFileName())
					.setString(matrix.getName())
					.setString(matrix.getDescription())
					.setBytes(DbStateUtils.saveToXml(matrix.getConstants(), allowedClasses))
					.setBytes(DbStateUtils.saveToXml(matrix.getMvelVars(), allowedClasses))
					.setString(DbStateUtils.formatTimestamp(matrix.getStarted(), DbStateUtils.createTimestampFormat()))
					.setInt(matrix.getActionsDone())
					.setBoolean(matrix.isSuccessful())
					.setBytes(DbStateUtils.saveToXml(matrix.getContext(), allowedClasses))
					.setString(DbStateUtils.formatTimestamp(md.getUploadDate(), timestampFormat))
					.setBoolean(md.isExecute())
					.setBoolean(md.isTrim())
					.setString(md.getLink())
					.setString(md.getType())
					.setBoolean(md.isAutoReload());
			
			return helper.insert(insertMatrix);
		}
		catch (Exception e)
		{
			throw savingException("matrix '"+matrix.getName()+"'", e);
		}
	}
	
	private void saveStepSuccess(Map<String, Boolean> stepSuccess, String matrixName, int matrixId, DbStateContext context) throws ExecutorStateException
	{
		try
		{
			for (Entry<String, Boolean> step : stepSuccess.entrySet())
			{
				int stepId = context.getStepId(step.getKey());
				saveStepSuccess(step.getValue(), matrixId, stepId, context);
			}
		}
		catch (Exception e)
		{
			throw savingException("step success", e);
		}
	}
	
	protected void saveStepSuccess(Boolean success, int matrixId, int stepId, DbStateContext context) throws SQLException
	{
		QueryParameterSetter.newInstance(insertStepSuccess)
				.setInt(matrixId)
				.setInt(stepId)
				.setBoolean(success);
		
		int id = helper.insert(insertStepSuccess);
		context.setStepSuccessId(matrixId, stepId, id);
	}
	
	private void saveStepStatusComments(Map<String, List<String>> stepStatusComments, String matrixName, int matrixId, DbStateContext context) throws ExecutorStateException
	{
		try
		{
			for (Entry<String, List<String>> step : stepStatusComments.entrySet())
			{
				int stepId = context.getStepId(step.getKey());
				saveStepStatusComments(step.getValue(), matrixId, stepId, context);
			}
		}
		catch (Exception e)
		{
			throw savingException("step status comments", e);
		}
	}
	
	protected void saveStepStatusComments(List<String> comments, int matrixId, int stepId, DbStateContext context) throws SQLException, IOException
	{
		QueryParameterSetter.newInstance(insertStepStatusComments)
				.setInt(matrixId)
				.setInt(stepId)
				.setBytes(DbStateUtils.saveToXml(comments, allowedClasses));
		
		int id = helper.insert(insertStepStatusComments);
		context.setStepStatusCommentsId(matrixId, stepId, id);
	}
	
	protected void saveActions(List<ActionState> actions, String matrixName, int matrixId, DbStateContext context) throws ExecutorStateException
	{
		final int maxActions = 1000;
		List<ActionReference> references = new ArrayList<>(maxActions+1);
		DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
		try
		{
			for (ActionState action : actions)
			{
				saveAction(action, matrixName, matrixId, context, timestampFormat, insertAction);
				references.add(new ActionReference(action.getIdInMatrix(), matrixName));
				if (references.size() >= maxActions)
				{
					int lastId = helper.insertBatch(insertAction);
					saveActionIds(lastId, references, context);
					references.clear();
				}
			}
			
			if (references.size() > 0)
			{
				int lastId = helper.insertBatch(insertAction);
				saveActionIds(lastId, references, context);
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw savingException("actions of matrix '"+matrixName+"'", e);
		}
	}
	
	private void saveAction(ActionState action, String matrixName, int matrixId, DbStateContext context, DateFormat timestampFormat,
			PreparedStatement query) throws ExecutorStateException
	{
		try
		{
			String stepName = action.getStepName();
			//If action is related to non-existing step, it will be saved with stepId = -1
			int stepId = stepName != null ? context.getStepId(stepName) : -1;
			
			QueryParameterSetter.newInstance(query)
					.setInt(matrixId)
					.setString(action.getActionClass().getCanonicalName())
					.setBytes(DbStateUtils.saveToXml(action.getMatrixInputParams(), allowedClasses))
					.setBytes(DbStateUtils.saveToXml(action.getInputParams(), allowedClasses))
					.setBytes(DbStateUtils.saveToXml(action.getSpecialParams(), allowedClasses))
					.setBytes(DbStateUtils.saveToXml(action.getSubActionsData(), allowedClasses))
					.setString(action.getIdInMatrix())
					.setString(action.getComment())
					.setString(action.getName())
					.setInt(stepId)
					.setString(action.getIdInTemplate())
					.setBoolean(action.isExecutable())
					.setBoolean(action.isInverted())
					.setBoolean(action.isDone())
					.setBoolean(action.isPassed())
					.setBoolean(action.isSuspendIfFailed())
					.setString(action.getFormulaExecutable())
					.setString(action.getFormulaInverted())
					.setString(action.getFormulaComment())
					.setString(action.getFormulaTimeout())
					.setString(action.getFormulaIdInTemplate())
					.setLong(action.getTimeout())
					.setBytes(DbStateUtils.saveToXml(action.getResult(), allowedClasses))
					.setString(DbStateUtils.formatTimestamp(action.getStarted(), timestampFormat))
					.setString(DbStateUtils.formatTimestamp(action.getFinished(), timestampFormat));
			query.addBatch();
		}
		catch (Exception e)
		{
			throw savingException("action '"+action.getIdInMatrix()+"' from matrix '"+matrixName+"'", e);
		}
	}
	
	private void saveActionIds(int lastId, List<ActionReference> references, DbStateContext context)
	{
		int id = lastId - references.size() + 1;
		for (ActionReference ref : references)
			context.setActionId(ref, id++);
	}
	
	private void saveStepContexts(List<StepState> steps, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Saving step contexts");
		
		for (StepState step : steps)
		{
			String stepName = step.getName();
			int stepId = context.getStepId(stepName);
			saveStepContexts(step.getStepContexts(), stepName, stepId, context);
		}
	}
	
	private void saveStepContexts(Map<String, StepContext> stepContexts, String stepName, int stepId, DbStateContext context) throws ExecutorStateException
	{
		if (stepContexts == null)
			return;
		
		try
		{
			for (Entry<String, StepContext> sc : stepContexts.entrySet())
			{
				int matrixId = context.getMatrixId(sc.getKey());
				saveStepContext(sc.getValue(), stepId, matrixId, context);
			}
		}
		catch (Exception e)
		{
			throw savingException("step context of step '"+stepName+"'", e);
		}
	}
	
	protected void saveStepContext(StepContext sc, int stepId, int matrixId, DbStateContext context) throws SQLException, IOException
	{
		QueryParameterSetter.newInstance(insertStepContext)
				.setInt(stepId)
				.setInt(matrixId)
				.setBytes(DbStateUtils.saveToXml(sc, allowedClasses));
		
		int id = helper.insert(insertStepContext);
		context.setStepContextId(stepId, matrixId, id);
	}
	
	
	private ExecutorStateException savingException(String entityName, Throwable cause)
	{
		return new ExecutorStateException("Could not save "+entityName, cause);
	}
}
