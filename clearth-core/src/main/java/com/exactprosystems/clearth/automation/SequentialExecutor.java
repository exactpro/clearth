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

package com.exactprosystems.clearth.automation;

import org.slf4j.Logger;

import java.io.File;
import java.util.*;

import com.exactprosystems.clearth.automation.exceptions.AutomationException;

public abstract class SequentialExecutor extends Thread
{
	protected final ExecutorFactory executorFactory;
	protected final Scheduler scheduler;
	protected final List<Step> steps;
	protected final SchedulerStatus status;
	protected final Date businessDay;
	protected final Map<String, Boolean> holidays;
	protected final String startedByUser;
	protected final Map<String, Preparable> preparableActions;
	
	protected boolean terminated = false, interrupted = false;
	protected final Object ceMonitor = new Object(), executionMonitor = new Object();
	protected String currentMatrix = null;
	protected Executor currentExecutor = null;
	
	public SequentialExecutor(ExecutorFactory executorFactory, Scheduler scheduler, String startedByUser,
			Map<String, Preparable> preparableActions)
	{
		super(scheduler.getName());
		
		this.executorFactory = executorFactory;
		
		this.scheduler = scheduler;
		this.steps = scheduler.getSteps();
		this.status = scheduler.getStatus();
		this.businessDay = scheduler.getBusinessDay();
		this.holidays = scheduler.getHolidays();
		this.startedByUser = startedByUser;
		this.preparableActions = preparableActions;
	}
	
	protected abstract Logger getLogger();
	protected abstract void initExecutor(Executor executor);
	
	@Override
	public void run()
	{
		try
		{
			List<Matrix> singleMatrixList = new ArrayList<Matrix>();
			List<MatrixData>  scripts = scheduler.getMatricesData();
			boolean first = true;
			for (MatrixData script : scripts)
			{
				if (!script.isExecute())
					continue;
				
				if (first)
					first = false;
				else
				{
					currentMatrix = null;
					synchronized (ceMonitor)
					{
						currentExecutor = null;
					}
					try
					{
						sleep(30000);
					}
					catch (InterruptedException e)
					{
						interrupted = true;
						break;
					}
				}

				currentMatrix = script.getFile().getName();
				scheduler.prepare(steps, singleMatrixList, Collections.singletonList(script), preparableActions);

				synchronized (ceMonitor)
				{
					currentExecutor = executorFactory.createExecutor(scheduler, singleMatrixList, startedByUser, preparableActions);
				}
				currentExecutor.setExecutionMonitor(executionMonitor);
				initExecutor(currentExecutor);
				
				try
				{
					synchronized (executionMonitor)
					{
						currentExecutor.start();
						executionMonitor.wait();
					}
				}
				catch (InterruptedException e)
				{
					interrupted = true;
				}
				
				if (interrupted)
					break;
			}
		}
		catch (Exception e)
		{
			getLogger().error("Error while executing matrices in sequence", e);
		}
		finally
		{
			terminated = true;
			currentMatrix = null;
			synchronized (ceMonitor)
			{
				currentExecutor = null;
			}
			scheduler.seqExecutorFinished();
			clearSteps();
		}
	}
	

	public Scheduler getScheduler()
	{
		return scheduler;
	}


	public String getStartedByUser()
	{
		return startedByUser;
	}


	public List<Step> getSteps()
	{
		return steps;
	}

	public SchedulerStatus getStatus()
	{
		return status;
	}

	public Date getBusinessDay()
	{
		return businessDay;
	}

	public Map<String, Boolean> getHolidays()
	{
		return holidays;
	}


	public boolean isTerminated()
	{
		return terminated;
	}

	public void interruptExecution() throws AutomationException
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor != null)
				currentExecutor.interruptExecution();
		}
	}

	public void interruptWholeExecution() throws AutomationException
	{
		interrupted = true;
		try
		{
			interruptExecution();
		}
		finally
		{
			interrupt();
		}
	}

	public boolean isExecutionInterrupted()
	{
		return interrupted;
	}
	
	public void pauseExecution() {
		
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.pauseExecution();
		}
		
	}
	
	public void continueExecution()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.continueExecution();
		}
	}
	
	public void replayStep()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.replayStep();
		}
	}
	
	public Step getCurrentStep()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				return currentExecutor.getCurrentStep();
			else
				return null;
		}
	}
	
	public boolean isCurrentStepIdle()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor != null)
				return currentExecutor.isCurrentStepIdle();
			else
				return false;
		}
	}
	
	public boolean isSuspended()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				return currentExecutor.isSuspended();
			else
				return false;
		}
	}
	
	public boolean isReplayEnabled()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				return currentExecutor.isReplayEnabled();
			else
				return false;
		}
	}
	
	
	public boolean isFailover()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				return currentExecutor.isFailover();
			else
				return false;
		}
	}
	
	public void tryAgainMain()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.tryAgainMain();
		}
	}
	
	public void tryAgainAlt()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.tryAgainAlt();
		}
	}
	
	public int getFailoverReason()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor==null)
				return FailoverReason.NONE;
			return currentExecutor.getFailoverReason();
		}
	}
	
	public String getFailoverReasonString()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor == null)
				return null;
			return currentExecutor.getFailoverReasonString();
		}
	}
	
	public int getFailoverActionType()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor==null)
				return ActionType.NONE;
			return currentExecutor.getFailoverActionType();
		}
	}

	public void setFailoverRestartAction(boolean needRestart)
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor==null)
				return;
			currentExecutor.setFailoverRestartAction(needRestart);
		}
	}
	
	public String getReportsDir()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				return currentExecutor.getReportsDir();
			else
				return null;
		}
	}
	
	public String getCompletedReportsDir()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				return currentExecutor.getCompletedReportsDir();
			else
				return null;
		}
	}
	
	public ReportsInfo getLastReportInfo()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				return currentExecutor.getLastReportsInfo();
			else
				return null;
		}
	}
	
	public void clearLastReportInfo()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				this.currentExecutor.clearLastReportsInfo();
		}
	}
	
	public void makeCurrentReport(String pathToStoreReports)
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.makeCurrentReports(pathToStoreReports);
		}
	}
	
	public String getCurrentMatrix()
	{
		return currentMatrix;
	}

	public void copyActionReports(File pathToStoreReports) {
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.copyActionReports(pathToStoreReports);
		}
		
	}

	protected void clearSteps()
	{
		steps.forEach(Step::clearActions);
		if (scheduler.seqExec != null)
			steps.clear();
	}
}
