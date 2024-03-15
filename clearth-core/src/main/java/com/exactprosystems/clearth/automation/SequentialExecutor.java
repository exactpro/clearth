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

import org.slf4j.Logger;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.data.DataHandlersFactory;
import com.exactprosystems.clearth.data.TestExecutionHandler;

public abstract class SequentialExecutor extends Thread implements IExecutor
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
	protected SimpleExecutor currentExecutor = null;
	protected Consumer<SequentialExecutor> onFinish;
	
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
	protected abstract void initExecutor(SimpleExecutor executor);
	
	@Override
	public void run()
	{
		try
		{
			List<Matrix> singleMatrixList = new ArrayList<Matrix>();
			List<MatrixData>  scripts = scheduler.getMatricesData();
			boolean first = true;
			DataHandlersFactory handlersFactory = ClearThCore.getInstance().getDataHandlersFactory();
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
				
				TestExecutionHandler executionHandler = handlersFactory.createTestExecutionHandler(scheduler.getName());
				synchronized (ceMonitor)
				{
					currentExecutor = executorFactory.createExecutor(scheduler, singleMatrixList, startedByUser, preparableActions, executionHandler);
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
			
			if (onFinish != null)
				onFinish.accept(this);
			
			clearSteps();
		}
	}
	
	
	public void setOnFinish(Consumer<SequentialExecutor> consumer)
	{
		onFinish = consumer;
	}
	
	
	public Scheduler getScheduler()
	{
		return scheduler;
	}


	public String getStartedByUser()
	{
		return startedByUser;
	}

	@Override
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

	@Override
	public Map<String, Boolean> getHolidays()
	{
		return holidays;
	}

	@Override
	public boolean isTerminated()
	{
		return terminated;
	}

	@Override
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
	
	@Override
	public boolean isExecutionInterrupted()
	{
		return interrupted;
	}
	
	@Override
	public void pauseExecution() {
		
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.pauseExecution();
		}
		
	}
	
	@Override
	public void continueExecution()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.continueExecution();
		}
	}
	
	@Override
	public void replayStep()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.replayStep();
		}
	}
	
	@Override
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
	
	@Override
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
	
	@Override
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
	
	@Override
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
	
	
	@Override
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
	
	@Override
	public void tryAgainMain()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.tryAgainMain();
		}
	}
	
	@Override
	public void tryAgainAlt()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				currentExecutor.tryAgainAlt();
		}
	}
	
	@Override
	public int getFailoverActionType()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor == null)
				return ActionType.NONE;
			return currentExecutor.getFailoverActionType();
		}
	}
	
	@Override
	public int getFailoverReason()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor==null)
				return FailoverReason.NONE;
			return currentExecutor.getFailoverReason();
		}
	}
	
	@Override
	public String getFailoverReasonString()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor == null)
				return null;
			return currentExecutor.getFailoverReasonString();
		}
	}
	
	@Override
	public String getFailoverConnectionName()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor == null)
				return null;
			return currentExecutor.getFailoverConnectionName();
		}
	}
	
	@Override
	public void setFailoverRestartAction(boolean needRestart)
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor==null)
				return;
			currentExecutor.setFailoverRestartAction(needRestart);
		}
	}
	
	@Override
	public void setFailoverSkipAction(boolean needSkipAction)
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor == null)
				return;
			currentExecutor.setFailoverSkipAction(needSkipAction);
		}
	}
	
	@Override
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
	
	@Override
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
	
	@Override
	public ReportsInfo getLastReportsInfo()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				return currentExecutor.getLastReportsInfo();
			else
				return null;
		}
	}
	
	@Override
	public void clearLastReportsInfo()
	{
		synchronized (ceMonitor)
		{
			if (currentExecutor!=null)
				this.currentExecutor.clearLastReportsInfo();
		}
	}
	
	@Override
	public void makeCurrentReports(String pathToStoreReports)
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

	@Override
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
	}
}
