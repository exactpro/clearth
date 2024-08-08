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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.persistence.ActionState;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateException;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateInfo;
import com.exactprosystems.clearth.automation.persistence.MatrixState;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.persistence.StepState;

import static com.exactprosystems.clearth.automation.persistence.db.DbStateOperator.*;

public class DbStateUpdater
{
	private static final Logger logger = LoggerFactory.getLogger(DbStateUpdater.class);
	
	private final QueryHelper helper;
	private final Class[] allowedClasses;
	private final DbStateSaver saver;
	
	private volatile boolean queriesPrepared;
	private volatile PreparedStatement updateStateInfo,
			updateStep,
			updateStepContext,
			updateMatrix,
			updateStepSuccess,
			updateStepStatusComments,
			updateAction,
			updateStepProperties,
			updateMatrixInState,
			removeActions;
	
	public DbStateUpdater(QueryHelper helper, Class[] allowedClasses, DbStateSaver saver)
	{
		this.helper = helper;
		this.allowedClasses = allowedClasses;
		this.saver = saver;
		this.queriesPrepared = false;
	}
	
	
	public void update(DbStateContext context, Action lastExecutedAction, ActionState actionState) throws ExecutorStateException
	{
		prepareQueriesIfNeeded();
		
		Step step = lastExecutedAction.getStep();
		Matrix matrix = lastExecutedAction.getMatrix();
		
		try
		{
			helper.startTransaction();
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Could not start transaction", e);
		}
		
		try
		{
			updateStep(step, context);
			updateStepContext(step, matrix, context);
			updateMatrix(matrix, context);
			updateStepSuccess(matrix, step, context);
			updateStepStatusComments(matrix, step, context);
			
			updateAction(actionState, matrix.getName(), context);
			
			helper.commitTransaction();
		}
		catch (Exception e)
		{
			try
			{
				helper.rollbackTransaction();
			}
			catch (SQLException e1)
			{
				e.addSuppressed(e1);
			}
			
			if (e instanceof ExecutorStateException)
				throw (ExecutorStateException)e;
			throw new ExecutorStateException("Error while updating state after action", e);
		}
	}
	
	public void update(DbStateContext context, Step lastFinishedStep) throws ExecutorStateException
	{
		prepareQueriesIfNeeded();
		
		try
		{
			helper.startTransaction();
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Could not start transaction", e);
		}
		
		try
		{
			updateStep(lastFinishedStep, context);
			updateStepMatrices(lastFinishedStep, context);
			
			helper.commitTransaction();
		}
		catch (Exception e)
		{
			try
			{
				helper.rollbackTransaction();
			}
			catch (SQLException e1)
			{
				e.addSuppressed(e1);
			}
			
			if (e instanceof ExecutorStateException)
				throw (ExecutorStateException)e;
			throw new ExecutorStateException("Error while updating state after global step", e);
		}
	}
	
	public void updateSteps(List<StepState> steps, DbStateContext context) throws ExecutorStateException
	{
		prepareQueriesIfNeeded();
		
		try
		{
			helper.startTransaction();
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Could not start transaction", e);
		}
		
		try
		{
			for (StepState step : steps)
			{
				int stepId = context.getStepId(step.getName());
				updateStepProperties(step, stepId);
			}
			
			helper.commitTransaction();
		}
		catch (Exception e)
		{
			try
			{
				helper.rollbackTransaction();
			}
			catch (SQLException e1)
			{
				e.addSuppressed(e1);
			}
			
			if (e instanceof ExecutorStateException)
				throw (ExecutorStateException)e;
			throw new ExecutorStateException("Error while updating steps", e);
		}
	}
	
	public void updateStateInfo(ExecutorStateInfo stateInfo, DbStateContext context) throws ExecutorStateException
	{
		prepareQueries();
		
		DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
		try
		{
			QueryParameterSetter.newInstance(updateStateInfo)
					.setString(stateInfo.getStartedByUser())
					.setString(DbStateUtils.formatTimestamp(stateInfo.getStarted(), timestampFormat))
					.setString(DbStateUtils.formatTimestamp(stateInfo.getEnded(), timestampFormat))
					.setString(stateInfo.getReportsInfo().getActionReportsPath())
					.setInt(context.getInfoId());
			helper.update(updateStateInfo);
		}
		catch (Exception e)
		{
			throw updatingException("state info", e);
		}
	}
	
	public void updateMatrices(DbStateContext context, Collection<MatrixState> matrixStates) throws ExecutorStateException
	{
		prepareQueriesIfNeeded();
		
		try
		{
			helper.startTransaction();
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Could not start transaction", e);
		}
		
		DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
		try
		{
			for (MatrixState m : matrixStates)
			{
				updateMatrixInState(m, timestampFormat, context);
				updateActionsInState(m, context);
			}
			
			helper.commitTransaction();
		}
		catch (Exception e)
		{
			try
			{
				helper.rollbackTransaction();
			}
			catch (SQLException e1)
			{
				e.addSuppressed(e1);
			}
			
			if (e instanceof ExecutorStateException)
				throw (ExecutorStateException)e;
			throw new ExecutorStateException("Error while updating matrix states", e);
		}
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
			updateStateInfo = helper.prepareUpdate(TABLE_INFOS, COLUMN_INFO_ID, Arrays.asList("startedByUser", "started", "ended", "actionReportsPath"));
			
			updateStep = helper.prepareUpdate(TABLE_STEPS, COLUMN_STEP_ID, Arrays.asList("started", "finished", "actionsSuccessful", "actionsDone",
					"anyActionFailed", "statusComment", "error", "failedDueToError"));
			
			updateStepContext = helper.prepareUpdate(TABLE_STEP_CONTEXTS, COLUMN_RECORD_ID, Arrays.asList("context"));
			
			updateMatrix = helper.prepareUpdate(TABLE_MATRICES, COLUMN_MATRIX_ID, Arrays.asList("mvelVars", "actionsDone", "successfulMatrix", "matrixContext"));
			updateStepSuccess = helper.prepareUpdate(TABLE_STEP_SUCCESS, COLUMN_RECORD_ID, Arrays.asList("success"));
			updateStepStatusComments = helper.prepareUpdate(TABLE_STEP_STATUS_COMMENTS, COLUMN_RECORD_ID, Arrays.asList("comments"));
			
			updateAction = helper.prepareUpdate(TABLE_ACTIONS, COLUMN_ACTION_ID, Arrays.asList("matrixInputParams", "inputParams", "specialParams", "subActionsData",
					"comment", "executable", "inverted", "done", "passed",
					"result", "started", "finished"));
			
			updateStepProperties = helper.prepareUpdate(TABLE_STEPS, COLUMN_STEP_ID, Arrays.asList("askForContinue", "askIfFailed", "execute", "startAt"));
			updateMatrixInState = helper.prepareUpdate(TABLE_MATRICES, COLUMN_MATRIX_ID, Arrays.asList("description", "constants", "mvelVars", "uploadDate"));
			removeActions = helper.prepare("delete from "+TABLE_ACTIONS+" where "+COLUMN_MATRIX_ID+" = ?");
		}
		catch (Exception e)
		{
			throw new ExecutorStateException("Error while preparing queries", e);
		}
		
		logger.debug("Preparing saver's queries");
		saver.prepareQueries();
	}
	
	private void updateStep(Step step, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Updating step");
		
		try
		{
			DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
			QueryParameterSetter.newInstance(updateStep)
					.setString(DbStateUtils.formatTimestamp(step.getStarted(), timestampFormat))
					.setString(DbStateUtils.formatTimestamp(step.getFinished(), timestampFormat))
					.setInt(step.getExecutionProgress().getSuccessful())
					.setInt(step.getExecutionProgress().getDone())
					.setBoolean(step.isAnyActionFailed())
					.setString(step.getStatusComment())
					.setBytes(DbStateUtils.saveToXml(step.getError(), allowedClasses))
					.setBoolean(step.isFailedDueToError())
					.setInt(context.getStepId(step.getName()));
			helper.update(updateStep);
		}
		catch (Exception e)
		{
			throw updatingException("step '"+step.getName()+"'", e);
		}
	}
	
	private void updateStepContext(Step step, Matrix matrix, DbStateContext context) throws ExecutorStateException
	{
		Map<Matrix, StepContext> stepContexts = step.getStepContexts();
		if (stepContexts == null)
			return;
		
		logger.debug("Updating step context");
		
		String stepName = step.getName(),
				matrixName = matrix.getName();
		try
		{
			int stepId = context.getStepId(stepName),
					matrixId = context.getMatrixId(matrixName);
			
			Integer id = context.getStepContextIdOrNull(stepId, matrixId);
			if (id == null)  //Context might not exist initially, but added during execution
			{
				saver.saveStepContext(stepContexts.get(matrix), stepId, matrixId, context);
				return;
			}
			
			QueryParameterSetter.newInstance(updateStepContext)
					.setBytes(DbStateUtils.saveToXml(stepContexts.get(matrix), allowedClasses))
					.setInt(id);
			helper.update(updateStepContext);
		}
		catch (Exception e)
		{
			throw updatingException("step context of step '"+stepName+"' and matrix '"+matrixName+"'", e);
		}
	}
	
	private void updateMatrix(Matrix matrix, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Updating matrix");
		
		try
		{
			QueryParameterSetter.newInstance(updateMatrix)
					.setBytes(DbStateUtils.saveToXml(matrix.getMvelVars(), allowedClasses))
					.setInt(matrix.getActionsDone())
					.setBoolean(matrix.isSuccessful())
					.setBytes(DbStateUtils.saveToXml(matrix.getContext(), allowedClasses))
					.setInt(context.getMatrixId(matrix.getName()));
			helper.update(updateMatrix);
		}
		catch (Exception e)
		{
			throw updatingException("matrix '"+matrix.getName()+"'", e);
		}
	}
	
	private void updateStepSuccess(Matrix matrix, Step step, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Updating step success flag");
		
		String matrixName = matrix.getName(),
				stepName = step.getName();
		try
		{
			int matrixId = context.getMatrixId(matrixName),
					stepId = context.getStepId(stepName);
			
			Integer id = context.getStepSuccessIdOrNull(matrixId, stepId);
			if (id == null)  //Success might not be initialized, but added during execution
			{
				saver.saveStepSuccess(matrix.isStepSuccessful(stepName), matrixId, stepId, context);
				return;
			}
			
			QueryParameterSetter.newInstance(updateStepSuccess)
					.setBoolean(matrix.isStepSuccessful(stepName))
					.setInt(id);
			helper.update(updateStepSuccess);
		}
		catch (Exception e)
		{
			throw updatingException("success of step '"+stepName+"' in matrix '"+matrixName+"'", e);
		}
	}
	
	private void updateStepStatusComments(Matrix matrix, Step step, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Updating step status comments");
		
		String matrixName = matrix.getName(),
				stepName = step.getName();
		try
		{
			int matrixId = context.getMatrixId(matrixName),
					stepId = context.getStepId(stepName);
			
			Integer id = context.getStepStatusCommentsIdOrNull(matrixId, stepId);
			if (id == null)  //Status comments might not be initialized, but added during execution
			{
				saver.saveStepStatusComments(matrix.getStepStatusComments(stepName), matrixId, stepId, context);
				return;
			}
			
			QueryParameterSetter.newInstance(updateStepStatusComments)
					.setBytes(DbStateUtils.saveToXml(matrix.getStepStatusComments(stepName), allowedClasses))
					.setInt(id);
			helper.update(updateStepStatusComments);
		}
		catch (Exception e)
		{
			throw updatingException("status comments of step '"+stepName+"' in matrix '"+matrixName+"'", e);
		}
	}
	
	private void updateAction(ActionState action, String matrixName, DbStateContext context) throws ExecutorStateException
	{
		logger.debug("Updating action");
		
		try
		{
			DateFormat timestampFormat = DbStateUtils.createTimestampFormat();
			QueryParameterSetter.newInstance(updateAction)
					.setBytes(DbStateUtils.saveToXml(action.getMatrixInputParams(), allowedClasses))
					.setBytes(DbStateUtils.saveToXml(action.getInputParams(), allowedClasses))
					.setBytes(DbStateUtils.saveToXml(action.getSpecialParams(), allowedClasses))
					.setBytes(DbStateUtils.saveToXml(action.getSubActionsData(), allowedClasses))
					.setString(action.getComment())
					.setBoolean(action.isExecutable())
					.setBoolean(action.isInverted())
					.setBoolean(action.isDone())
					.setBoolean(action.isPassed())
					.setBytes(DbStateUtils.saveToXml(action.getResult(), allowedClasses))
					.setString(DbStateUtils.formatTimestamp(action.getStarted(), timestampFormat))
					.setString(DbStateUtils.formatTimestamp(action.getFinished(), timestampFormat))
					.setInt(context.getActionId(new ActionReference(action.getIdInMatrix(), matrixName)));
			helper.update(updateAction);
		}
		catch (Exception e)
		{
			throw updatingException("action '"+action.getIdInMatrix()+"' from matrix '"+matrixName+"'", e);
		}
	}
	
	private void updateStepMatrices(Step step, DbStateContext context) throws ExecutorStateException
	{
		Set<Matrix> matrices = new HashSet<>();
		for (Action action : step.getActions())
			matrices.add(action.getMatrix());
		
		for (Matrix m : matrices)
		{
			updateStepContext(step, m, context);
			updateStepSuccess(m, step, context);
			updateStepStatusComments(m, step, context);
		}
	}
	
	
	private void updateStepProperties(StepState step, int stepId) throws ExecutorStateException
	{
		try
		{
			QueryParameterSetter.newInstance(updateStepProperties)
					.setBoolean(step.isAskForContinue())
					.setBoolean(step.isAskIfFailed())
					.setBoolean(step.isExecute())
					.setString(step.getStartAt())
					.setInt(stepId);
			helper.update(updateStepProperties);
		}
		catch (Exception e)
		{
			throw updatingException("properties of step '"+step.getName()+"'", e);
		}
	}
	
	
	private void updateMatrixInState(MatrixState matrixState, DateFormat timestampFormat, DbStateContext context) throws ExecutorStateException
	{
		String matrixName = matrixState.getName();
		
		logger.debug("Updating state of matrix '"+matrixName+"'");
		
		try
		{
			QueryParameterSetter.newInstance(updateMatrixInState)
					.setString(matrixState.getDescription())
					.setBytes(DbStateUtils.saveToXml(matrixState.getConstants(), allowedClasses))
					.setBytes(DbStateUtils.saveToXml(matrixState.getMvelVars(), allowedClasses))
					.setString(DbStateUtils.formatTimestamp(matrixState.getMatrixData().getUploadDate(), timestampFormat))
					.setInt(context.getMatrixId(matrixName));
			helper.update(updateMatrixInState);
		}
		catch (Exception e)
		{
			throw updatingException("state of matrix '"+matrixName+"'", e);
		}
	}
	
	private void updateActionsInState(MatrixState matrixState, DbStateContext context) throws ExecutorStateException
	{
		String matrixName = matrixState.getName();
		
		logger.debug("Updating state of actions from matrix '"+matrixName+"'");
		
		int matrixId = context.getMatrixId(matrixName);
		try
		{
			QueryParameterSetter.newInstance(removeActions)
					.setInt(matrixId);
			helper.update(removeActions);
			context.removeActionIds(matrixName);
			
			saver.saveActions(matrixState.getActions(), matrixName, matrixId, context);
		}
		catch (Exception e)
		{
			throw updatingException("state of actions from matrix '"+matrixName+"' after matrix update", e);
		}
	}
	
	
	private ExecutorStateException updatingException(String entityName, Throwable cause)
	{
		return new ExecutorStateException("Could not update "+entityName, cause);
	}
}
