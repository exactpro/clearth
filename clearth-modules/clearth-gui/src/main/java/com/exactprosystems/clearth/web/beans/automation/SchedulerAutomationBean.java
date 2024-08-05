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

package com.exactprosystems.clearth.web.beans.automation;

import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.exceptions.NothingToStartException;
import com.exactprosystems.clearth.automation.persistence.StateConfig;
import com.exactprosystems.clearth.automation.report.ReportsConfig;
import com.exactprosystems.clearth.automation.status.StatusLine;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SchedulerAutomationBean extends ClearThBean {
	
	protected AutomationBean automationBean;
	
	protected boolean showReportsDialog = false;
	
	public SchedulerAutomationBean() {

	}

	protected Scheduler selectedScheduler() {
		return this.automationBean.selectedScheduler;
	}

	public void setAutomationBean(AutomationBean automationBean) {
		this.automationBean = automationBean;
	}

	/* Execution management */

	public void start()
	{
		try
		{
			selectedScheduler().start(UserInfoUtils.getUserName());

			logSchedulerStarted();
			MessageUtils.addInfoMessage("Scheduler started", "All uploaded matrices will be executed in single run");
		}
		catch (NothingToStartException e)
		{
			MessageUtils.addWarningMessage("Scheduler will not start", e.getMessage());
		}
		catch (AutomationException e)
		{
			getLogger().error("Error while starting scheduler", e);
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		this.automationBean.getMatrixChecker().refreshMatricesChecking();
	}

	public void startSequential()
	{
		try
		{
			selectedScheduler().startSequential(UserInfoUtils.getUserName());
			getLogger().info("Started scheduler sequential run");
			MessageUtils.addInfoMessage("Scheduler started", "Uploaded matrices will be run sequentially");
		}
		catch (AutomationException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		this.automationBean.getMatrixChecker().refreshMatricesChecking();
	}

	private void logSchedulerStarted()
	{
		List<Matrix> matrices = selectedScheduler().getMatrices();
		List<String> matrixNames = new ArrayList<String>();
		for (Matrix matrix : matrices)
		{
			matrixNames.add(matrix.getName());
		}
		getLogger().info("Started scheduler with matrices: {}",matrixNames);
	}

	public void stop()
	{
		try
		{
			selectedScheduler().stop();

			getLogger().info("Stopped scheduler '" + selectedScheduler().getName() + "'");
			MessageUtils.addInfoMessage("Scheduler stopped", "");
		}
		catch (AutomationException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
	}

	public boolean isRunning()
	{
		return selectedScheduler().isRunning();
	}

	public boolean isSequentialRun()
	{
		return selectedScheduler().isSequentialRun();
	}

	public String getCurrentMatrix()
	{
		return selectedScheduler().getCurrentMatrix();
	}

	public boolean isInterrupted()
	{
		return selectedScheduler().isInterrupted();
	}

	public boolean isSuspended()
	{
		return selectedScheduler().isSuspended();
	}

	public boolean isReplayEnabled()
	{
		return selectedScheduler().isReplayEnabled();
	}

	public void continueExecution()
	{
		selectedScheduler().continueExecution();
		getLogger().info("Continued execution");
	}

	public void replayStep()
	{
		selectedScheduler().replayStep();
	}

	/* Failover */

	public boolean isFailover()
	{
		return selectedScheduler().isFailover();
	}
	
	public String getFailoverReasonString()
	{
		return selectedScheduler().getFailoverReasonString();
	}
	
	public String getFailoverConnectionName()
	{
		return selectedScheduler().getFailoverConnectionName();
	}
	
	public void tryAgainMain()
	{
		selectedScheduler().tryAgainMain();
	}

	public void tryAgainAlt()
	{
		selectedScheduler().tryAgainAlt();
	}
	
	public void restartActionOnFailover()
	{
		getLogger().info("Restarting current action due to decision in failover dialog");
		selectedScheduler().setFailoverRestartAction(true);
	}
	
	public void skipConnectionFailure(boolean skipAllTheSame)
	{
		getLogger().info("Skipping current action due to decision in failover dialog");
		if (skipAllTheSame)
			selectedScheduler().addConnectionToIgnoreFailuresByRun(getFailoverConnectionName());
		selectedScheduler().setFailoverSkipAction(true);
	}
	
	public List<StatusLine> getStatus()
	{
		return selectedScheduler().getStatus().getLines();
	}

	public boolean isStatusEmpty()
	{
		return getStatus().size()==0;
	}

	public boolean isRunning(Step step)
	{
		IExecutor exec = selectedScheduler().getExecutor();
		if (exec != null)
		{
			Step currentStep = exec.getCurrentStep();
			if (currentStep != null)
				return currentStep.equals(step);
		}
		return false;
	}


	public List<Step> getRealTimeSteps()
	{
		return selectedScheduler().getSteps();
	}
	
	public long getStartTime()
	{
		IExecutor exec = selectedScheduler().getExecutor();
		long startTime;
		if (exec != null && ((startTime = exec.getStartTimeStep()) != 0L))
			return Math.max(0, (startTime - System.currentTimeMillis()) / 1000);
		return 0L;
	}


	public void skipStepWaiting()
	{
		IExecutor exec = selectedScheduler().getExecutor();
		if (exec != null) {
			exec.skipWaitingStep();
			exec.continueExecution();
			getLogger().info("Skipped waiting for step");
		}
	}

	public boolean isWaitingForStep()
	{
		return selectedScheduler().isCurrentStepIdle();
	}

	public boolean isShowReportsDialog()
	{
		return showReportsDialog;
	}

	public void setShowReportsDialog(boolean showReportsDialog)
	{
		this.showReportsDialog = showReportsDialog;
	}

	public void pause()
	{
		try
		{
			selectedScheduler().pause();

			getLogger().info("Paused scheduler");
			MessageUtils.addInfoMessage("Scheduler paused", "");
		}
		catch (AutomationException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
	}

	public Date getSchedulerBaseTime()
	{
		return selectedScheduler().getBaseTime();
	}

	public Date getSchedulerBusinessDay()
	{
		return selectedScheduler().getBusinessDay();
	}
	
	public ReportsConfig getSchedulerReportsConfig()
	{
		return selectedScheduler().getCurrentReportsConfig();
	}
	
	public StateConfig getSchedulerStateConfig()
	{
		return selectedScheduler().getCurrentStateConfig();
	}
}
