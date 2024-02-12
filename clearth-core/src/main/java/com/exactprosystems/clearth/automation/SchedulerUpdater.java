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

import com.exactprosystems.clearth.automation.exceptions.ActionUpdateException;
import com.exactprosystems.clearth.automation.exceptions.SchedulerUpdateException;
import com.exactprosystems.clearth.utils.CommaBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerUpdater
{
	private static final Logger logger = LoggerFactory.getLogger(SchedulerUpdater.class);
	
	private final Scheduler scheduler;
	
	public SchedulerUpdater(Scheduler scheduler)
	{
		this.scheduler = scheduler;
	}
	
	public void updateMatrices(List<MatrixData> updatedMatrixData) throws SchedulerUpdateException, ActionUpdateException
	{
		checkUpdatedMatrixFiles(updatedMatrixData);
		
		if (logger.isInfoEnabled())
			logger.info("Compiling updated matrices: {}", updatedMatrixData.stream().map(MatrixData::getName).collect(Collectors.toList()));
		List<Matrix> updatedMatrices = compileUpdatedMatrices(updatedMatrixData);
		logger.info("Updating matrices in scheduler");
		applyUpdate(updatedMatrices);
	}
	
	
	private void checkUpdatedMatrixFiles(List<MatrixData> updatedMatrices) throws SchedulerUpdateException
	{
		CommaBuilder errors = null;
		Set<String> existingMatrices = scheduler.getMatrices().stream().map(Matrix::getName).collect(Collectors.toSet());
		for (MatrixData m : updatedMatrices)
		{
			if (!existingMatrices.contains(m.getName()))
			{
				if (errors == null)
					errors = new CommaBuilder();
				errors.append(m.getName());
			}
		}
		
		if (errors != null)
			throw new SchedulerUpdateException("The following matrices are not used in the run: "+errors.toString());
	}
	
	private List<Matrix> compileUpdatedMatrices(List<MatrixData> updatedMatrixData) throws SchedulerUpdateException
	{
		List<Step> updatedSteps = createStepsToUpdate();
		List<Matrix> updatedMatrices = new ArrayList<>(updatedMatrixData.size());
		Map<String, Preparable> preparableActions = new LinkedHashMap<>();
		try
		{
			scheduler.prepare(updatedSteps, updatedMatrices, updatedMatrixData, preparableActions);
		}
		catch (Exception e)
		{
			throw new SchedulerUpdateException("Error while compiling updated matrices", e);
		}
		return updatedMatrices;
	}
	
	private void applyUpdate(List<Matrix> updatedMatrices) throws ActionUpdateException
	{
		Step currentStep = scheduler.getCurrentStep();
		Action currentAction = currentStep.getCurrentAction();
		
		ActionsUpdater updater = new ActionsUpdater(scheduler.getMatrices(), scheduler.getSteps());
		updater.updateFrom(currentAction, updatedMatrices);
		
		if (!currentStep.isEnded())  //If current step is not finished yet, need to rewind its progress back to current action
			currentStep.rewindToAction(currentAction);
	}
	
	
	private List<Step> createStepsToUpdate()
	{
		StepFactory stepFactory = scheduler.getStepFactory();
		List<Step> steps = scheduler.getSteps(),
				result = new ArrayList<>(steps.size());
		for (Step step : steps)
		{
			Step copy = stepFactory.createStep(step.getName(), step.getKind(),
					step.getStartAt(), step.getStartAtType(), step.isWaitNextDay(), step.getParameter(),
					step.isAskForContinue(), step.isAskIfFailed(), step.isExecute(), step.getComment());
			result.add(copy);
		}
		return result;
	}
}
