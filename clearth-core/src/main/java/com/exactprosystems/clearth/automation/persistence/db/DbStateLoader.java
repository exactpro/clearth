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

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.persistence.ExecutorStateException;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateInfo;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateObjects;
import com.exactprosystems.clearth.automation.persistence.MatrixState;
import com.exactprosystems.clearth.automation.persistence.StepState;
import com.exactprosystems.clearth.utils.Pair;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import com.exactprosystems.clearth.xmldata.XmlReportsConfig;
import com.exactprosystems.clearth.automation.ActionsExecutionProgress;
import com.exactprosystems.clearth.automation.MatrixData;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.automation.StartAtType;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.persistence.ActionState;

import static com.exactprosystems.clearth.automation.persistence.db.DbStateOperator.*;

public class DbStateLoader
{
	private static final Logger logger = LoggerFactory.getLogger(DbStateLoader.class);
	
	private final QueryHelper helper;
	private final Class[] allowedClasses;
	
	private volatile boolean queriesPrepared;
	private volatile PreparedStatement selectInfoId,
			selectInfo,
			selectReports,
			selectFixedIds,
			selectSteps,
			selectStepContexts,
			selectMatrixNames,
			selectMatrices,
			selectStepSuccess,
			selectStepStatusComments,
			selectActions;
	
	public DbStateLoader(QueryHelper helper, Class[] allowedClasses)
	{
		this.helper = helper;
		this.allowedClasses = allowedClasses;
		this.queriesPrepared = false;
	}
	
	
	public Pair<ExecutorStateInfo, DbStateContext> loadStateInfo() throws ExecutorStateException
	{
		prepareQueriesIfNeeded();
		
		int infoId = loadStateInfoId();
		DbStateContext context = new DbStateContext(infoId);
		DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
		
		ExecutorStateInfo stateInfo = loadStateInfoProperties(context, timestampFormat);
		List<XmlMatrixInfo> matricesReports = loadMatricesReportsInfo(context);
		stateInfo.getReportsInfo().setMatrices(matricesReports);
		
		List<String> matrixNames = loadMatrixNames(context);
		List<StepState> steps = loadSteps(context, timestampFormat);
		
		stateInfo.setMatrices(matrixNames);
		stateInfo.setSteps(steps);
		
		return new Pair<>(stateInfo, context);
	}
	
	public ExecutorStateObjects loadStateObjects(DbStateContext context) throws ExecutorStateException
	{
		prepareQueriesIfNeeded();
		
		DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
		
		Map<String, String> fixedIds = loadFixedIds(context);
		List<MatrixState> matrices = loadMatrices(context, timestampFormat);
		
		ExecutorStateObjects stateObjects = new ExecutorStateObjects();
		stateObjects.setFixedIDs(fixedIds);
		stateObjects.setMatrices(matrices);
		return stateObjects;
	}
	
	
	private void prepareQueriesIfNeeded() throws ExecutorStateException
	{
		if (!queriesPrepared)
		{
			prepareQueries();
			queriesPrepared = true;
		}
	}
	
	private void prepareQueries() throws ExecutorStateException
	{
		logger.debug("Preparing queries");
		
		try
		{
			selectInfoId = helper.prepare(String.format("select max(%s) from %s", COLUMN_INFO_ID, TABLE_INFOS));
			selectInfo = helper.prepareSelect(TABLE_INFOS, COLUMN_INFO_ID);
			selectReports = helper.prepareSelect(TABLE_REPORTS, COLUMN_INFO_ID);
			selectFixedIds = helper.prepareSelect(TABLE_FIXED_IDS, COLUMN_INFO_ID);
			
			selectSteps = helper.prepareSelect(TABLE_STEPS, COLUMN_INFO_ID);
			selectStepContexts = helper.prepareSelect(TABLE_STEP_CONTEXTS, COLUMN_STEP_ID);
			
			selectMatrixNames = helper.prepare(String.format("select %s, fileName from %s where %s = ?", COLUMN_MATRIX_ID, TABLE_MATRICES, COLUMN_INFO_ID));
			selectMatrices = helper.prepareSelect(TABLE_MATRICES, COLUMN_INFO_ID);
			selectStepSuccess = helper.prepareSelect(TABLE_STEP_SUCCESS, COLUMN_MATRIX_ID);
			selectStepStatusComments = helper.prepareSelect(TABLE_STEP_STATUS_COMMENTS, COLUMN_MATRIX_ID);
			selectActions = helper.prepareSelect(TABLE_ACTIONS, COLUMN_MATRIX_ID);
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Error while preparing queries", e);
		}
	}
	
	private int loadStateInfoId() throws ExecutorStateException
	{
		String entity = "state info ID";
		logger.debug("Loading {}", entity);
		
		try (ResultSet rs = helper.selectNonEmpty(selectInfoId, entity))
		{
			return rs.getInt(1);
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException(entity, e);
		}
	}
	
	private ExecutorStateInfo loadStateInfoProperties(DbStateContext context, DateFormat timestampFormat) throws ExecutorStateException
	{
		String entity = "state info properties";
		logger.debug("Loading {}", entity);
		
		try
		{
			selectInfo.setInt(1, context.getInfoId());
			try (ResultSet rs = helper.selectNonEmpty(selectInfo, entity))
			{
				ExecutorStateInfo result = new ExecutorStateInfo();
				result.setWeekendHoliday(rs.getBoolean("weekendHoliday"));
				result.setHolidays(DbStateUtils.loadFromXml(rs.getBytes("holidays"), allowedClasses));
				result.setBusinessDay(DbStateUtils.parseTimestamp(rs.getString("businessDay"), DbStateUtils.createDateFormat()));
				result.setStartedByUser(rs.getString("startedByUser"));
				result.setStarted(DbStateUtils.parseTimestamp(rs.getString("started"), timestampFormat));
				result.setEnded(DbStateUtils.parseTimestamp(rs.getString("ended"), timestampFormat));
				
				XmlReportsConfig reportsConfig = new XmlReportsConfig();
				reportsConfig.setCompleteHtmlReport(rs.getBoolean("completeHtmlReport"));
				reportsConfig.setFailedHtmlReport(rs.getBoolean("failedHtmlReport"));
				reportsConfig.setCompleteJsonReport(rs.getBoolean("completeJsonReport"));
				
				ReportsInfo reportsInfo = new ReportsInfo();
				reportsInfo.setPath(rs.getString("reportsPath"));
				reportsInfo.setActionReportsPath(rs.getString("actionReportsPath"));
				reportsInfo.setXmlReportsConfig(reportsConfig);
				
				result.setReportsInfo(reportsInfo);
				return result;
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException(entity, e);
		}
	}
	
	private List<XmlMatrixInfo> loadMatricesReportsInfo(DbStateContext context) throws ExecutorStateException
	{
		String entity = "matrices reports info";
		logger.debug("Loading {}", entity);
		
		try
		{
			selectReports.setInt(1, context.getInfoId());
			try (ResultSet rs = helper.select(selectReports))
			{
				if (!rs.next())
					return new ArrayList<>();
				
				List<XmlMatrixInfo> result = new ArrayList<>();
				do
				{
					XmlMatrixInfo mi = new XmlMatrixInfo();
					mi.setFileName(rs.getString("fileName"));
					mi.setName(rs.getString("name"));
					mi.setActionsDone(rs.getInt("actionsDone"));
					mi.setSuccessful(rs.getBoolean("successful"));
					
					result.add(mi);
				}
				while (rs.next());
				
				return result;
			}
		}
		catch (Exception e)
		{
			throw loadingException(entity, e);
		}
	}
	
	private List<StepState> loadSteps(DbStateContext context, DateFormat timestampFormat) throws ExecutorStateException
	{
		String entity = "steps";
		logger.debug("Loading {}", entity);
		
		try
		{
			selectSteps.setInt(1, context.getInfoId());
			try (ResultSet rs = helper.selectNonEmpty(selectSteps, entity))
			{
				List<StepState> result = new ArrayList<>();
				do
				{
					int stepId = rs.getInt(COLUMN_STEP_ID);
					StepState step = loadStep(rs, timestampFormat);
					context.setStepId(step.getName(), stepId);
					
					Map<String, StepContext> contexts = loadStepContexts(step, context);
					step.setStepContexts(contexts);
					
					result.add(step);
				}
				while (rs.next());
				
				return result;
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException(entity, e);
		}
	}
	
	private StepState loadStep(ResultSet rs, DateFormat timestampFormat) throws SQLException, ParseException, IOException
	{
		StepState step = new StepState();
		step.setName(rs.getString("name"));
		step.setKind(rs.getString("kind"));
		step.setStartAt(rs.getString("startAt"));
		step.setParameter(rs.getString("parameter"));
		step.setAskForContinue(rs.getBoolean("askForContinue"));
		step.setAskIfFailed(rs.getBoolean("askIfFailed"));
		step.setExecute(rs.getBoolean("execute"));
		step.setStartAtType(StartAtType.getValue(rs.getString("startAtType")));
		step.setWaitNextDay(rs.getBoolean("waitNextDay"));
		step.setComment(rs.getString("comment"));
		step.setStarted(DbStateUtils.parseTimestamp(rs.getString("started"), timestampFormat));
		step.setFinished(DbStateUtils.parseTimestamp(rs.getString("finished"), timestampFormat));
		step.setExecutionProgress(new ActionsExecutionProgress(rs.getInt("actionsSuccessful"), rs.getInt("actionsDone")));
		step.setAnyActionFailed(rs.getBoolean("anyActionFailed"));
		step.setFailedDueToError(rs.getBoolean("failedDueToError"));
		step.setStatusComment(rs.getString("statusComment"));
		step.setError(DbStateUtils.loadFromXml(rs.getBytes("error"), allowedClasses));
		return step;
	}
	
	private Map<String, String> loadFixedIds(DbStateContext context) throws ExecutorStateException
	{
		String entity = "fixed IDs";
		logger.debug("Loading {}", entity);
		
		try
		{
			selectFixedIds.setInt(1, context.getInfoId());
			try (ResultSet rs = helper.select(selectFixedIds))
			{
				if (!rs.next())
					return null;
				
				return DbStateUtils.loadFromXml(rs.getBytes("fixedIds"), allowedClasses);
			}
		}
		catch (Exception e)
		{
			throw loadingException(entity, e);
		}
	}
	
	private List<String> loadMatrixNames(DbStateContext context) throws ExecutorStateException
	{
		String entity = "matrix names";
		logger.debug("Loading {}", entity);
		
		try
		{
			selectMatrixNames.setInt(1, context.getInfoId());
			try (ResultSet rs = helper.selectNonEmpty(selectMatrixNames, entity))
			{
				List<String> result = new ArrayList<>();
				do
				{
					int matrixId = rs.getInt(COLUMN_MATRIX_ID);
					String fileName = rs.getString("fileName"),
							name = new File(fileName).getName();
					
					result.add(name);
					context.setMatrixId(name, matrixId);
				}
				while (rs.next());
				
				return result;
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException(entity, e);
		}
	}
	
	private List<MatrixState> loadMatrices(DbStateContext context, DateFormat timestampFormat) throws ExecutorStateException
	{
		String entity = "matrices";
		logger.debug("Loading {}", entity);
		
		try
		{
			selectMatrices.setInt(1, context.getInfoId());
			try (ResultSet rs = helper.selectNonEmpty(selectMatrices, entity))
			{
				List<MatrixState> result = new ArrayList<>();
				do
				{
					int matrixId = rs.getInt(COLUMN_MATRIX_ID);
					MatrixState matrix = loadMatrix(rs, timestampFormat);
					result.add(matrix);
					context.setMatrixId(matrix.getName(), matrixId);
					
					Map<String, Boolean> success = loadStepSuccess(matrixId, context);
					matrix.setStepSuccess(success);
					
					Map<String, List<String>> comments = loadStepStatusComments(matrixId, context);
					matrix.setStepStatusComments(comments);
					
					List<ActionState> actions = loadActions(matrixId, matrix.getName(), context, timestampFormat);
					matrix.setActions(actions);
				}
				while (rs.next());
				
				return result;
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException(entity, e);
		}
	}
	
	private MatrixState loadMatrix(ResultSet rs, DateFormat timestampFormat) throws SQLException, IOException, ParseException
	{
		MatrixState matrix = new MatrixState();
		matrix.setFileName(rs.getString("fileName"));
		matrix.setName(rs.getString("name"));
		matrix.setDescription(rs.getString("description"));
		matrix.setConstants(DbStateUtils.loadFromXml(rs.getBytes("constants"), allowedClasses));
		matrix.setMvelVars(DbStateUtils.loadFromXml(rs.getBytes("mvelVars"), allowedClasses));
		matrix.setStarted(DbStateUtils.parseTimestamp(rs.getString("started"), timestampFormat));
		matrix.setActionsDone(rs.getInt("actionsDone"));
		matrix.setSuccessful(rs.getBoolean("successfulMatrix"));
		matrix.setContext(DbStateUtils.loadFromXml(rs.getBytes("matrixContext"), allowedClasses));
		
		MatrixData md = new MatrixData();
		md.setName(matrix.getName());
		md.setFile(new File(matrix.getFileName()));
		md.setUploadDate(DbStateUtils.parseTimestamp(rs.getString("uploadDate"), timestampFormat));
		md.setExecute(rs.getBoolean("execute"));
		md.setTrim(rs.getBoolean("trimSpaces"));
		md.setLink(rs.getString("link"));
		md.setType(rs.getString("linkType"));
		md.setAutoReload(rs.getBoolean("autoReload"));
		matrix.setMatrixData(md);
		return matrix;
	}
	
	private Map<String, Boolean> loadStepSuccess(int matrixId, DbStateContext context) throws ExecutorStateException
	{
		try
		{
			selectStepSuccess.setInt(1, matrixId);
			try (ResultSet rs = helper.select(selectStepSuccess))
			{
				if (!rs.next())
					return new HashMap<>();
				
				Map<String, Boolean> result = new HashMap<>();
				do
				{
					int stepId = rs.getInt(COLUMN_STEP_ID),
							recordId = rs.getInt(COLUMN_RECORD_ID);
					String stepName = context.getStepName(stepId);
					boolean success = rs.getBoolean("success");
					
					result.put(stepName, success);
					context.setStepSuccessId(matrixId, stepId, recordId);
				}
				while (rs.next());
				
				return result;
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException("step success", e);
		}
	}
	
	private Map<String, List<String>> loadStepStatusComments(int matrixId, DbStateContext context) throws ExecutorStateException
	{
		try
		{
			selectStepStatusComments.setInt(1, matrixId);
			try (ResultSet rs = helper.select(selectStepStatusComments))
			{
				if (!rs.next())
					return new HashMap<>();
				
				Map<String, List<String>> result = new HashMap<>();
				do
				{
					int stepId = rs.getInt(COLUMN_STEP_ID),
							recordId = rs.getInt(COLUMN_RECORD_ID);
					String stepName = context.getStepName(stepId);
					List<String> comments = DbStateUtils.loadFromXml(rs.getBytes("comments"), allowedClasses);
					
					result.put(stepName, comments);
					context.setStepStatusCommentsId(matrixId, stepId, recordId);
				}
				while (rs.next());
				
				return result;
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException("step status comments", e);
		}
	}
	
	private List<ActionState> loadActions(int matrixId, String matrixName, DbStateContext context, DateFormat timestampFormat) throws ExecutorStateException
	{
		try
		{
			selectActions.setInt(1, matrixId);
			try (ResultSet rs = helper.select(selectActions))
			{
				if (!rs.next())
					return new ArrayList<>();
				
				List<ActionState> result = new ArrayList<>();
				do
				{
					int actionId = rs.getInt(COLUMN_ACTION_ID);
					ActionState action = loadAction(rs, context, timestampFormat);
					
					result.add(action);
					context.setActionId(new ActionReference(action.getIdInMatrix(), matrixName), actionId);
				}
				while (rs.next());
				
				return result;
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException("actions", e);
		}
	}
	
	private ActionState loadAction(ResultSet rs, DbStateContext context, DateFormat timestampFormat)
			throws SQLException, IOException, ParseException, ClassNotFoundException, ExecutorStateException
	{
		int stepId = rs.getInt(COLUMN_STEP_ID);
		//If action is related to non-existing step, it will have stepId < 0
		String stepName = stepId >= 0 ? context.getStepName(stepId) : null;
		
		ActionState action = new ActionState();
		action.setActionClass(Class.forName(rs.getString("actionClass")));
		action.setMatrixInputParams(DbStateUtils.loadFromXml(rs.getBytes("matrixInputParams"), allowedClasses));
		action.setInputParams(DbStateUtils.loadFromXml(rs.getBytes("inputParams"), allowedClasses));
		action.setSpecialParams(DbStateUtils.loadFromXml(rs.getBytes("specialParams"), allowedClasses));
		action.setSubActionsData(DbStateUtils.loadFromXml(rs.getBytes("subActionsData"), allowedClasses));
		action.setIdInMatrix(rs.getString("idInMatrix"));
		action.setComment(rs.getString("comment"));
		action.setName(rs.getString("name"));
		action.setStepName(stepName);
		action.setIdInTemplate(rs.getString("idInTemplate"));
		action.setExecutable(rs.getBoolean("executable"));
		action.setInverted(rs.getBoolean("inverted"));
		action.setDone(rs.getBoolean("done"));
		action.setPassed(rs.getBoolean("passed"));
		action.setSuspendIfFailed(rs.getBoolean("suspendIfFailed"));
		action.setFormulaExecutable(rs.getString("formulaExecutable"));
		action.setFormulaInverted(rs.getString("formulaInverted"));
		action.setFormulaComment(rs.getString("formulaComment"));
		action.setFormulaTimeout(rs.getString("formulaTimeout"));
		action.setFormulaIdInTemplate(rs.getString("formulaIdInTemplate"));
		action.setTimeout(rs.getLong("timeout"));
		action.setResult(DbStateUtils.loadFromXml(rs.getBytes("result"), allowedClasses));
		action.setStarted(DbStateUtils.parseTimestamp(rs.getString("started"), timestampFormat));
		action.setFinished(DbStateUtils.parseTimestamp(rs.getString("finished"), timestampFormat));
		return action;
	}
	
	private Map<String, StepContext> loadStepContexts(StepState step, DbStateContext context) throws ExecutorStateException
	{
		try
		{
			int stepId = context.getStepId(step.getName());
			selectStepContexts.setInt(1, stepId);
			try (ResultSet rs = helper.select(selectStepContexts))
			{
				if (!rs.next())
					return null;
				
				Map<String, StepContext> result = new HashMap<>();
				do
				{
					int matrixId = rs.getInt(COLUMN_MATRIX_ID);
					String matrixName = context.getMatrixName(matrixId);
					StepContext sc = DbStateUtils.loadFromXml(rs.getBytes("context"), allowedClasses);
					result.put(matrixName, sc);
					
					context.setStepContextId(stepId, matrixId, rs.getInt(COLUMN_RECORD_ID));
				}
				while (rs.next());
				
				return result;
			}
		}
		catch (ExecutorStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw loadingException("step context of step '"+step.getName()+"'", e);
		}
	}
	
	
	private ExecutorStateException loadingException(String entityName, Throwable cause)
	{
		return new ExecutorStateException("Could not load "+entityName, cause);
	}
}
