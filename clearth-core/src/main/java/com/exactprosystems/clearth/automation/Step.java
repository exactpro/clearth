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

import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.utils.BinaryConverter;
import com.exactprosystems.clearth.utils.csv.writers.ClearThCsvWriter;
import com.exactprosystems.clearth.utils.javaFunction.BiConsumerWithException;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.exactprosystems.clearth.automation.ActionExecutor.isAsyncAction;

public abstract class Step implements CsvDataManager
{
	protected String parameter;
	protected boolean executable;
	protected volatile boolean async;
	protected String comment = "";
	
	protected final List<Action> actions = new ArrayList<>();
	protected final Set<Action> asyncActions = new HashSet<>();
	protected volatile Iterator<Action> actionsIterator = null;
	protected volatile Action currentAction = null;
	protected String actionsReportsDir = null;

	protected boolean interrupted = false, paused = false;
	protected AtomicBoolean anyActionFailed = new AtomicBoolean(false);
	protected AtomicBoolean failedDueToError = new AtomicBoolean(false);
	protected SchedulerSuspension suspension = null;
	protected Map<Matrix, StepContext> stepContexts = null;
	protected String statusComment = null;
	protected Throwable error = null;
	protected Result result = null;

	protected StartAtType startAtType = StartAtType.DEFAULT;
	protected boolean waitNextDay = false;
	protected String safeName = null;
	
	protected boolean actionPause = false;
	protected String actionPauseDescription = null;
	protected StepData stepData = new StepData();


	public Step()
	{
	}

	public Step(String name, String kind, String startAt, StartAtType startAtType, boolean waitNextDay, String parameter,
			boolean askForContinue, boolean askIfFailed, boolean execute, String comment)
	{
		stepData.setName(name);
		safeName = name;
		stepData.setKind(kind);
		stepData.setStartAt(startAt);
		this.parameter = parameter;
		stepData.setAskForContinue(askForContinue);
		stepData.setAskIfFailed(askIfFailed);
		stepData.setExecute(execute);
		this.startAtType = startAtType;
		this.waitNextDay = waitNextDay;
		setComment(comment);
	}

	public Step(Map<String, String> record) throws IOException
	{
		assignFields(record);
	}


	@Override
	public void assignFields(Map<String, String> record) throws IOException
	{
		stepData.assignBasicFields(record);
		safeName = stepData.getName();
		setStartAtTypeString(record.get(StepParams.START_AT_TYPE.getValue()));
		parameter = record.get(StepParams.PARAMETER.getValue());
		setComment(record.get(StepParams.COMMENT.getValue()));
		String waitNextD = record.get(StepParams.WAIT_NEXT_DAY.getValue());
		waitNextDay = !waitNextD.isEmpty() && !waitNextD.equals("0");
	}

	protected abstract Logger getLogger();
	
	public abstract void init();
	public abstract void initBeforeReplay();
	
	protected abstract void beforeActions(GlobalContext globalContext);
	protected abstract void beforeAction(Action action, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext);
	protected abstract void actionFailover(Action action, FailoverException e, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext);
	protected abstract void afterAction(Action action, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext);
	protected abstract void afterActions(GlobalContext globalContext, Map<Matrix, StepContext> stepContexts);

	public String getName()
	{
		return stepData.getName();
	}

	public void setName(String name)
	{
		stepData.setName(name);
	}

	public String getSafeName()
	{
		return safeName;
	}

	public void setSafeName(String fileName)
	{
		this.safeName = fileName;
	}

	public String getKind()
	{
		return stepData.getKind();
	}
	
	public void setKind(String kind)
	{
		stepData.setKind(kind);
	}
	
	
	public String getStartAt()
	{
		return stepData.getStartAt();
	}
	
	public void setStartAt(String startAt)
	{
		stepData.setStartAt(startAt != null ? startAt.trim() : null);
	}
	
	public StartAtType getStartAtType()
	{
		return startAtType;
	}
	
	
	public String getStartAtTypeString()
	{
		return startAtType.getStringType();
	}

	public void setStartAtTypeString(String startAtType)
	{
		this.startAtType = StartAtType.getValue(startAtType);
	}
	
	
	public boolean isWaitNextDay()
	{
		return this.waitNextDay;
	}
	
	public void setWaitNextDay(boolean waitNextDay)
	{
		this.waitNextDay = waitNextDay;
	}
	
	
	public String getParameter()
	{
		return parameter;
	}
	
	public void setParameter(String parameter)
	{
		this.parameter = parameter;
	}


	public boolean isAskForContinue()
	{
		return stepData.isAskForContinue();
	}

	public void setAskForContinue(boolean askForContinue)
	{
		stepData.setAskForContinue(askForContinue);
	}


	public boolean isAskIfFailed()
	{
		return stepData.isAskIfFailed();
	}

	public void setAskIfFailed(boolean askIfFailed)
	{
		stepData.setAskIfFailed(askIfFailed);
	}

	/**
	 * @return True if execute flag is true
	 */
	public boolean isExecute()
	{
		return stepData.isExecute();
	}

	/**
	 * @return True if step is execute and if at least one action in actions is executable.
	 */
	protected boolean isExecutable()
	{
		return executable;
	}
	
	public void setExecute(boolean execute)
	{
		stepData.setExecute(execute);
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		if(comment != null)
			comment = comment.replaceAll("[\r\n\t]+"," ");
		this.comment = comment;
	}

	public List<Action> getActions()
	{
		return Collections.unmodifiableList(actions);
	}
	
	public String getActionsReportsDir()
	{
		return actionsReportsDir;
	}


	public Date getStarted()
	{
		return stepData.getStarted();
	}
	
	public void setStarted(Date started)
	{
		stepData.setStarted(started);
	}
	
	
	public Date getFinished()
	{
		return stepData.getFinished();
	}
	
	public void setFinished(Date finished)
	{
		stepData.setFinished(finished);
	}
	
	public boolean isEnded()
	{
		return getFinished() != null;
	}
	
	
	public ActionsExecutionProgress getExecutionProgress()
	{
		return stepData.getExecutionProgress();
	}
	
	public void setExecutionProgress(ActionsExecutionProgress executionProgress)
	{
		stepData.setExecutionProgress(executionProgress);
	}

	public boolean isAnyActionFailed()
	{
		return anyActionFailed.get();
	}

	public void setAnyActionFailed(boolean anyActionFailed)
	{
		this.anyActionFailed.set(isAnyActionFailed() || anyActionFailed);
	}

	public boolean isFailedDueToError()
	{
		return failedDueToError.get();
	}

	public void setFailedDueToError(boolean failedDueToError)
	{
		// If Step failed once it cannot be not failed anymore.
		this.failedDueToError.set(isFailedDueToError() || failedDueToError);
	}

	public void interruptExecution()
	{
		interrupted = true;
	}
	
	public boolean isInterrupted()
	{
		return interrupted;
	}
		
	
	public boolean isPaused()
	{
		return paused;
	}

	public void pause()
	{
		this.paused = true;
	}

	public String getStatusComment()
	{
		return statusComment;
	}
	
	public void setStatusComment(String statusComment)
	{
		this.statusComment = statusComment;
	}
	
	
	public Throwable getError()
	{
		return error;
	}
	
	public void setError(Throwable error)
	{
		this.error = error;
		if(error != null)
		{
			setFailedDueToError(true);
		}
	}

	public Result getResult() {
		return result;
	}

	public void setResult(Result result) {
		this.result = result;
	}


	public Map<Matrix, StepContext> getStepContexts()
	{
		return stepContexts;
	}
	
	public void setStepContexts(Map<Matrix, StepContext> stepContexts)
	{
		this.stepContexts = stepContexts;
	}

	public boolean isActionPause()
	{
		return actionPause;
	}

	public String getActionPauseDescription()
	{
		return actionPauseDescription;
	}

	public StepData getStepData()
	{
		return stepData;
	}

	@Override
	public void save(ClearThCsvWriter writer) throws IOException
	{
		writer.write(stepData.getName());
		writer.write(stepData.getKind());
		writer.write(stepData.getStartAt());
		writer.write(startAtType.getStringType());
		writer.write(BinaryConverter.getBinaryStringFromBoolean(waitNextDay));
		writer.write(parameter);
		writer.write(BinaryConverter.getBinaryStringFromBoolean(stepData.isAskForContinue()));
		writer.write(BinaryConverter.getBinaryStringFromBoolean(stepData.isAskIfFailed()));
		writer.write(BinaryConverter.getBinaryStringFromBoolean(stepData.isExecute()));
		writer.write(comment);
	}

	public static String getValidFileName(String fileName) {
		return fileName.replaceAll("[.\\\\/:*?\"<>|']", "_");
	}

	protected void pauseStep()
	{
		try
		{
			synchronized (suspension)
			{
				suspension.setReplayStep(false);
				suspension.setSuspended(true);
				suspension.wait();
				
				this.paused = false;
			}
		}
		catch (InterruptedException e)
		{
			getLogger().error("Wait interrupted", e);
		}
	}
	
	protected void pauseAction(String pauseDescription)
	{
		actionPauseDescription = pauseDescription;
		actionPause = true;
		pauseStep();
	}
	
	protected void checkContextsExist()
	{
		if (stepContexts == null)
			stepContexts = new LinkedHashMap<Matrix, StepContext>();
	}
	
	protected StepContext createStepContext(String stepName, Date started)
	{
		return new StepContext(stepName, started);
	}
	
	protected StepContext getStepContext(Matrix matrix)
	{
		checkContextsExist();
		
		StepContext stepContext = stepContexts.get(matrix);
		if (stepContext == null)
		{
			stepContext = createStepContext(stepData.getName(), stepData.getStarted());
			stepContexts.put(matrix, stepContext);
		}
		
		return stepContext;
	}
	
	protected void updateByAsyncActions(ActionExecutor actionExec)
	{
		actionExec.checkAsyncActions();
	}

	protected void waitForAsyncActions(ActionExecutor actionExec,
			BiConsumerWithException<ActionExecutor, String, InterruptedException> waitMethod)
	{
		if (!interrupted)
		{
			try
			{
				waitMethod.accept(actionExec, this.getName());
			}
			catch (InterruptedException e)
			{
				getLogger().warn("Wait for async actions interrupted", e);
			}
		}

		updateByAsyncActions(actionExec);
	}
	

	public void executeActions(ActionExecutor actionExec, String actionsReportsDir, BooleanObject replay, SchedulerSuspension suspension)
	{
		waitForAsyncActions(actionExec, ActionExecutor::waitForBeforeStepAsyncActions);

		this.actionsReportsDir = actionsReportsDir;
		Logger logger = getLogger();
		checkContextsExist();
		if (actions.isEmpty())
		{
			logger.info("No actions for step {}", this.getName());
			return;
		}
		
		GlobalContext globalContext = actionExec.getGlobalContext();
		this.suspension = suspension;
		try
		{
			interrupted = false;
			//paused = false;
			beforeActions(globalContext);

			//Resetting all internal counters to prepare execution of actions from this particular step
			actionExec.reset(actionsReportsDir, stepData.getExecutionProgress());

			AtomicBoolean canReplay = new AtomicBoolean(false);
			logger.info("Running actions for step '{}'", this.getName());
			actionsIterator = createActionsIterator();
			while (actionsIterator.hasNext())
			{
				currentAction = actionsIterator.next();
				try
				{
					if (currentAction.getFinished() != null)  // If we replay the step and this action is already done
						continue;
					
					if (paused)
						this.pauseStep();
					
					if (interrupted)
						break;
					
					actionExec.prepareToAction(currentAction);
					
					Matrix matrix = currentAction.getMatrix();
					MatrixContext matrixContext = matrix.getContext();
					StepContext stepContext = getStepContext(matrix);
					
					beforeAction(currentAction, stepContext, matrixContext, globalContext);
					
					if (replay.getValue() && !actionExec.prepareActionReplay(currentAction))
						continue;

					//Need to "execute actions" even if step is not executable, because actions may need to set some parameters referenced by further actions
					actionExec.executeAction(currentAction, stepContext, canReplay);
					
					afterAction(currentAction, stepContext, matrixContext, globalContext);
					updateByAsyncActions(actionExec);
				}
				finally
				{
					if (!isAsyncAction(currentAction))
						currentAction.dispose();
				}
			}
			actionsIterator = null;
			
			actionExec.afterActionsExecution(this);
			waitForAsyncActions(actionExec, ActionExecutor::waitForStepAsyncActions);
			replay.setValue(canReplay.get());
		}
		finally
		{
			afterActions(globalContext, stepContexts);
		}
	}

	public void clearContexts()
	{
		if(stepContexts != null)
		{
			stepContexts.values().forEach(StepContext::clearContext);
			stepContexts.clear();
		}
	}

	public boolean isAsync()
	{
		return async;
	}

	public void addAction(Action action)
	{
		actions.add(action);
		if (stepData.isExecute() && !executable && action.isExecutable())
			executable = true;
		if (executable && action.isAsync())
		{
			asyncActions.add(action);
			if (!async)
				async = true;
		}
	}

	public void clearActions()
	{
		clearSyncActions();
		asyncActions.clear();
		executable = async = false;
	}
	
	public void clearSyncActions()
	{
		actions.clear();
	}

	public void setActions(List<Action> actions)
	{
		clearActions();
		this.actions.addAll(actions);
		asyncActions.addAll(actions.stream().filter(Action::isAsync).collect(Collectors.toList()));
		executable = stepData.isExecute() && !actions.isEmpty() && actions.stream().anyMatch(Action::isExecutable);
		async = !asyncActions.isEmpty();
		
		if (actionsIterator != null)
		{
			actionsIterator = createActionsIterator();
			currentAction = null;
		}
	}
	
	public void rewindToAction(Action action)
	{
		if (action.getStep() != this)
			throw new IllegalArgumentException("Given action (ID="+action.getIdInMatrix()+") is not from this global step");
		
		if (actionsIterator == null)
			throw new IllegalStateException("This global step is not being executed");
		
		Iterator<Action> newIterator = createActionsIterator();
		while (newIterator.hasNext())
		{
			Action nextAction = newIterator.next();
			if (nextAction == action)
			{
				actionsIterator = newIterator;
				currentAction = action;
				return;
			}
		}
		throw new IllegalArgumentException("Given action (ID="+action.getIdInMatrix()+") is not found in this global step");
	}
	
	public void refreshAsyncFlag(Action asyncAction)
	{
		synchronized (asyncActions)
		{
			if (asyncActions.remove(asyncAction))
			{
				async = !asyncActions.isEmpty();
			}
		}
	}
	
	public Action getCurrentAction()
	{
		return currentAction;
	}
	
	
	private Iterator<Action> createActionsIterator()
	{
		return actions.iterator();
	}
	
	
	public enum StepParams
	{
		GLOBAL_STEP ("Global step"),
		STEP_KIND ("Step kind"),
		START_AT ("Start at"),
		START_AT_TYPE ("Start at type"),
		WAIT_NEXT_DAY("Wait next day"),
		PARAMETER ("Parameter"),
		COMMENT ("Comment"),
		ASK_FOR_CONTINUE ("Ask for continue"),
		ASK_IF_FAILED ("Ask if failed"),
		EXECUTE ("Execute"),
		STARTED ("Started"),
		ACTIONS_SUCCESSFUL ("Actions successful"),
		FINISHED ("Finished");


		private final String value;

		StepParams(String value)
		{
			this.value = value;
		}

		public String getValue()
		{
			return value;
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Step step = (Step) o;

		if (!stepData.equals(step.stepData))
			return false;
		if (waitNextDay != step.waitNextDay)
			return false;
		if (!Objects.equals(parameter, step.parameter))
			return false;
		if (!Objects.equals(comment, step.comment))
			return false;
		return startAtType == step.startAtType;
	}

	@Override
	public int hashCode()
	{
		int result = Objects.hashCode(stepData);
		result = 31 * result + Objects.hashCode(parameter);
		result = 31 * result + Objects.hashCode(comment);
		result = 31 * result + Objects.hashCode(startAtType);
		result = 31 * result + (waitNextDay ? 1 : 0);
		return result;
	}
}
