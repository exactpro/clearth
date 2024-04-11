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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.exceptions.ParametersException;
import com.exactprosystems.clearth.automation.report.ActionReportWriter;
import com.exactprosystems.clearth.automation.report.ReportException;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.automation.report.ReportsWriter;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.automation.steps.Default;
import com.exactprosystems.clearth.data.HandledTestExecutionIdStorage;
import com.exactprosystems.clearth.data.HandledTestExecutionId;
import com.exactprosystems.clearth.data.TestExecutionHandler;
import com.exactprosystems.clearth.data.TestExecutionHandlingException;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.utils.ObjectWrapper;
import com.exactprosystems.clearth.utils.Utils;
import com.exactprosystems.clearth.xmldata.XmlMatrixInfo;
import com.exactprosystems.clearth.xmldata.XmlSchedulerLaunchInfo;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

public abstract class SimpleExecutor extends Thread implements IExecutor
{
	protected static final String format = "HH:mm:ss",
			REPORTDIR_COMPLETED = "completed",
			REPORTDIR_ACTIONS = "actions";

	protected static final SimpleDateFormat hmsFormatter = new SimpleDateFormat(format);

	protected final Scheduler scheduler;
	protected final List<Step> steps;
	protected final List<Matrix> matrices;
	protected final SchedulerStatus status;
	protected final GlobalContext globalContext;
	protected final FailoverStatus failoverStatus;
	protected final Map<String, Preparable> preparableActions;
	protected final ActionParamsCalculator paramsCalculator;
	protected final TestExecutionHandler executionHandler;
	protected final ActionExecutor actionExecutor;
	protected final List<StepData> stepData;
	protected final ReportsConfig reportsConfig;

	private HandledTestExecutionIdStorage handledIdStorage;
	private Map<String, String> fixedIds = null;  //Contains action IDs fixed for MVEL so that they can start with digits or underscores
	
	private AtomicBoolean terminated = new AtomicBoolean(false), //Must be set on interruption: on terminate() method call and on throw of InterruptedException
			interrupted = new AtomicBoolean(false),
			paused = new AtomicBoolean(false);

	private final SchedulerSuspension suspension = new SchedulerSuspension(false, false, false);
	private Step currentStep;
	private boolean restored = false;
	
	//This is used to highlight idle mode
	private boolean idle;
	
	private Date started, ended;
	private final SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	protected String reportsDir, specificDir, completedReportsDir, actionsReportsDir;
	protected volatile ReportsInfo lastReportsInfo = null;
	private Object executionMonitor = null;
	protected File storedActionsReportsDir;

	private Timer sleepTimer = null;
	private volatile long startTimeStep = 0L;
	private Consumer<SimpleExecutor> onFinish;

	public SimpleExecutor(Scheduler scheduler, List<Step> steps, List<Matrix> matrices, GlobalContext globalContext,
			FailoverStatus failoverStatus, Map<String, Preparable> preparableActions, ReportsConfig reportsConfig)
	{
		super(scheduler.getName());
		
		this.scheduler = scheduler;
		this.steps = steps;
		this.matrices = matrices;
		this.status = scheduler.getStatus();
		this.globalContext = globalContext;
		this.failoverStatus = failoverStatus;
		this.preparableActions = preparableActions;
		this.paramsCalculator = createParamsCalculator();
		this.executionHandler = globalContext.getExecutionHandler();
		this.reportsConfig = new ReportsConfig(reportsConfig);
		this.actionExecutor = createActionExecutor();
		this.stepData = new ArrayList<>(steps.size());
	}

	protected abstract Logger getLogger();
	
	protected abstract boolean createConnections() throws Exception;
	protected abstract boolean loadMappingsAndSettings() throws Exception;
	protected abstract StepImpl createStepImpl(String stepKind, String stepParameter);
	protected abstract void closeConnections();
	
	protected abstract void prepareToTryAgainMain();
	protected abstract void prepareToTryAgainAlt();

	protected abstract ReportsWriter initReportsWriter(String pathToStoreReports, String pathToActionsReports);


	@Override
	public void run()
	{
		try
		{
			Logger logger = getLogger();

			currentStep = null;
			suspension.setSuspended(false);
			//terminated = false;

			started = Calendar.getInstance().getTime();
			scheduler.setLastExecutorStartedTime(started);
			globalContext.setStarted(started);
			getLogger().info("Start of scheduler execution");
			ended = null;
			
			//Let's put reports into public directory: all images inside a report will be downloadable
			setOutputPaths();

			//Explicitly creating directory for parts of reports so that it will be available in all further calls
			Files.createDirectories(Paths.get(ClearThCore.appRootRelative(reportsDir)));

			getLogger().info("Version: {}", ClearThCore.getInstance().getVersion());
			
			if (!createConnections())
				return;
			
			if (!loadMappingsAndSettings())
				return;
			
			if (!prepareActions())
				return;
			
			if (!restored)
			{
				for (Step step : steps)
					step.init();
				
				storeStepNames();
			}
			else
				restoreActionsReports();

			for (Matrix matrix : matrices)
				evaluateMatrixInfo(matrix);

			// Remove unused executed matrices from directory
			removeUnusedExecutedMatrices(matrices);

			// Storing executed matrices in scheduler
			saveExecutedMatrices(scheduler, matrices);

			scheduler.setExecutedStepsData(stepData);

			if (!interrupted.get())
			{
				status.add("Executing steps and actions...");
				handleExecutionStart();
				
				for (Step step : steps)
				try
				{
					stepData.add(step.getStepData());

					if (step.isEnded())
						continue;
					
					startTimeStep = 0L;
					if (logger.isTraceEnabled()) {
						logger.trace("Step {} started", step.getName());
					}
					
					currentStep = step;
					
					waitForStepStart(step);
					if (interrupted.get())
						break;
					
					if (paused.get())
					{
						step.pause();
					}
						
					BooleanObject replay = new BooleanObject(false);
					do
					{
						if (replay.getValue())
						{
							step.initBeforeReplay();
							for (Matrix m : matrices)
							{
								if (m.isStepSuccessful(step.getName()))  //Step is successful, no need to replay it
									continue;
								
								m.setStepSuccessful(step.getName(), true);  //Will get here only if m.isStepSuccessful() is already set and is false
								m.setSuccessful(true);
								for (String sn : m.getStepSuccess().keySet())
									if ((!sn.equals(step.getName())) && (!m.isStepSuccessful(sn)))
									{
										m.setSuccessful(false);
										break;
									}
										
								if (m.isSuccessful())
									for (Action action : m.getActions())  //Checking if matrix was failed due to one of actions not from current step
										if ((!step.getActions().contains(action)) && (action.getResult()!=null) && (!action.getResult().isSuccess()))
										{
											m.setSuccessful(false);
											break;
										}
							}
						}
						
						stepStarted(step);
						
						checkStepFileName(step, steps);
						
						StepImpl stepImpl = null;
						if (step.isExecute())
						{
							stepImpl = createStepImpl(step);
							if (stepImpl != null)
							{
								try
								{
									stepImpl.doBeforeActions(globalContext);
								}
								catch (Exception e)
								{
									logger.error("Step '{}' (kind: {}) preparation failed.", step.getName(), step.getKind(), e);
									if (e instanceof AutomationException)
										status.add(format("Step '%s' preparation failed: %s", step.getName(), e.getMessage()));
									else // RuntimeException
										status.add(format("Internal error occurred while preparation of step '%s'. See logs for details.", step.getName()));
									step.setFinished(Calendar.getInstance().getTime());
									continue;
								}
							}
						}
						
						//Need to "execute actions" even if step is not executable, because actions may need to set some parameters referenced by further actions
						step.executeActions(actionExecutor, actionsReportsDir, replay, suspension);
						
						if (interrupted.get())
						{
							step.setFinished(Calendar.getInstance().getTime());
							break;
						}
						
						if (!step.isExecute())
						{
							step.setFinished(Calendar.getInstance().getTime());
							continue;
						}
						
						Result stepResult = null;
						if (stepImpl != null)
						{
							try
							{
								stepResult = stepImpl.execute(step.getStepContexts(), globalContext);
							}
							catch (Exception e)
							{
								getLogger().warn("Step '{}' thrown exception during execution", step.getName(), e);
								stepResult = DefaultResult.failed(e);
							}
						}
						
						step.setFinished(Calendar.getInstance().getTime());
						if (stepResult!=null)
						{
							step.setFailedDueToError(!stepResult.isSuccess());
							step.setStatusComment(stepResult.getComment());
							step.setResult(stepResult);
							if ((stepResult.getError()!=null) && (!(stepResult.getError() instanceof InterruptedException)))
								step.setError(stepResult.getError());
							
							if (!stepResult.isSuccess())
							{
								for (Action action : step.getActions())
								{
									action.getMatrix().setStepSuccessful(step.getName(), false);
								}
							}
						}
						
						if ((stepResult!=null) && (stepResult.getError()!=null) && (stepResult.getError() instanceof InterruptedException))
							interrupted.set(true);
						else if (step.isAskForContinue()|| step.isAskIfFailed() && step.getExecutionProgress().getSuccessful() < step.getExecutionProgress().getDone())
						{
							ended = Calendar.getInstance().getTime();
							lastReportsInfo = null;
							try
							{
								synchronized (suspension)
								{
									suspension.setReplayStep(replay.getValue());  //Will show "Replay step" button in GUI
									suspension.setSuspended(true);
									suspension.wait();
									replay.setValue(suspension.isReplayStep());
								}
							}
							catch (InterruptedException e)
							{
								getLogger().error("Interrupted wait while asking for continue", e);
								interrupted.set(true);
								synchronized (suspension)
								{
									suspension.setReplayStep(false);
									suspension.setSuspended(false);
								}
							}
						}
					}
					while ((!interrupted.get()) && (suspension.isReplayStep()));
					
					step.setFinished(Calendar.getInstance().getTime());
					
					if (logger.isDebugEnabled()) {
						logger.debug("Step {} finished", step.getName());
					}
					
					if (interrupted.get())
						break;
				}
				finally
				{
					stepFinished(step);
				}
				
				if (!interrupted.get())
				{
					waitForAsyncActions();
					status.add("Execution finished");
				}
				else
					status.add("Execution interrupted");
			}
			
			ended = Calendar.getInstance().getTime();

			//Forming reports
			String pathToStoreReports = ClearThCore.appRootRelative(completedReportsDir), //AppRootRelative because we don't want to download file, we want to see it in browser
					pathToActionsReports = ClearThCore.appRootRelative(actionsReportsDir);
			if (reportsConfig.isAnyReportEnabled())
			{
				status.add("Making reports...");
				makeReports(pathToStoreReports, pathToActionsReports);
				status.add("Reports made");
			}
			lastReportsInfo = createReportsInfo(pathToStoreReports);
			scheduler.saveExecutedStepsData();

			//Storing info about this launch to make user able to access it from GUI
			Date finished = Calendar.getInstance().getTime();
			globalContext.setFinished(finished);
			getLogger().info("Execution of the scheduler completed now");
			XmlSchedulerLaunchInfo launchInfo = buildXmlSchedulerLaunchInfo(finished);
			scheduler.addLaunch(launchInfo);
			
			status.add("Finished");
		}
		catch (Exception e)
		{
			getLogger().error("FATAL error occurred", e);
			status.add("FATAL ERROR: "+(e instanceof ParametersException ? e.getMessage() : ExceptionUtils.getDetailedMessage(e)));
			status.add("See log for details");
		}
		finally
		{
			terminated.set(true);
			handleExecutionEnd();

			//Disposing all connections
			disposeConnections();

			if (executionMonitor!=null)
				synchronized (executionMonitor)
				{
					executionMonitor.notify();
				}
			
			//Let scheduler know about execution end so that the scheduler can free this thread's resources.
			if (onFinish != null)
				onFinish.accept(this);

			clearMatricesContexts();
			clearGlobalContexts();

			this.matrices.clear();
			
			clearSteps();

			if (sleepTimer != null) {
				sleepTimer.cancel();
				sleepTimer = null;
			}
			Utils.closeResource(actionExecutor);
			Utils.closeResource(executionHandler);
		}
	}
	
	private void setOutputPaths()
	{
		specificDir = scheduler.getForUser() + "/" + scheduler.getName() + "/" + df.format(started) + "/";
		reportsDir = ClearThCore.getInstance().getReportsPath() + specificDir; //Not File.separator, because browser will transform "\" into unexpected character
		completedReportsDir = reportsDir + REPORTDIR_COMPLETED + "/";
		actionsReportsDir = reportsDir + REPORTDIR_ACTIONS + "/";
	}
	
	public void setOnFinish(Consumer<SimpleExecutor> consumer)
	{
		onFinish = consumer;
	}
	
	protected void stepStarted(Step step)
	{
		step.setStarted(Calendar.getInstance().getTime());
		
		if (!checkExecutionHandler("step start"))
			return;
		
		try
		{
			executionHandler.onGlobalStepStart(new StepMetadata(step.getName(), step.getStarted().toInstant()));
		}
		catch (Exception e)
		{
			getLogger().warn("Error occurred while handling start of global step '"+step.getName()+"'", e);
		}
	}
	
	protected void stepFinished(Step step)
	{
		step.clearContexts();
		step.clearSyncActions();
		
		if (!checkExecutionHandler("step end"))
			return;
		
		try
		{
			executionHandler.onGlobalStepEnd();
		}
		catch (Exception e)
		{
			getLogger().warn("Error occurred while handling end of global step '"+step.getName()+"'", e);
		}
	}
	
	
	protected void clearSteps()
	{
		steps.forEach(Step::clearActions);
	}
	
	
	protected void handleExecutionStart() throws TestExecutionHandlingException
	{
		if (!checkExecutionHandler("scheduler start"))
			return;
		
		List<String> matrixNames = matrices.stream().map(Matrix::getName).collect(Collectors.toList());
		handledIdStorage = executionHandler.onTestStart(matrixNames, globalContext);
		
		status.add(String.format("ID in %s: %s", executionHandler.getName(),
				handledIdStorage != null ? handledIdStorage.getExecutionId() : null));
	}
	
	protected void handleExecutionEnd()
	{
		if (!checkExecutionHandler("scheduler end"))
			return;
		
		try
		{
			executionHandler.onTestEnd();
		}
		catch (Exception e)
		{
			getLogger().warn("Error occurred while handling test end", e);
		}
	}
	
	protected boolean checkExecutionHandler(String eventName)
	{
		boolean result = executionHandler.isActive();
		if (!result)
			getLogger().trace("Test execution handler is inactive, {} handling is skipped", eventName);
		return result;
	}
	
	
	public void clearMatricesContexts()
	{
		matrices.forEach(matrix -> matrix.getContext().clearContext());
	}

	public void clearGlobalContexts()
	{
		globalContext.clearContext();
	}

	private void disposeConnections()
	{
		try
		{
			closeConnections();
		} catch (Exception e)
		{
			getLogger().error("Exception during closing connections.", e);
		}
	}

	@SuppressWarnings("unchecked")
	protected void evaluateMatrixInfo(Matrix matrix) throws Exception
	{
		MatrixFunctions functions = globalContext.getMatrixFunctions();
		Map<String, String> constants = matrix.getConstants(),
				formulas = matrix.getFormulas();

		MvelVariables mvelVars = matrix.getMvelVars();
		if (isNotEmpty(matrix.getDescription()))
		{
			try
			{
				String desc = functions.calculateExpression(matrix.getDescription(), Matrix.DESCRIPTION,
						mvelVars.getVariables(), null, null, new ObjectWrapper(0)).toString();
				matrix.setDescription(desc);
			}
			catch (Exception e)
			{
				throw new ParametersException(format("Could not calculate expressions in description of matrix '%s'",matrix.getName()), e);
			}
		}
		
		if (formulas == null)
			return;
		
		Map<String, String> references = (Map<String, String>)mvelVars.get(Matrix.MATRIX);
		//Evaluating matrix constants if they contain expressions
		for (Entry<String, String> f : formulas.entrySet())
		{
			try
			{
				String value = functions.calculateExpression(f.getValue(), f.getKey(),
						mvelVars.getVariables(), null, null, new ObjectWrapper(0)).toString();
				constants.put(f.getKey(), value);
				references.put(f.getKey(), value);
			}
			catch (Exception e)
			{
				throw new ParametersException(format("Could not calculate value of '%s' constant in matrix '%s'",f.getKey(),matrix.getName()), e);
			}
		}
	}
	
	
	protected ActionParamsCalculator createParamsCalculator()
	{
		return new ActionParamsCalculator(globalContext.getMatrixFunctions());
	}
	
	protected ActionReportWriter createReportWriter()
	{
		return new ActionReportWriter(getReportsConfig(), ClearThCore.getInstance().getReportTemplatesProcessor());
	}
	
	protected ActionExecutor createActionExecutor()
	{
		return new ActionExecutor(globalContext, paramsCalculator, createReportWriter(), failoverStatus,
				scheduler.getSchedulerData().isIgnoreAllConnectionsFailures(), scheduler.getConnectionsToIgnoreFailuresByRun());
	}
	
	protected void waitForAsyncActions()
	{
		if (!interrupted.get())
		{
			try
			{
				actionExecutor.waitForSchedulerAsyncActions();
			}
			catch (InterruptedException e)
			{
				getLogger().warn("Wait for scheduler async actions interrupted", e);
			}
		}
		
		actionExecutor.checkAsyncActions();
	}
	
	
	/**
	 * If step name doesn't comply with requirements for file's name in Windows invalid characters are changed to "_".
	 * The resulting name is saved in step as safeName and used as step's file name 
	 * and as part of container name in Freemarker templates.
	 * If another step has the same safeName this name is complemented by "_" character in the end of name. 
	 * @param step - current step
	 * @param steps - all steps
	 */
	protected void checkStepFileName(Step step, List<Step> steps)
	{
		String validName = Step.getValidFileName(step.getSafeName());
		if (!validName.equals(step.getSafeName()))
		{
			while(true)
			{
				boolean valid = true;
				for (Step st : steps )
				{
					if (st.getSafeName().equals(validName))
					{
						valid = false;
						validName += "_";
						break;
					}
				}
				if (valid)
				{
					step.setSafeName(validName);
					break;
				}
			}
		}
	}
	
	protected boolean prepareActions() throws Exception
	{
		if (preparableActions != null)
			for (Preparable action : preparableActions.values())
				action.prepare(globalContext, status);
		return true;
	}

	protected XmlSchedulerLaunchInfo buildXmlSchedulerLaunchInfo(Date finished)
	{
		XmlSchedulerLaunchInfo launchInfo = ClearThCore.getInstance().getSchedulerFactory().createSchedulerLaunchInfo();
		launchInfo.setStarted(started);
		launchInfo.setFinished(finished);
		launchInfo.setInterrupted(interrupted.get());
		launchInfo.setReportsPath(specificDir + REPORTDIR_COMPLETED);
		launchInfo.getMatricesInfo().addAll(lastReportsInfo.getMatrices());
		launchInfo.setReportsConfig(lastReportsInfo.getXmlReportsConfig());
		boolean successfulRun = true;
		for (XmlMatrixInfo matrixInfo : launchInfo.getMatricesInfo())
			if (!matrixInfo.isSuccessful())
			{
				successfulRun = false;
				break;
			}
		launchInfo.setSuccess(successfulRun);
		return launchInfo;
	}
	
	protected StepImpl createStepImpl(Step step)
	{
		StepImpl stepImpl;
		switch (CoreStepKind.stepKindByLabel(step.getKind()))
		{
			case Default :
				stepImpl = new Default();
				break;
			default :
				stepImpl = createStepImpl(step.getKind(), step.getParameter());
				if (stepImpl==null)
				{
					getLogger().warn("Unknown step kind: '{}'", step.getKind());
				}
		}
		return stepImpl;
	}
	
	@Override
	public List<Step> getSteps()
	{
		return steps;
	}
	
	public List<Matrix> getMatrices()
	{
		return matrices;
	}
	
	public String getStartedByUser()
	{
		return globalContext.getStartedByUser();
	}
	
	public Date getBusinessDay()
	{
		return globalContext.getCurrentDate();
	}
	
	public boolean isWeekendHoliday()
	{
		return globalContext.isWeekendHoliday();
	}
	
	@Override
	public Map<String, Boolean> getHolidays()
	{
		return globalContext.getHolidays();
	}
	
	public HandledTestExecutionId getMatrixExecutionId(String matrixName)
	{
		if (handledIdStorage == null)
			return null;
		return handledIdStorage.getMatrixId(matrixName);
	}
	
	public ReportsConfig getReportsConfig()
	{
		return reportsConfig;
	}
	
	
	@Override
	public void interruptExecution() throws AutomationException
	{
		interrupted.set(true);
		
		if (currentStep!=null)
		{
			currentStep.interruptExecution();
			
			if (currentStep.isPaused())
			{
				synchronized (suspension)
				{
					suspension.setSuspended(false);
					suspension.notify();
				}
			}
		}
		
		actionExecutor.interruptExecution();

		try
		{
			scheduler.saveExecutedStepsData();
		}
		catch (IOException e)
		{
			throw new AutomationException("Executed steps cannot be stored", e);
		}
		finally
		{
			interrupt();
		}
	}

	@Override
	public void pauseExecution()
	{
		synchronized (suspension)
		{
			if (suspension.isTimeout())
			{
				suspension.setSuspended(true);
				return;
			}
		}
		this.paused.set(true);
		
		if (currentStep!=null)
		{
			currentStep.pause();
		}
	}
	
	@Override
	public boolean isExecutionInterrupted()
	{
		return interrupted.get();
	}

	@Override
	public boolean isTerminated()
	{
		return terminated.get();
	}

	@Override
	public Step getCurrentStep()
	{
		return currentStep;
	}
	
	@Override
	public boolean isCurrentStepIdle()
	{
		return idle;
	}

	@Override
	public boolean isSuspended()
	{
		return suspension.isSuspended();
	}
	
	@Override
	public boolean isReplayEnabled()
	{
		return suspension.isReplayStep();
	}

	@Override
	public void continueExecution()
	{
		if (paused.get())
			paused.set(false);
		
		synchronized (suspension)
		{
			suspension.setReplayStep(false);
			suspension.setSuspended(false);
			if (!suspension.isTimeout())
			{
				suspension.notify();
			}
		}
	}
	
	@Override
	public void replayStep()
	{
		synchronized (suspension)
		{
			suspension.setReplayStep(true);  //Will cause step replay in do..while of run()
			suspension.setSuspended(false);
			suspension.notify();
		}
	}
	
	@Override
	public boolean isFailover()
	{
		return failoverStatus.failover;
	}
	
	private void tryAgain()
	{
		failoverStatus.failover = false;
		failoverStatus.notify();
	}
	
	@Override
	public void tryAgainMain()
	{
		synchronized (failoverStatus)
		{
			prepareToTryAgainMain();
			tryAgain();
		}
	}
	
	@Override
	public void tryAgainAlt()
	{
		synchronized (failoverStatus)
		{
			prepareToTryAgainAlt();
			tryAgain();
		}
	}
	
	@Override
	public int getFailoverActionType()
	{
		if (!failoverStatus.failover)
			return ActionType.NONE;
		
		return failoverStatus.actionType;
	}
	
	@Override
	public int getFailoverReason()
	{
		if (!failoverStatus.failover)
			return ActionType.NONE;
		
		return failoverStatus.reason;
	}
	
	@Override
	public String getFailoverReasonString()
	{
		if (!failoverStatus.failover)
			return null;
		
		return failoverStatus.reasonString;
	}
	
	@Override
	public String getFailoverConnectionName()
	{
		if (!failoverStatus.failover)
			return null;
		
		return failoverStatus.connectionName;
	}
	
	@Override
	public void setFailoverRestartAction(boolean needRestart)
	{
		synchronized (failoverStatus)
		{
			failoverStatus.needRestartAction = needRestart;
			tryAgain();
		}
	}
	
	@Override
	public void setFailoverSkipAction(boolean needSkipAction)
	{
		synchronized (failoverStatus)
		{
			failoverStatus.needSkipAction = needSkipAction;
			tryAgain();
		}
	}
	
	
	public boolean isRestored()
	{
		return restored;
	}
	
	public void setRestored(boolean restored)
	{
		this.restored = restored;
	}
	
	
	public Date getStarted()
	{
		return started;
	}

	public void setStarted(Date started)
	{
		this.started = started;
	}
	
	
	public Date getEnded()
	{
		return ended;
	}

	public Date getReportEndTime()
	{
		if (isTerminated())
		{
			return getEnded();
		}

		Date endTime = getEnded();
		for (Step step : getSteps())
		{
			if (!step.isExecute())
				continue;

			if (step.getStarted() == null)
				return endTime;

			if (step.getFinished() != null)
			{
				endTime = step.getFinished();
				continue;
			}

			for (Action action : step.getActions())
			{
				Date actionEndTime = action.getFinished();
				if (actionEndTime == null)
					break;

				if (endTime == null || endTime.before(actionEndTime))
					endTime = actionEndTime;
			}
		}
		return endTime;
	}

	public void setEnded(Date ended)
	{
		this.ended = ended;
	}
	
	
	public Map<String, String> getFixedIds()
	{
		return fixedIds;
	}
	
	public void setFixedIds(Map<String, String> fixedIds)
	{
		this.fixedIds = fixedIds;
	}
	
	
	@Override
	public String getReportsDir()
	{
		return reportsDir;
	}
	
	@Override
	public String getCompletedReportsDir()
	{
		return completedReportsDir;
	}
	
	
	public void setExecutionMonitor(Object executionMonitor)
	{
		this.executionMonitor = executionMonitor;
	}
	
	
	protected void backupDir(File source, File target) throws IOException
	{
		if (source.isDirectory())
		{
			if (!target.exists())
				target.mkdirs();

			File[] entries = source.listFiles();
			if (entries != null)
			{
				for (File entry : entries) 
					backupDir(entry, new File(target, entry.getName()));
			}
		}
		else
			com.exactprosystems.clearth.utils.FileOperationUtils.copyFile(source.getCanonicalPath(), target.getCanonicalPath());
	}
	
	protected void storeStepNames()
	{
		File stepNamesFile = new File(ClearThCore.appRootRelative(reportsDir), "steps");
		PrintWriter stepNamesWriter = null;
		try
		{
			stepNamesWriter = new PrintWriter(stepNamesFile);
			for (Step step : steps)
				stepNamesWriter.println(step.getName());
		}
		catch (IOException e)
		{
			getLogger().warn("Could not store step names list to '{}'. Automatic report restoration won't work for this run",
					stepNamesFile.getAbsolutePath(), e);
		}
		finally
		{
			Utils.closeResource(stepNamesWriter);
		}
	}
	
	protected Step searchPreviousStep(Step step) {
		ListIterator<Step> stepListIterator = steps.listIterator(steps.size());
		while (stepListIterator.hasPrevious() && !(step.equals(stepListIterator.previous())));
		Step prevStep = null;
		while (stepListIterator.hasPrevious() && !(prevStep = stepListIterator.previous()).isExecute());
		return prevStep == null || !prevStep.isExecute() ? null : prevStep;
	}

	protected Step searchFirstExecutableStep() {
		for (Step step: steps) {
			if (step.isExecute() && step.getStarted() != null) {
				return step;
			}
		}
		return null;
	}
	
	protected boolean isStepExecutable(Step step)
	{
		if ((step.isExecute()) && (step.getActions() != null) && (step.getActions().size() > 0))
		{
			for (Action action : step.getActions())
			{
				if (action.isExecutable())
					return true;
			}
		}
		return false;
	}
	
	protected void waitForStepStart(Step step)
	{
		if (step.isExecutable() && (step.getStartAt() != null) && (!step.getStartAt().isEmpty()))
		{
			Calendar now = Calendar.getInstance(), 
					startTime = Calendar.getInstance();

			try
			{
				idle = true;
				boolean relative = step.getStartAt().startsWith("+");
				String start = relative ? step.getStartAt().substring(1) : step.getStartAt();
				if (start.length() == 5)
					start += ":00";
				if (relative)
				{
					Step prevStep;
					switch (step.getStartAtType())
					{
						case END_STEP:
							prevStep = searchPreviousStep(step);
							if (prevStep != null)
								now.setTime(prevStep.getFinished());
							break;
						case START_STEP:
							prevStep = searchPreviousStep(step);
							if (prevStep != null)
								now.setTime(prevStep.getStarted());
							break;
						case START_SCHEDULER:
							now.setTime(globalContext.getStarted());
							break;
						case START_EXECUTION:
							prevStep = searchFirstExecutableStep();
							if (prevStep != null)
								now.setTime(prevStep.getStarted());
							else
								now.setTime(globalContext.getStarted());
							break;
					}

					String[] parts = start.split(":");
					int[] intParts = new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]) };
					startTime = (Calendar) now.clone();
					startTime.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY) + intParts[0]);
					startTime.set(Calendar.MINUTE, now.get(Calendar.MINUTE) + intParts[1]);
					startTime.set(Calendar.SECOND, now.get(Calendar.SECOND) + intParts[2]);
				}
				else
				{
					startTime.setTime(hmsFormatter.parse(start));
					startTime.set(Calendar.YEAR, now.get(Calendar.YEAR));
					startTime.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR));
				}

				boolean disableWait = false;
				if (startTime.getTimeInMillis() <= now.getTimeInMillis())
				{
					if (step.isWaitNextDay())
						startTime.set(Calendar.DAY_OF_YEAR, startTime.get(Calendar.DAY_OF_YEAR) + 1);
					else
					{
						disableWait = true;
						getLogger().debug("Executing next step without waiting");
					}
				}
				startTimeStep = startTime.getTimeInMillis();
				
				if (!disableWait)
				{
					getLogger().debug("Waiting for next step until {}", startTime.getTime());
					if (sleepTimer == null)
					{
						sleepTimer = new Timer();
						getLogger().debug("Timer created");
					}
					TimerTask task = new UnsleepTask();
					sleepTimer.schedule(task, startTime.getTime());

					synchronized (suspension)
					{
						suspension.setTimeout(true);
						suspension.wait();
					}

					task.cancel();
					getLogger().trace("Finished waiting for next step");
				}
			}
			catch (Exception e)
			{
				if (e instanceof InterruptedException)
				{
					getLogger().error("Wait for step start interrupted", e);
					interrupted.set(true);
				}
				else
				{
					String msg = format("Step '%s': error while parsing 'Start at' parameter (%s), it must be in format '%s' with optional '+' in the beginning",
							step.getName(), step.getStartAt(), format);
					status.add(msg);
					getLogger().warn(msg, e);
				}
			}
			finally
			{
				idle = false;
			}
		}
	}

	//FIXME: this method is not thread-safe
	protected void makeReports(String pathToStoreReports, String pathToActionsReports) throws IOException, ReportException
	{
		Files.createDirectories(Path.of(pathToStoreReports));

		ReportsWriter reportsWriter = initReportsWriter(pathToStoreReports, pathToActionsReports);
		for (Matrix matrix : matrices)
		{
			List<String> stepsMatrix = getMatrixSteps(matrix.getShortFileName());
			reportsWriter.buildAndWriteReports(matrix, stepsMatrix, globalContext.getStartedByUser(), executionHandler.getName());
		}
	}
	
	protected ReportsInfo createReportsInfo(String pathToStoreReports)
	{
		ReportsInfo result = new ReportsInfo();
		result.setPath(pathToStoreReports);
		result.setReportsConfig(getReportsConfig());
		List<XmlMatrixInfo> mi = result.getMatrices();
		for (Matrix matrix : matrices)
		{
			XmlMatrixInfo matrixInfo = new XmlMatrixInfo();
			matrixInfo.setName(matrix.getName());
			matrixInfo.setFileName(matrix.getShortFileName());
			matrixInfo.setActionsDone(matrix.getActionsDone());
			matrixInfo.setSuccessful(matrix.isSuccessful());
			
			mi.add(matrixInfo);
		}
		return result;
	}
	
	/**
	 * @return map which contains pairs: matrix name - list of matrix step names 
	 */
	public static Map<String, List<String>> getStepsByMatricesMap(File actionsReports)
	{
		//actionsReports contains directories per matrix with set of steps as sub-files (action reports) within
		File[] maReports = actionsReports.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				return pathname.isDirectory();
			}
		});
		
		Map<String, List<String>> matrSteps = new HashMap<String, List<String>>();
		if (maReports != null) {
			for (File ma : maReports) {
				String[] steps = ma.list();
				if (steps != null)
					matrSteps.put(ma.getName(), Arrays.asList(steps));
			}
		}
		return matrSteps;
	}
	
	@Override
	public void makeCurrentReports(String pathToStoreReports)
	{
		try
		{
			pathToStoreReports = ClearThCore.appRootRelative(pathToStoreReports);
			makeReports(pathToStoreReports, ClearThCore.appRootRelative(actionsReportsDir));
			lastReportsInfo = createReportsInfo(pathToStoreReports);
		}
		catch (Exception e)
		{
			getLogger().warn("Error while making reports", e);
		}
	}
	
	@Override
	public void copyActionReports(File toDir)
	{
		try
		{
			FileUtils.deleteDirectory(toDir);
			FileUtils.copyDirectory(new File (ClearThCore.appRootRelative(actionsReportsDir)),
					toDir);
		}
		catch (Exception e)
		{
			getLogger().warn("Error while copying actions directory", e);
		}
	}
	
	public void setStoredActionReports (File srcDir)
	{
		this.storedActionsReportsDir = srcDir;
	}
	
	protected void restoreActionsReports() throws IOException
	{
		if (storedActionsReportsDir != null && storedActionsReportsDir.exists())
		{
			FileUtils.copyDirectory(storedActionsReportsDir,
					new File(ClearThCore.appRootRelative(actionsReportsDir)));
		}
		else
		{
			getLogger().warn("Stored actions reports were not found. Directory {} does not exist", storedActionsReportsDir);
		}
	}
	
	@Override
	public ReportsInfo getLastReportsInfo()
	{
		return lastReportsInfo;
	}
	
	@Override
	public void clearLastReportsInfo()
	{
		this.lastReportsInfo = null;
	}

	public String getActionsReportsDir()
	{
		return actionsReportsDir;
	}

	public long getStartTimeStep() {
		return startTimeStep;
	}

	public void skipWaitingStep()  {
		this.startTimeStep = 0;
		suspension.setTimeout(false);
	}

	public boolean isSuspensionTimeout() {
		return suspension.isTimeout();
	}

	public abstract List<String> getMatrixSteps(String matrixName);

	protected class UnsleepTask extends TimerTask {
		@Override
		public void run() {
			synchronized (suspension) {
				suspension.setTimeout(false);
				if (!suspension.isSuspended()) {
					suspension.notify();
				}
			}
		}
	}

	private void saveExecutedMatrices(Scheduler scheduler, List<Matrix> matrices) throws IOException, AutomationException
	{
		Path executedMatricesPath = scheduler.getExecutedMatricesPath();
		Files.createDirectories(executedMatricesPath);
		List<MatrixData> executedMatrices = new ArrayList<>(matrices.size());
		MatrixDataFactory matrixDataFactory = scheduler.getMatrixDataFactory();

		try
		{
			for (Matrix matrix : matrices)
			{
				MatrixData matrixData = matrix.getMatrixData();
				File matrixFile = matrixData.getFile();

				Path executedMatrix = Files.copy(matrixFile.toPath(),
						executedMatricesPath.resolve(matrixFile.getName()));

				MatrixData createdMatrix = matrixDataFactory.createMatrixData(
						matrixData.getName(),
						executedMatrix.toFile(),
						matrixData.getUploadDate(),
						matrixData.isExecute(),
						matrixData.isTrim(),
						matrixData.getLink(),
						matrixData.getType(),
						matrixData.isAutoReload());

				executedMatrices.add(createdMatrix);
			}
		}
		catch (IOException e)
		{
			String errorMessage = "Could not save executed matrices";
			getLogger().error(errorMessage, e);
			removeExecutedMatrices(executedMatrices);
			throw new AutomationException(errorMessage, e);
		}

		scheduler.setExecutedMatrices(executedMatrices);
	}

	private void removeExecutedMatrices(List<MatrixData> matrices)
	{
		for (MatrixData matrix : matrices)
		{
			try
			{
				Files.delete(matrix.getFile().toPath());
			}
			catch (IOException e)
			{
				getLogger().error("Could not remove copy of executed matrix: '{}'", matrix.getFile().getName(), e);
			}
		}
	}

	private void removeUnusedExecutedMatrices(List<Matrix> matrices)
	{
		File[] files = scheduler.getExecutedMatricesPath().toFile().listFiles();

		if (files == null || files.length == 0)
			return;

		Set<String> matricesFilesNames = matrices.stream()
				.map(Matrix::getFileName)
				.collect(Collectors.toSet());

		for (File file : files)
		{
			if (!matricesFilesNames.contains(file.getName()))
			{
				try
				{
					Files.delete(file.toPath());
				}
				catch (IOException e)
				{
					getLogger().error("Could not remove previously saved matrix: '{}'", file.getName(), e);
				}
			}
		}
	}
}
