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
import com.exactprosystems.clearth.automation.persistence.StepState;
import com.exactprosystems.clearth.utils.CommaBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
		Set<String> existingMatrices = scheduler.getMatrices().stream().map(Matrix::getName).collect(Collectors.toSet()),
				matrixStates = isAutoSave() ? new HashSet<>(scheduler.getStateInfo().getMatrices()) : null;
		checkUpdatedMatrixFiles(updatedMatrixData, existingMatrices, matrixStates);
		
		if (logger.isInfoEnabled())
			logger.info("Compiling updated matrices: {}", updatedMatrixData.stream().map(MatrixData::getName).collect(Collectors.toList()));
		List<Matrix> updatedMatrices = compileUpdatedMatrices(updatedMatrixData);
		logger.info("Updating matrices in scheduler");
		applyUpdatedMatrices(updatedMatrices);
	}
	
	public void updateSteps(List<StepData> updatedStepData) throws SchedulerUpdateException
	{
		Map<String, Step> existingSteps = scheduler.getSteps().stream().collect(Collectors.toMap(Step::getName, Function.identity()));
		Map<String, StepState> stepStates = isAutoSave() ? scheduler.getStateInfo().getSteps().stream().collect(Collectors.toMap(StepState::getName, Function.identity())) : null;
		checkUpdatedSteps(updatedStepData, existingSteps, stepStates);
		
		if (logger.isInfoEnabled())
			logger.info("Updating steps: {}", updatedStepData.stream().map(StepData::getName).collect(Collectors.toList()));
		applyUpdatedSteps(updatedStepData, existingSteps, stepStates);
	}
	
	
	private void checkUpdatedMatrixFiles(List<MatrixData> updatedMatrices, Set<String> existingMatrices, Set<String> matrixStates) throws SchedulerUpdateException
	{
		CommaBuilder errors = new CommaBuilder("; ");
		for (MatrixData m : updatedMatrices)
		{
			String name = m.getName();
			
			if (!existingMatrices.contains(name))
			{
				errors.append("Matrix '"+name+"' is not used in the run");
				continue;
			}
			
			if (matrixStates != null && !matrixStates.contains(name))
				errors.append("State of matrix '"+name+"' is not saved");
		}
		
		if (errors.length() > 0)
			throw new SchedulerUpdateException(errors.toString());
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
	
	private void applyUpdatedMatrices(List<Matrix> updatedMatrices) throws ActionUpdateException, SchedulerUpdateException
	{
		Step currentStep = scheduler.getCurrentStep();
		Action currentAction = currentStep.getCurrentAction();
		
		ActionsUpdater updater = new ActionsUpdater(scheduler.getMatrices(), scheduler.getSteps());
		updater.updateFrom(currentAction, updatedMatrices);
		
		if (!currentStep.isEnded())  //If current step is not finished yet, need to rewind its progress back to current action
			currentStep.rewindToAction(currentAction);
		
		if (!isAutoSave())
			return;
		
		try
		{
			scheduler.updateMatrixStates(updatedMatrices);
		}
		catch (Exception e)
		{
			throw new SchedulerUpdateException("Error while updating matrix states", e);
		}
	}
	
	
	private void checkUpdatedSteps(List<StepData> updatedSteps, Map<String, Step> existingSteps, Map<String, StepState> stepStates) throws SchedulerUpdateException
	{
		for (StepData step : updatedSteps)
		{
			String stepName = step.getName();
			Step existingStep = existingSteps.get(stepName);
			if (existingStep == null)
				throw new SchedulerUpdateException("Global step '"+stepName+"' is not used in the run");
			if (existingStep.isEnded())
				throw new SchedulerUpdateException("Global step '"+stepName+"' is ended and cannot be updated");
			if (!existingStep.isExecute() && step.isExecute())
				throw new SchedulerUpdateException("Global step '"+stepName+"' is not executable and cannot be changed to executable");
			
			if (stepStates == null)
				continue;
			
			StepState state = stepStates.get(stepName);
			if (state == null)
				throw new SchedulerUpdateException("State of global step '"+stepName+"' is not saved");
		}
	}
	
	private void applyUpdatedSteps(List<StepData> updatedSteps, Map<String, Step> existingSteps, Map<String, StepState> stepStates) throws SchedulerUpdateException
	{
		for (StepData step : updatedSteps)
		{
			String stepName = step.getName();
			logger.debug("Updating step '{}': Ask for continue={}, Ask if failed={}",
					stepName, step.isAskForContinue(), step.isAskIfFailed());
			
			Step existingStep = existingSteps.get(stepName);
			existingStep.setAskForContinue(step.isAskForContinue());
			existingStep.setAskIfFailed(step.isAskIfFailed());
			if (existingStep.getStarted() == null)  //Can update the values below only for steps that are not started yet
			{
				logger.debug("Updating startup fields: Execute={}, Start at={}, Kind={}",
						step.isExecute(), step.getStartAt(), step.getKind());
				existingStep.setExecute(step.isExecute());
				existingStep.setStartAt(step.getStartAt());
				existingStep.setKind(step.getKind());
			}
			
			if (stepStates == null)
				continue;
			
			try
			{
				StepState originalState = stepStates.get(stepName),
						state = scheduler.createStepState(originalState);
				state.setAskForContinue(step.isAskForContinue());
				state.setAskIfFailed(step.isAskIfFailed());
				if (originalState.getStarted() == null)
				{
					state.setExecute(step.isExecute());
					state.setStartAt(step.getStartAt());
					state.setKind(step.getKind());
				}
				scheduler.updateStepState(originalState, state);
			}
			catch (Exception e)
			{
				throw new SchedulerUpdateException("Error while updating state of global step '"+stepName+"'", e);
			}
		}
		
		if (stepStates != null)
		{
			try
			{
				scheduler.saveStepsState();
			}
			catch (Exception e)
			{
				throw new SchedulerUpdateException("Error while saving state of global steps", e);
			}
		}
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
	
	
	private boolean isAutoSave()
	{
		return scheduler.getCurrentStateConfig().isAutoSave();
	}
}
