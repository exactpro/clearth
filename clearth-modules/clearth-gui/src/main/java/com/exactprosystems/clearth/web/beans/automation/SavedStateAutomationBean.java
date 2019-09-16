/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateInfo;
import com.exactprosystems.clearth.automation.persistence.StepState;
import com.exactprosystems.clearth.utils.ExceptionUtils;
import com.exactprosystems.clearth.web.beans.ClearThBean;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.UserInfoUtils;

import java.io.IOException;

@SuppressWarnings({"WeakerAccess", "unused"})
public class SavedStateAutomationBean extends ClearThBean {

	//State management variables
	protected boolean stateExecuteAll = true;
	protected boolean stateAskForContinueAll = true;
	protected boolean stateAskIfFailedAll = true;
	
	protected StepState selectedStepState;
	protected StepState originalSelectedStepState;

	protected AutomationBean automationBean;

	protected Scheduler selectedScheduler() {
		return this.automationBean.selectedScheduler;
	}

	public void setAutomationBean(AutomationBean automationBean) {
		this.automationBean = automationBean;
	}
	

	public void saveStepState()
	{
		try
		{
			selectedScheduler().modifyStepState(originalSelectedStepState, selectedStepState);
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", ExceptionUtils.getDetailedMessage(e));
		}
	}

	public StepState getSelectedStepState()
	{
		return selectedStepState;
	}

	public void setSelectedStepState(StepState selectedStepState)
	{
		this.originalSelectedStepState = selectedStepState;
		this.selectedStepState = selectedScheduler().getStepFactory().createStepState(selectedStepState);
	}

	public ExecutorStateInfo getStateInfo()
	{
		return selectedScheduler().getStateInfo();
	}

	public boolean isStateExecuteAll()
	{
		return stateExecuteAll;
	}

	public void setStateExecuteAll(boolean stateExecuteAll)
	{
		this.stateExecuteAll = stateExecuteAll;
	}


	public boolean isStateAskForContinueAll()
	{
		return stateAskForContinueAll;
	}

	public void setStateAskForContinueAll(boolean stateAskForContinueAll)
	{
		this.stateAskForContinueAll = stateAskForContinueAll;
	}


	public boolean isStateAskIfFailedAll()
	{
		return stateAskIfFailedAll;
	}

	public void setStateAskIfFailedAll(boolean stateAskIfFailedAll)
	{
		this.stateAskIfFailedAll = stateAskIfFailedAll;
	}


	public void toggleStateExecute()
	{
		selectedStepState.setExecute(!originalSelectedStepState.isExecute());
		saveStepState();
	}

	public void toggleAllStateExecute()
	{
		stateExecuteAll = !stateExecuteAll;
		try
		{
			selectedScheduler().setStateExecute(stateExecuteAll);
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", ExceptionUtils.getDetailedMessage(e));
		}
	}


	public void toggleStateAskForContinue()
	{
		selectedStepState.setAskForContinue(!originalSelectedStepState.isAskForContinue());
		saveStepState();
	}

	public void toggleAllStateAskForContinue()
	{
		stateAskForContinueAll = !stateAskForContinueAll;
		try
		{
			selectedScheduler().setStateAskForContinue(stateAskForContinueAll);
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", ExceptionUtils.getDetailedMessage(e));
		}
	}


	public void toggleStateAskIfFailed()
	{
		selectedStepState.setAskIfFailed(!originalSelectedStepState.isAskIfFailed());
		saveStepState();
	}

	public void toggleAllStateAskIfFailed()
	{
		stateAskIfFailedAll = !stateAskIfFailedAll;
		try
		{
			selectedScheduler().setStateAskIfFailed(stateAskIfFailedAll);
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", ExceptionUtils.getDetailedMessage(e));
		}
	}

	/* Scheduler state routines */

	public void saveState()
	{
		try
		{
			if (!selectedScheduler().saveState())
				MessageUtils.addWarningMessage("Could not save state", "Only status of suspended scheduler in single run mode can be saved");
			else
				MessageUtils.addInfoMessage("State saved", "You can restore state later when scheduler is stopped");
		}
		catch (Exception e)
		{
			getLogger().error("Error while saving state of '" + selectedScheduler().getName() + "' scheduler", e);
			MessageUtils.addErrorMessage("Could not save state", ExceptionUtils.getDetailedMessage(e));
		}
	}

	public void removeSavedState()
	{
		selectedScheduler().removeSavedState();
	}

	public boolean isStateSaved()
	{
		return selectedScheduler().getSchedulerData().isStateSaved();
	}

	public void restoreState()
	{
		try
		{
			if (!selectedScheduler().restoreState(UserInfoUtils.getUserName()))
				MessageUtils.addWarningMessage("Could not restore state", "State can only be restored if saved previously and if scheduler is not running");
			else
				MessageUtils.addInfoMessage("Scheduler state restored", "Execution continues");
		}
		catch (Exception e)
		{
			getLogger().error("Error while restoring state of '"+selectedScheduler().getName()+"' scheduler", e);
			MessageUtils.addErrorMessage("Could not restore state", ExceptionUtils.getDetailedMessage(e));
		}
	}
}
