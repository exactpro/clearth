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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionsUpdater
{
	private static final Logger logger = LoggerFactory.getLogger(ActionsUpdater.class);
	
	private final Map<String, Matrix> matrixObjects;
	private final Map<String, Integer> matrixIndices;
	private final Map<String, Step> stepObjects;
	private final Map<String, Integer> stepIndices;
	private final List<Step> steps;
	
	public ActionsUpdater(List<Matrix> matrices, List<Step> steps)
	{
		matrixObjects = new HashMap<>();
		matrixIndices = new HashMap<>();
		int i = 0;
		for (Matrix m : matrices)
		{
			String name = m.getName();
			matrixObjects.put(name, m);
			matrixIndices.put(name, i);
			i++;
		}
		
		stepObjects = new HashMap<>();
		stepIndices = new HashMap<>();
		i = 0;
		for (Step s : steps)
		{
			String name = s.getName();
			stepObjects.put(name, s);
			stepIndices.put(name, i);
			i++;
		}
		this.steps = steps;
	}
	
	public void updateFrom(Action currentAction, Collection<Matrix> updatedMatrices) throws ActionUpdateException
	{
		checkCurrentAction(currentAction);
		checkUpdatedMatrices(updatedMatrices, currentAction);
		
		int currentActionMatrixIndex = matrixIndices.get(currentAction.getMatrix().getName()),
				currentActionStepIndex = stepIndices.get(currentAction.getStepName());
		for (Matrix m : updatedMatrices)
		{
			String matrixName = m.getName();
			logger.debug("Updating matrix '{}'", matrixName);
			
			int matrixIndex = matrixIndices.get(matrixName);
			if (matrixIndex < currentActionMatrixIndex)
			{
				//This matrix stands before currently executed matrix = actions from this matrix in currently executed step are already done
				//Updating only actions from next steps
				updateActions(m, steps.subList(currentActionStepIndex+1, steps.size()));
			}
			else if (matrixIndex > currentActionMatrixIndex)
			{
				//This matrix stands after currently executed matrix = actions from this matrix in currently executed step are not done yet
				//Updating actions starting from current step
				updateActions(m, steps.subList(currentActionStepIndex, steps.size()));
			}
			else
				updateActionsInCurrentMatrix(m, currentAction);
		}
	}
	
	
	private void checkCurrentAction(Action currentAction) throws ActionUpdateException
	{
		Matrix m = matrixObjects.get(currentAction.getMatrix().getName());
		if (m != currentAction.getMatrix())
			throw new ActionUpdateException("Given current action is not from running matrices");
		
		Step s = stepObjects.get(currentAction.getStepName());
		if (s != currentAction.getStep())
			throw new ActionUpdateException("Given current action is not from running steps");
	}
	
	private void checkUpdatedMatrices(Collection<Matrix> updatedMatrices, Action currentAction) throws ActionUpdateException
	{
		String currentActionMatrix = currentAction.getMatrix().getName();
		for (Matrix m : updatedMatrices)
		{
			if (!matrixObjects.containsKey(m.getName()))
				throw new ActionUpdateException("Matrix '"+m.getName()+"' is not in list of running matrices");
			
			if (m.getName().equals(currentActionMatrix))
				checkCurrentActionMatrix(m, currentAction);
		}
	}
	
	private void checkCurrentActionMatrix(Matrix matrix, Action currentAction) throws ActionUpdateException
	{
		for (Action a : matrix.getActions())
		{
			if (a.getIdInMatrix().equals(currentAction.getIdInMatrix()))
			{
				if (!a.getStepName().equals(currentAction.getStepName())
						|| !a.getName().equals(currentAction.getName()))
					throw new ActionUpdateException(String.format("Current action (ID=%s, matrix '%s') differs in updated matrix from action in running matrix",
							currentAction.getIdInMatrix(), matrix.getName()));
				return;
			}
		}
		throw new ActionUpdateException(String.format("Current action (ID=%s) is not found in updated matrix '%s'",
				currentAction.getIdInMatrix(), matrix.getName()));
	}
	
	
	private void updateActions(Matrix updatedMatrix, List<Step> stepsToUpdate)
	{
		Set<String> stepNames = getStepNames(stepsToUpdate);
		Map<String, List<Action>> actionsByStep = prepareUpdatedActions(updatedMatrix, stepNames);
		Matrix matrixToUpdate = matrixObjects.get(updatedMatrix.getName());
		
		updateActionsList(matrixToUpdate, actionsByStep);
		updateActionsInSteps(stepsToUpdate, matrixToUpdate, actionsByStep);
	}
	
	private void updateActionsInCurrentMatrix(Matrix updatedMatrix, Action currentAction)
	{
		int currentActionStepIndex = stepIndices.get(currentAction.getStepName());
		List<Step> stepsToUpdate = new ArrayList<>(steps.subList(currentActionStepIndex, steps.size()));
		
		Set<String> stepNames = getStepNames(stepsToUpdate);
		Map<String, List<Action>> actionsByStep = prepareUpdatedActions(updatedMatrix, stepNames);
		Matrix matrixToUpdate = matrixObjects.get(updatedMatrix.getName());
		Step currentStep = stepsToUpdate.remove(0);
		
		updateActionsListInCurrentMatrix(matrixToUpdate, currentAction, actionsByStep);
		updateActionsInCurrentStep(currentStep, currentAction, actionsByStep.get(currentStep.getName()));
		updateActionsInSteps(stepsToUpdate, matrixToUpdate, actionsByStep);
	}
	
	
	private Set<String> getStepNames(List<Step> steps)
	{
		return steps.stream().map(Step::getName).collect(Collectors.toSet());
	}
	
	private Map<String, List<Action>> prepareUpdatedActions(Matrix matrix, Set<String> stepNames)
	{
		Map<String, List<Action>> result = new HashMap<>();
		for (Action a : matrix.getActions())
		{
			String actionStepName = a.getStepName();
			if (stepNames.contains(actionStepName))
			{
				setOriginalProperties(a);
				result.computeIfAbsent(actionStepName, k -> new ArrayList<>())
						.add(a);
			}
		}
		return result;
	}
	
	private void updateActionsList(Matrix toUpdate, Map<String, List<Action>> updatedActionsByStep)
	{
		List<Action> oldActions = toUpdate.getActions(),
				newActions = new ArrayList<>(oldActions.size());
		
		//Keeping only actions that are not in possibly updated steps
		for (Action a : oldActions)
		{
			if (!updatedActionsByStep.containsKey(a.getStepName()))
				newActions.add(a);
		}
		
		//Adding actions from next updated steps
		addActions(newActions, updatedActionsByStep.values());
		
		toUpdate.setActions(newActions);
	}
	
	private void updateActionsInSteps(List<Step> stepsToUpdate, Matrix matrixToUpdate, Map<String, List<Action>> updatedActionsByStep)
	{
		for (Step step : stepsToUpdate)
		{
			List<Action> oldActions = step.getActions(),
					newActions = new ArrayList<>(oldActions.size()),
					updatedActions = updatedActionsByStep.get(step.getName());
			
			//Actions in the list are clustered by matrix: first, go all actions for current step from first matrix, then actions from seconds matrix, etc.
			//Replacing actions from updated matrix. Keeping actions from other matrices.
			int updatedMatrixIndex = matrixIndices.get(matrixToUpdate.getName());
			for (Action a : oldActions)
			{
				int matrixIndex = matrixIndices.get(a.getMatrix().getName());
				if (matrixIndex < updatedMatrixIndex)
				{
					newActions.add(a);
					continue;
				}
				
				if (updatedActions != null)
				{
					newActions.addAll(updatedActions);
					updatedActions = null;
				}
				
				if (matrixIndex > updatedMatrixIndex)
					newActions.add(a);
			}
			
			if (updatedActions != null)
				newActions.addAll(updatedActions);
			
			step.setActions(newActions);
		}
	}
	
	
	private void updateActionsListInCurrentMatrix(Matrix toUpdate, Action currentAction, Map<String, List<Action>> updatedActionsByStep)
	{
		List<Action> oldActions = toUpdate.getActions(),
				newActions = new ArrayList<>(oldActions.size());
		addOldActions(newActions, oldActions, updatedActionsByStep.keySet(), currentAction);
		
		updatedActionsByStep = new HashMap<>(updatedActionsByStep);
		List<Action> updatedCurrentStepActions = updatedActionsByStep.remove(currentAction.getStepName());
		addUpdatedActionsFromCurrentStep(newActions, updatedCurrentStepActions, currentAction);
		
		addActions(newActions, updatedActionsByStep.values());
		
		toUpdate.setActions(newActions);
	}
	
	private void addOldActions(List<Action> newActions, List<Action> oldActions, Set<String> updatedStepNames, Action currentAction)
	{
		//Keeping only actions that are not in possibly updated steps
		//Actions from currently executed step should be kept only till the current action, inclusively
		String currentActionStep = currentAction.getStepName(),
				currentActionId = currentAction.getIdInMatrix();
		boolean currentActionMet = false;
		for (Action a : oldActions)
		{
			String actionStep = a.getStepName();
			if (!updatedStepNames.contains(actionStep))
			{
				newActions.add(a);
				continue;
			}
			
			if (!currentActionStep.equals(actionStep) || currentActionMet)
				continue;
			
			newActions.add(a);
			if (currentActionId.equals(a.getIdInMatrix()))
				currentActionMet = true;
		}
	}
	
	private void addUpdatedActionsFromCurrentStep(List<Action> newActions, List<Action> updatedActions, Action currentAction)
	{
		String currentActionId = currentAction.getIdInMatrix();
		
		//Adding updated actions from currently executed step, taking only actions after current action
		boolean currentActionMet = false;
		for (Action a : updatedActions)
		{
			if (!currentActionMet)
			{
				if (currentActionId.equals(a.getIdInMatrix()))
					currentActionMet = true;
				continue;
			}
			newActions.add(a);
		}
	}
	
	private void addActions(List<Action> newActions, Collection<List<Action>> updatedActions)
	{
		for (List<Action> updatedActionsFromStep : updatedActions)
			newActions.addAll(updatedActionsFromStep);
	}
	
	
	private void updateActionsInCurrentStep(Step currentStep, Action currentAction, List<Action> updatedActions)
	{
		List<Action> oldActions = currentStep.getActions(),
				newActions = new ArrayList<>(oldActions.size());
		
		//Actions in the list are clustered by matrix: first, go all actions for current step from first matrix, then actions from seconds matrix, etc.
		//Keeping actions from updated matrix that are before current action, inclusively.
		//Replacing actions from updated matrix that go after current action.
		//Keeping actions from other matrices.
		Matrix matrixToUpdate = currentAction.getMatrix();
		String currentActionId = currentAction.getIdInMatrix();
		boolean currentActionMet = false;
		for (Action a : oldActions)
		{
			if (a.getMatrix() != matrixToUpdate)
			{
				newActions.add(a);
				continue;
			}
			
			if (!currentActionMet)
			{
				newActions.add(a);
				if (currentActionId.equals(a.getIdInMatrix()))
					currentActionMet = true;
				continue;
			}
			
			if (updatedActions != null)
			{
				addUpdatedActionsFromCurrentStep(newActions, updatedActions, currentAction);
				updatedActions = null;
			}
		}
		
		if (updatedActions != null)
			addUpdatedActionsFromCurrentStep(newActions, updatedActions, currentAction);
		
		currentStep.setActions(newActions);
	}
	
	
	private void setOriginalProperties(Action action)
	{
		action.setMatrix(matrixObjects.get(action.getMatrix().getName()));
		action.setStep(stepObjects.get(action.getStepName()));
	}
}