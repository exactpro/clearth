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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.automation.actions.SchedulerPause;
import com.exactprosystems.clearth.automation.async.AsyncActionData;
import com.exactprosystems.clearth.automation.async.AsyncActionsManager;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.report.ActionReportWriter;
import com.exactprosystems.clearth.automation.report.FailReason;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.data.TestExecutionHandler;
import com.exactprosystems.clearth.utils.Utils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.map.UnmodifiableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class ActionExecutor implements Closeable
{
	private static final Logger logger = LoggerFactory.getLogger(ActionExecutor.class);
	
	protected static final String ACTION_SKIP_MSG_PREFIX = "Action has been skipped as user decided to ignore ";
	
	public static final String PARAMS_IN = "in",
			PARAMS_OUT = "out",
			PARAMS_PREV_ACTION = "prevAction",
			PARAMS_THIS_ACTION = "thisAction",
			VARKEY_ACTION = "action";
	
	private final GlobalContext globalContext;
	private final ActionParamsCalculator calculator;
	private final ActionReportWriter reportWriter;
	private final TestExecutionHandler executionHandler;
	private final FailoverStatus failoverStatus;
	private final boolean ignoreAllConnectionsFailures;
	private final Set<String> connectionsToIgnoreFailures;
	
	private AsyncActionsManager asyncManager;
	private ActionsExecutionProgress executionProgress;
	private String actionsReportsDir;
	private boolean interrupted = false;
	
	protected boolean saveDetailedResult = false;
	
	public ActionExecutor(GlobalContext globalContext, ActionParamsCalculator calculator, ActionReportWriter reportWriter,
			FailoverStatus failoverStatus, boolean ignoreAllConnectionsFailures, Set<String> connectionsToIgnoreFailures)
	{
		this.globalContext = globalContext;
		this.calculator = calculator;
		this.reportWriter = reportWriter;
		this.executionHandler = globalContext.getExecutionHandler();
		this.failoverStatus = failoverStatus;
		this.ignoreAllConnectionsFailures = ignoreAllConnectionsFailures;
		this.connectionsToIgnoreFailures = connectionsToIgnoreFailures;
		this.asyncManager = null;
	}
	
	
	@Override
	public void close() throws IOException
	{
		Utils.closeResource(asyncManager);
	}
	
	
	public ActionParamsCalculator getCalculator()
	{
		return calculator;
	}

	public ActionReportWriter getReportWriter()
	{
		return reportWriter;
	}
	
	
	/**
	 * Use this method to prepare action executor for actions from new step
	 */
	public void reset(String actionsReportsDir, ActionsExecutionProgress executionProgress)
	{
		this.executionProgress = executionProgress;
		reportWriter.reset();
		this.actionsReportsDir = actionsReportsDir;
	}


	public void prepareToAction(Action action)
	{
		Matrix matrix = action.getMatrix();
		String stepName = action.getStepName();
		if (!matrix.getStepSuccess().containsKey(stepName))  //If this is first executed action of this step in matrix
			matrix.setStepSuccessful(stepName, true);
	}
	
	public boolean prepareActionReplay(Action action)
	{
		//If we are replaying the step we need to replay only failed ReceiveXXX actions
		Matrix matrix = action.getMatrix();
		String stepName = action.getStepName();
		
		if (!isFailedReplayableAction(action))
		{
			if ((action.getResult() != null) && (!action.getResult().isSuccess()))
			{
				matrix.setStepSuccessful(stepName, false);
				matrix.addStepStatusComment(stepName, "One or more actions failed");
			}
			else if (!action.isSubaction())
			{
				executionProgress.incrementSuccessful();
				matrix.incActionsSuccess();
			}
			
			if (!action.isSubaction())
				executionProgress.incrementDone();
			return false;
		}
		
		action.setDone(false);
		matrix.setActionsDone(matrix.getActionsDone() - 1);
		return true;
	}
	
	public void executeAction(Action action, StepContext stepContext, AtomicBoolean canReplay)
	{
		String actionDesc = null;
		
		boolean stepExecutable = action.getStep().isExecute();
		try
		{
			if (getLogger().isDebugEnabled())
			{
				actionDesc = action.getDescForLog("");
				getLogger().debug("Calculating parameters of{}", actionDesc);
			}

			List<String> errorsInParams = calculator.calculateParameters(action, stepExecutable);
			
			if (getLogger().isTraceEnabled())
				getLogger().trace("Finished calculation for{}", actionDesc != null ? actionDesc : action.getDescForLog(""));
			
			if (action.isExecutable() && stepExecutable)
			{
				if (!doExecuteAction(action, errorsInParams, stepContext, canReplay))  //Action may trigger step end due to aborted failover
					return;
			}
			else
				handleNonExecutableAction(action, stepExecutable);
		}
		catch (Exception e)
		{
			handleActionCrash(action, e);
		}
		
		if (!isAsyncAction(action))
			cleanContexts(action);
	}
	
	public void callActionAsync(Action action, StepContext stepContext, MatrixContext matrixContext) throws InterruptedException
	{
		if (!isAsyncEnabled())
			asyncManager = createAsyncManager();
		
		String waitMsg;
		switch (action.getWaitAsyncEnd())
		{
		case STEP :
			String beforeStep = action.getWaitAsyncEndStep();
			waitMsg = (isNotBlank(beforeStep))
				? "Will wait for this action to finish before starting step '" + beforeStep + "'"
				: "Will wait in end of step for this action to finish";
			break;
		case SCHEDULER : waitMsg = "Will wait in end of scheduler for this action to finish"; break;
		default : waitMsg = "Won't wait for this action to finish"; break;
		}
		
		action.setResult(DefaultResult.passed("Action is executing asynchronously. " + waitMsg));
		action.setFinished(new Date());
		
		asyncManager.addAsyncAction(createAsyncActionData(action, stepContext, matrixContext));
	}
	
	public void callAction(Action action, StepContext stepContext, MatrixContext matrixContext) throws FailoverException
	{
		//Execution may fail with FailoverException indicating a need to establish new connection
		action.execute(stepContext, matrixContext, globalContext);
		action.setFinished(new Date());
	}
	
	public void checkAsyncActions(long minimumFinishAge)
	{
		if (!isAsyncEnabled())
			return;
		
		Date oldestFinish = asyncManager.getTimestampOfNextFinishedAction();
		if (oldestFinish == null || new Date().getTime() - oldestFinish.getTime() < minimumFinishAge)
			return;
		
		List<Action> finishedActions = null;
		AsyncActionData a;
		while ((a = asyncManager.getNextFinishedAction()) != null)
		{
			updateActionAfterAsyncFinish(a);
			
			if (finishedActions == null)
				finishedActions = new ArrayList<>();
			finishedActions.add(a.getAction());
		}
		
		if (!CollectionUtils.isEmpty(finishedActions))
			updateAsyncActions(finishedActions);
	}

	public void waitForBeforeStepAsyncActions(String stepName) throws InterruptedException
	{
		callWaitForAsyncActions(a -> a.getBeforeStepActions(stepName),
				"Waiting for async actions to finish before step '" + stepName + "'");
	}

	public void waitForStepAsyncActions(String stepName) throws InterruptedException
	{
		callWaitForAsyncActions(a -> a.getStepActions(stepName),
				"Waiting for step '"+stepName+"' async actions to finish");
	}
	
	public void waitForSchedulerAsyncActions() throws InterruptedException
	{
		callWaitForAsyncActions(AsyncActionsManager::getSchedulerActions,
				"Waiting for scheduler async actions to finish");
	}

	protected void callWaitForAsyncActions(Function<AsyncActionsManager, Set<AsyncActionData>> actionSupplier,
										   String messageToLog) throws InterruptedException
	{
		if ((!isAsyncEnabled()) || (isExecutionInterrupted()))
			return;

		Set<AsyncActionData> actions = actionSupplier.apply(asyncManager);
		if ((actions == null) || (actions.isEmpty()))
			return;

		getLogger().debug(messageToLog);
		waitForAsyncActions(actions);
	}
	
	public void interruptExecution()
	{
		interrupted = true;
		if (isAsyncEnabled())
			asyncManager.interruptExecution();
	}
	
	public boolean isExecutionInterrupted()
	{
		return interrupted;
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	protected GlobalContext getGlobalContext()
	{
		return globalContext;
	}
	
	
	protected AsyncActionsManager getAsyncManager()
	{
		return asyncManager;
	}
	
	protected boolean isAsyncEnabled()
	{
		return asyncManager != null;
	}
	
	protected AsyncActionsManager createAsyncManager()
	{
		return new AsyncActionsManager(globalContext, this::actionToMvel);
	}
	
	public static boolean isAsyncAction(Action action)
	{
		return (action.isAsync()) && (!action.isSubaction()) && (!(action instanceof SchedulerPause));
	}
	
	protected AsyncActionData createAsyncActionData(Action action, StepContext stepContext, MatrixContext matrixContext)
	{
		return new AsyncActionData(action, stepContext, matrixContext);
	}
	
	protected int getAsyncEndWaitInterval()
	{
		return 1000;
	}
	
	protected void updateActionAfterAsyncFinish(AsyncActionData actionData)
	{
		Action action = actionData.getAction();
		action.setStarted(actionData.getStarted());
		action.setFinished(actionData.getFinished());
		action.setResult(actionData.getResult());
		applyActionResult(action, false);
		applyStepSuccess(action);
		
		action.setPayloadFinished(true);
	}
	
	protected void updateAsyncActions(Collection<Action> actions)
	{
		reportWriter.updateReports(actions, actionsReportsDir);
		
		for (Action action : actions)
		{
			handleActionResult(action);
			processActionResult(action);
			
			// If asynchronous action is finished and failed, need to decrement number of successful actions for certain step
			// because all async actions are treated as passed on start of their executions
			Result result = action.getResult();
			if (result != null && !result.isSuccess())
				action.getStep().getExecutionProgress().decrementSuccessful();
			
			afterAsyncAction(action);
			cleanAfterAsyncAction(action);
		}
	}
	
	/**
	 * Update step success before it ends.
	 */
	protected void applyStepSuccess(Action action)
	{
		Step step = action.getStep();
		step.setAnyActionFailed(step.isAnyActionFailed() || !action.isPassed());
	}

	protected void afterAsyncAction(@SuppressWarnings("unused") Action action) { /* Nothing to do by default*/ }
	
	protected void cleanAfterAsyncAction(Action action)
	{
		action.dispose();
		cleanContexts(action);
	}
	
	protected void waitForAsyncActions(Set<AsyncActionData> actions) throws InterruptedException
	{
		int asyncEndWaitInterval = getAsyncEndWaitInterval();
		for (AsyncActionData action : actions)
		{
			while (!asyncManager.isActionFinished(action))
			{
				Thread.sleep(asyncEndWaitInterval);
				//InterruptedException may have occurred in other place (in callActionAsync(), for example) so that sleep ends correctly.
				//But if execution is interrupted, need to break the loop anyway
				if (isExecutionInterrupted())
					break;
			}
			
			if (isExecutionInterrupted())
				break;
		}
		actions.clear();  //All actions finished. Clearing history to free memory
	}
	
	
	protected void handleActionCrash(Action action, Exception e)
	{
		//Action crashed. Write error to log, store fact of crash and set Matrix to FAILED
		String actionId = action.getIdInMatrix();
		Matrix matrix = action.getMatrix();
		String stepName = action.getStepName();
		
		if (getLogger().isErrorEnabled())
			getLogger().error(action.getDescForLog("Error while running"), e);
		
		Result result = DefaultResult.failed(e);
		result.setCrashed(true);
		
		action.setResult(result);
		action.setFinished(new Date());
		
		if (action.isSubaction())
		{
			SubActionData subActionData = new SubActionData(action);
			matrix.getContext().setSubActionData(actionId, subActionData);
			subActionData.setException(e);
		}
		
		matrix.setStepSuccessful(stepName, false);
		matrix.addStepStatusComment(stepName, "One or more actions CRASHED");
		
		if (action.isAsync())
				action.setPayloadFinished(true);
		reportWriter.writeReport(action, actionsReportsDir, action.getStep().getSafeName());
		handleActionResult(action);
	}
	
	
	protected void handleDuplicateParameters(Action action, SubActionData subActionData)
	{
		StringBuilder comment = new StringBuilder("Duplicate parameters: ");
		for (int i = 0; i < action.getDuplicateParams().size(); i++)
		{
			if (i > 0)
				comment.append(", ");
			comment.append("'" + action.getDuplicateParams().get(i) + "'");
		}
		comment.append(". Check action string in matrix");

		if (!action.isSubaction())
		{
			Result actionResult = DefaultResult.failed(comment.toString());
			actionResult.setFailReason(FailReason.CALCULATION);
			
			action.setResult(actionResult);
		}
		else
			subActionData.setFailedComment(comment.toString());
	}
	
	protected void handleErrorsInParameters(Action action, SubActionData subActionData, List<String> errorsInParams)
	{
		StringBuilder comment = new StringBuilder("Could not calculate the following parameters: \r\n");
		for (String error : errorsInParams)
			comment.append(error).append("\r\n");
		comment.append("Check if all references and function calls are correct and all referenced actions are successful");
		
		if (!action.isSubaction())
		{
			Result actionResult = new DefaultResult();
			actionResult.setSuccess(false);
			actionResult.setFailReason(FailReason.CALCULATION);
			actionResult.setComment(comment.toString());
			action.setResult(actionResult);
			if (action.isAsync())
			{
				/*
					if the calculated parameters have an error message and action is async,
					then we cannot create AsyncActionsManager and shouldn't write pre/post report data
				 */
				action.setPayloadFinished(true);
				action.getStep().refreshAsyncFlag(action);
			}
		}
		else
			subActionData.setFailedComment(comment.toString());
	}
	
	/**
	 * Handles failover exception that occurred while executing current action.
	 * @return {@code false}, if action should be restarted; {@code true} otherwise.
	 */
	protected boolean handleFailoverException(Action action, String actionDesc, FailoverException exception,
			StepContext stepContext, MatrixContext matrixContext) throws InterruptedException
	{
		if (ignoreAllConnectionsFailures)
		{
			action.setResult(DefaultResult.failed(ACTION_SKIP_MSG_PREFIX + "all connections failures.", exception));
			return true;
		}
		else if (connectionsToIgnoreFailures.contains(exception.getConnectionName()))
		{
			action.setResult(DefaultResult.failed(ACTION_SKIP_MSG_PREFIX + "all failures for connection '"
					+ exception.getConnectionName() + "'.", exception));
			return true;
		}
		
		action.getStep().actionFailover(action, exception, stepContext, matrixContext, globalContext);  // Disposing connections according to action type, if needed
		synchronized (failoverStatus)
		{
			failoverStatus.failover = true;
			failoverStatus.actionType = action.getActionType();
			failoverStatus.reason = exception.getReason();
			failoverStatus.reasonString = exception.getMessage();
			failoverStatus.connectionName = exception.getConnectionName();
			failoverStatus.needRestartAction = true;
			failoverStatus.needSkipAction = false;
			failoverStatus.setFailoverInfo(action, exception);
			failoverStatus.wait(); // needRestartAction and needSkipAction may be changed in automationBean
			
			if (failoverStatus.needSkipAction)
			{
				action.setResult(DefaultResult.failed(ACTION_SKIP_MSG_PREFIX + "current failure.", exception));
				getLogger().info("Skipping{}", actionDesc);
				return true;
			}
			else
			{
				if (failoverStatus.needRestartAction)
					getLogger().info("Restarting{}", actionDesc);
				return !failoverStatus.needRestartAction;
			}
		}
	}
	
	protected void applyActionResult(Action action, boolean countSuccess)
	{
		Matrix matrix = action.getMatrix();
		Result result = action.getResult();
		if (result != null)
		{
			// If action is inverted - invert the result
			if (action.isInverted())
				result.setInverted(true);
			
			if (result.isSuccess())
			{
				incSuccessful(action, countSuccess, matrix);
			}
			else
			{
				// If verification is not successful - mark this matrix and step as FAILED
				markAsFailed(action, matrix);
			}
			
			action.setPassed(result.isSuccess());
		}
		else
		{
			if(!action.isInverted())
			{
				incSuccessful(action, countSuccess, matrix);
			}
			else
			{
				markAsFailed(action, matrix);
			}
			action.setPassed(!action.isInverted());
		}
	}

	private void markAsFailed(Action action, Matrix matrix)
	{
		String stepName = action.getStepName();
		matrix.setStepSuccessful(stepName, false);
		matrix.addStepStatusComment(stepName, "One or more actions failed");
	}

	private void incSuccessful(Action action, boolean countSuccess, Matrix matrix)
	{
		if (!action.isSubaction() && countSuccess)
		{
			executionProgress.incrementSuccessful();
			matrix.incActionsSuccess();
		}
	}

	protected void processActionResult(Action action)
	{
		Result result = action.getResult();
		if (result == null)
			return;
		
		if (globalContext.getLoadedContext(GlobalContext.TEST_MODE) == null)
		{
			if (!saveDetailedResult)
				result.clearDetails();
			result.clearLinkedMessages();
		}
	}
	
	protected SubActionData createSubActionData(Action action)
	{
		return new SubActionData(action);
	}
	
	protected void actionToMvel(Action action)
	{
		if (getLogger().isTraceEnabled())
			getLogger().trace(action.getDescForLog("Adding output params of"));

		MvelVariables mvelVars = action.getMatrix().getMvelVars();
		mvelVars.saveActionResult(action);
		mvelVars.cleanAfterAction(action);
	}
	
	
	//*** Action execution methods ***
	protected boolean checkAction(Action action, SubActionData subActionData, List<String> errorsInParams, 
			StepContext stepContext, MatrixContext matrixContext)
	{
		if (action.getDuplicateParams() != null && action.duplicateParamsDisabled())
		{
			handleDuplicateParameters(action, subActionData);
			return false;
		}
		else
		{
			Result actionResult = checkAction(action, stepContext, matrixContext);
			if (actionResult != null)
			{
				if (!action.isSubaction())
					action.setResult(actionResult);
				else
					subActionData.setFailedComment(actionResult.getComment());
				
				return false;
			}
			
			if (!CollectionUtils.isEmpty(errorsInParams))
			{
				handleErrorsInParameters(action, subActionData, errorsInParams);
				return false;
			}
		}
		
		return true;
	}
	
	protected void prepareActionInputParams(Action action)
	{
		action.inputParams = UnmodifiableMap.unmodifiableMap(action.inputParams); // To prevent inputParams modification during action execution
	}
	
	protected void prepareToAction(Action action, StepContext stepContext, MatrixContext matrixContext) throws FailoverException
	{
	}
	
	protected void handleTimeout(Action action) throws InterruptedException
	{
		//If you handle timeout in a special way, don't forget to amend ReportStatus to change its actualTimeout and waitBeforeAction fields
		if ((!(action instanceof TimeoutAwaiter) || !((TimeoutAwaiter)action).isUsesTimeout()) && (action.getTimeOut() > 0))
			Thread.sleep(action.getTimeOut());
	}
	
	protected void checkActionResult(Action action) throws FailoverException
	{
		Result result = action.getResult();
		if ((result != null) && (result.getFailoverData() != null))
			throw result.getFailoverData();
		
		String pauseDesc = null;
		if (action instanceof SchedulerPause)
			pauseDesc = ((SchedulerPause) action).getPauseDescription();
		else if (action.isSuspendIfFailed() && result != null && !result.isSuccess())
			pauseDesc = "Action with ID '" + action.getIdInMatrix() + "' from matrix '" + action.getMatrix().getName() + "' failed";
		
		if (pauseDesc != null)
			action.getStep().pauseAction(pauseDesc);
	}

	protected boolean doExecuteAction(Action action, List<String> errorsInParams, StepContext stepContext,	AtomicBoolean canReplay) throws InterruptedException
	{
		String actionId = action.getIdInMatrix();
		Matrix matrix = action.getMatrix();
		MatrixContext matrixContext = matrix.getContext();
		SubActionData subActionData = null;
		String actionDesc = null;
		
		action.setDone(true);
		if (getLogger().isInfoEnabled())
		{
			actionDesc = action.getDescForLog("");
			getLogger().info("Running{}", actionDesc);
		}
		
		//Run action
		if (!action.isSubaction())
		{
			executionProgress.incrementDone();
			matrix.incActionsDone();
		}
		else
		{
			subActionData = createSubActionData(action);
			matrixContext.setSubActionData(actionId, subActionData);
		}
		
		boolean needReturn = false;  //Indicates whether this method should end after action execution.
		//Usable when action has failed due to failover and has been aborted by user, but should be visible in report
		//If action shouldn't be executed due to some reason which needs to be shown in report - form appropriate result and skip action execution
		if (checkAction(action, subActionData, errorsInParams, stepContext, matrixContext))
		{
			//No errors found, action should be executed
			boolean passed;
			do
			{
				passed = true;
				try
				{
					prepareActionInputParams(action);
					prepareToAction(action, stepContext, matrixContext);  //Creating connections according to action type, if needed
					action.setStarted(new Date());
					handleTimeout(action);
					handleAction(action);  //Handling actual action start
					if (isAsyncAction(action))
						callActionAsync(action, stepContext, matrixContext);
					else
						callAction(action, stepContext, matrixContext);
					
					if (isFailedReplayableAction(action))
						canReplay.set(true);
					checkActionResult(action);
				}
				catch (FailoverException e)
				{
					try
					{
						passed = handleFailoverException(action, actionDesc, e, stepContext, matrixContext);
					}
					catch (InterruptedException e1)
					{
						getLogger().warn("Handling action's failover interrupted", e1);
						action.getStep().interruptExecution();
						synchronized (failoverStatus)
						{
							failoverStatus.failover = false;
						}
						
						if (action.getResult() == null)
							return false;
						else
						{
							needReturn = true;
							break;
						}
					}
				}
			}
			while (!passed);
		}
		
		applyActionResult(action, true);

		if (!action.isSubaction())
		{
			actionToMvel(action);
			reportWriter.writeReport(action, actionsReportsDir, action.getStep().getSafeName());
		}
		else
		{
			if (action.getSubActionData() != null && action.getSubActionData().size() > 0)
				matrixContext.getSubActionData(actionId).setSubActionData(action.getSubActionData());
		
			if (!action.isPassed())
				reportWriter.writeReport(action, actionsReportsDir, action.getStep().getSafeName());
		}
		
		handleActionResult(action);
		
		processActionResult(action);
		applyStepSuccess(action);
		
		if (getLogger().isDebugEnabled())
			getLogger().debug("Finished{}", actionDesc != null ? actionDesc : action.getDescForLog(""));
		
		//If action had failed due to failover and has been aborted by user, but returned a result: 
		//result is written to report, now it's time to end the step due to abortion
		if (needReturn)
			return false;
		return true;
	}
	
	private boolean handleAction(Action action)
	{
		if (!executionHandler.isActive())
		{
			logger.trace("Skipped handling execution of action '{}' ({})", action.getIdInMatrix(), action.getName());
			return false;
		}
		
		try
		{
			logger.trace("Handling execution of action '{}' ({})", action.getIdInMatrix(), action.getName());
			HandledTestExecutionId executionId = executionHandler.onAction(action);
			action.setTestExecutionId(executionId);
		}
		catch (Exception e)
		{
			logger.warn(String.format("Error occurred while handling execution of action '%s' (%s)", action.getIdInMatrix(), action.getName()), e);
		}
		return true;
	}
	
	private void handleActionResult(Action action)
	{
		//If action start wasn't handled for some reason, handle it now else action result cannot be handled properly
		if (action.getTestExecutionId() == null)
		{
			if (!handleAction(action))
				return;
		}
		
		try
		{
			logger.trace("Handling result of action '{}' ({})", action.getIdInMatrix(), action.getName());
			executionHandler.onActionResult(action.getResult(), action);
		}
		catch (Exception e)
		{
			logger.warn(String.format("Error occurred while handling result of action '%s' (%s)", action.getIdInMatrix(), action.getName()), e);
		}
	}
	
	protected void handleNonExecutableAction(Action action, boolean stepExecutable)
	{
		MvelVariables variables = action.getMatrix().getMvelVars();
		
		if (stepExecutable && (action.getFormulaExecutable() != null))
		{
			Result actionResult = new DefaultResult();
			actionResult.setSuccess(false);
			actionResult.setFailReason(FailReason.NOT_EXECUTED);
			action.setResult(actionResult);
			variables.saveOutputParams(action);
			
			if (action.isAsync())
				action.setPayloadFinished(true);
			reportWriter.writeReport(action, actionsReportsDir, action.getStep().getSafeName());
			handleActionResult(action);
		}
		variables.cleanAfterAction(action);
	}
	
	protected void cleanContexts(Action action)
	{
		if (action.getCleanableContext() == null)
			return;
		
		MatrixContext mc = action.getMatrix().getContext();
		for (String clCont : action.getCleanableContext())
			mc.setContext(clCont, null);
	}
	
	
	protected boolean isFailedReplayableAction(Action action)
	{
		return false;
	}
	
	protected Result checkAction(Action action, StepContext stepContext, MatrixContext matrixContext)
	{
		return null;
	}

	public void afterActionsExecution(Step step)
	{
		reportWriter.makeReportsEnding(actionsReportsDir, step.getSafeName());
	}
}
