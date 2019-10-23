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

import com.exactprosystems.clearth.automation.CoreStepKind;
import com.exactprosystems.clearth.automation.Scheduler;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.automation.StepImpl;
import com.exactprosystems.clearth.automation.steps.ParamDescription;
import com.exactprosystems.clearth.utils.CommaBuilder;
import com.exactprosystems.clearth.utils.SettingsException;
import com.exactprosystems.clearth.web.misc.MessageUtils;
import com.exactprosystems.clearth.web.misc.StepPropsToEdit;
import com.exactprosystems.clearth.web.misc.StepUploadHandler;
import com.exactprosystems.clearth.web.misc.WebUtils;

import org.primefaces.context.PrimeFacesContext;
import org.primefaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AutomationStepsManagement {

	private static final Logger logger = LoggerFactory.getLogger(AutomationStepsManagement.class);

	protected final List<Step> selectedSteps;
	protected List<Step> originalSelectedSteps;
	protected final StepPropsToEdit stepProps;

	protected boolean appendSteps = false;
	protected boolean askForContinueAll = true;
	protected boolean executeAll = true;
	protected boolean askIfFailedAll = true;
	protected boolean stepsReordering = true;
	
	protected ConfigurationAutomationBean configurationBean;

	public AutomationStepsManagement(ConfigurationAutomationBean configurationBean) {
		this.selectedSteps = new ArrayList<>();
		this.configurationBean = configurationBean;
		this.stepProps = createStepPropsToEdit();
	}

	/* Steps management */
	

	public List<Step> getSteps()
	{
		return selectedScheduler().getSchedulerData().getSteps();
	}

	public Scheduler selectedScheduler() {
		return this.configurationBean.selectedScheduler();
	}

	public List<Step> getSchedulerSteps()
	{
		return selectedScheduler().getSteps();
	}

	public Step getCurrentSchedulerStep()
	{
		return selectedScheduler().getCurrentStep();
	}

	public boolean isCurrentStepIdle()
	{
		return selectedScheduler().isCurrentStepIdle();
	}


	public List<Step> getSelectedSteps()
	{
		return selectedSteps;
	}

	public void setSelectedSteps(List<Step> selectedSteps)
	{
		this.originalSelectedSteps = selectedSteps;
		this.selectedSteps.clear();
		for (Step s : originalSelectedSteps)
			this.selectedSteps.add(selectedScheduler().getStepFactory().createStep(s.getName(), s.getKind(),
					s.getStartAt(), s.getStartAtType(), s.isWaitNextDay(), s.getParameter(), s.isAskForContinue(), s.isAskIfFailed(), s.isExecute(), s.getComment()));
	}


	public Step getOneSelectedStep()
	{
		if (selectedSteps.isEmpty())
			return null;
		return selectedSteps.get(0);
	}

	public void setOneSelectedStep(Step selectedStep)
	{
		setSelectedSteps(Collections.singletonList(selectedStep));
	}

	public boolean isOneStepSelected()
	{
		return selectedSteps.size() == 1;
	}


	protected StepPropsToEdit createStepPropsToEdit()
	{
		return new StepPropsToEdit();
	}

	public StepPropsToEdit getStepProps()
	{
		return stepProps;
	}


	public void saveStepsPositions() throws IOException
	{
		synchronized (selectedScheduler())
		{
			selectedScheduler().saveStepsAndInit("Error while saving steps after moving one of them");
		}
	}

	protected void resetStepsSelection()
	{
		originalSelectedSteps = null;
		selectedSteps.clear();
	}

	public void newStep()
	{
		resetStepsSelection();

		Step s = selectedScheduler().getStepFactory().createStep();
		s.setExecute(true);
		s.setKind(CoreStepKind.Default.getLabel());

		selectedSteps.add(s);
	}

	public void removeSteps()
	{
		try
		{
			selectedScheduler().removeSteps(originalSelectedSteps);
			if (logger.isInfoEnabled())
			{
				if (originalSelectedSteps.size() == 1)
					logger.info("removed step '"+originalSelectedSteps.get(0).getName()+"' from scheduler '"+selectedScheduler().getName()+"'");
				else
				{
					CommaBuilder cb = new CommaBuilder();
					for (Step step : originalSelectedSteps)
						cb.append("'"+step.getName()+"'");
					logger.info("removed steps "+cb.toString()+" from scheduler '"+selectedScheduler().getName()+"'");
				}
			}
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		finally
		{
			resetStepsSelection();
		}
	}

	public void clearSteps()
	{
		try
		{
			selectedScheduler().clearSteps();
			logger.info("cleared steps from '"+selectedScheduler().getName()+"'");
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		finally
		{
			resetStepsSelection();
		}
	}

	protected void editStepProps(Step stepToEdit, Step changes, Step original, StepPropsToEdit propsToEdit)
	{
		stepToEdit.setKind(propsToEdit.isKind() ? changes.getKind() : original.getKind());
		stepToEdit.setStartAt(propsToEdit.isStartAt() ? changes.getStartAt() : original.getStartAt());
		stepToEdit.setWaitNextDay(propsToEdit.isWaitNextDay() ? changes.isWaitNextDay() : original.isWaitNextDay());
		stepToEdit.setParameter(propsToEdit.isParameter() ? changes.getParameter() : original.getParameter());
		stepToEdit.setAskForContinue(propsToEdit.isAskForContinue() ? changes.isAskForContinue() : original.isAskForContinue());
		stepToEdit.setAskIfFailed(propsToEdit.isAskIfFailed() ? changes.isAskIfFailed() : original.isAskIfFailed());
		stepToEdit.setExecute(propsToEdit.isExecute() ? changes.isExecute() : original.isExecute());
		stepToEdit.setComment(propsToEdit.isComment() ? changes.getComment() : original.getComment());
	}

	public void saveSteps()
	{
		if (selectedSteps.size() > 1)  //If multiple steps are edited, JSF changes only the first one. Need to apply changes to all other selected steps. Step name shouldn't be changed here!
		{
			//We get here only if multiple steps are edited, thus originalSelectedSteps can't be null
			Step firstStep = getOneSelectedStep(),
					originalFirstStep = originalSelectedSteps.get(0);
			editStepProps(firstStep, firstStep, originalFirstStep, stepProps);  //Restoring properties that shouldn't be edited, because JSF has changed all the properties of the first step
			for (int i = 1; i < selectedSteps.size(); i++)
			{
				Step s = selectedSteps.get(i);
				editStepProps(s, firstStep, s, stepProps);
			}
		}

		boolean canClose = true;
		try
		{
			if (originalSelectedSteps == null)
			{
				Step step = getOneSelectedStep();
				selectedScheduler().addStep(step);
				logger.info("created step '"+step.getName()+"' in scheduler '"+selectedScheduler().getName()+"'");
			}
			else
			{
				selectedScheduler().modifySteps(originalSelectedSteps, selectedSteps);
				if (logger.isInfoEnabled())
				{
					if (selectedSteps.size() == 1)
						logger.info("modified step '"+selectedSteps.get(0).getName()+"' in scheduler '"+selectedScheduler().getName()+"'");
					else
					{
						CommaBuilder cb = new CommaBuilder();
						for (Step step : selectedSteps)
							cb.append("'"+step.getName()+"'");
						logger.info("modified steps "+cb.toString()+" in scheduler '"+selectedScheduler().getName()+"'");
					}
				}
			}
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
		catch (SettingsException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
			canClose = false;
		}
		WebUtils.addCanCloseCallback(canClose);
	}


	public boolean isAppendSteps()
	{
		return appendSteps;
	}

	public void setAppendSteps(boolean appendSteps)
	{
		this.appendSteps = appendSteps;
	}

	public void uploadSteps(FileUploadEvent event)
	{
		StepUploadHandler.uploadSteps(event, 
				WebUtils.getMimeType(event.getFile().getFileName()),
				selectedScheduler(), appendSteps);
	}

	public void toggleAskForContinue()
	{
		try
		{
			for (int i = 0; i < selectedSteps.size(); i++)
			{
				Step os = originalSelectedSteps.get(i),
						s = selectedSteps.get(i);
				s.setAskForContinue(!os.isAskForContinue());
			}
			selectedScheduler().modifySteps(originalSelectedSteps, selectedSteps);
		}
		catch (IOException | SettingsException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
	}

	public void toggleAskIfFailed()
	{
		try
		{
			for (int i = 0; i < selectedSteps.size(); i++)
			{
				Step os = originalSelectedSteps.get(i),
						s = selectedSteps.get(i);
				s.setAskIfFailed(!os.isAskIfFailed());
			}
			selectedScheduler().modifySteps(originalSelectedSteps, selectedSteps);
		}
		catch (IOException | SettingsException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
	}

	public void toggleAllAskForContinue()
	{
		askForContinueAll = !askForContinueAll;
		try
		{
			selectedScheduler().setAskForContinue(askForContinueAll);
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
	}

	public void toggleAllAskIfFailed()
	{
		askIfFailedAll = !askIfFailedAll;
		try
		{
			selectedScheduler().setAskIfFailed(askIfFailedAll);
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
	}

	public void toggleExecute()
	{
		try
		{
			int i = -1;
			for (Step s : selectedSteps)
			{
				i++;
				Step os = originalSelectedSteps.get(i);
				s.setExecute(!os.isExecute());
			}
			selectedScheduler().modifySteps(originalSelectedSteps, selectedSteps);
		}
		catch (IOException | SettingsException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
	}

	public void toggleAllExecute()
	{
		executeAll = !executeAll;
		try
		{
			selectedScheduler().setExecute(executeAll);
		}
		catch (IOException e)
		{
			MessageUtils.addErrorMessage("Error", e.getMessage());
		}
	}

	public String getParamsDescription() {
		Step selectedStep = getOneSelectedStep();
		if (selectedStep == null)
			return null;

		Class<? extends StepImpl> stepImplClass = selectedScheduler().getStepImplClass(getOneSelectedStep().getKind());
		if (stepImplClass != null)
		{
			ParamDescription pd = stepImplClass.getAnnotation(ParamDescription.class);
			if (pd != null)
				return pd.description();
		}

		return "No parameters description defined";
	}

	public boolean isExecuteAll()
	{
		return executeAll;
	}

	public void setExecuteAll(boolean executeAll)
	{
		this.executeAll = executeAll;
	}

	public boolean isAskForContinueAll()
	{
		return askForContinueAll;
	}

	public void setAskForContinueAll(boolean askForContinueAll)
	{
		this.askForContinueAll = askForContinueAll;
	}

	public boolean isAskIfFailedAll()
	{
		return askIfFailedAll;
	}

	public void setAskIfFailedAll(boolean askIfFailedAll)
	{
		this.askIfFailedAll = askIfFailedAll;
	}

	public boolean isStepsReordering()
	{
		return stepsReordering;
	}

	public void setStepsReordering(boolean stepsReordering)
	{
		this.stepsReordering = stepsReordering;
	}
	
}
