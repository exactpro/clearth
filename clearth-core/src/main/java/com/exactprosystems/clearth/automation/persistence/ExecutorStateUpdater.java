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

package com.exactprosystems.clearth.automation.persistence;

import java.io.IOException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.*;

public class ExecutorStateUpdater<C extends ExecutorStateContext> implements AutoCloseable
{
	private static final Logger logger = LoggerFactory.getLogger(ExecutorStateUpdater.class);
	
	private final ExecutorStateInfo stateInfo;
	private final C context;
	private final ExecutorStateOperator<C> operator;
	private final Function<Step, StepState> stepStateCreator;
	private final Function<Action, ActionState> actionStateCreator;
	
	public ExecutorStateUpdater(ExecutorStateInfo stateInfo, C context, ExecutorStateOperator<C> operator, 
			Function<Step, StepState> stepStateCreator, Function<Action, ActionState> actionStateCreator)
	{
		this.stateInfo = stateInfo;
		this.context = context;
		this.operator = operator;
		this.stepStateCreator = stepStateCreator;
		this.actionStateCreator = actionStateCreator;
	}
	
	@Override
	public void close() throws Exception
	{
		operator.close();
	}
	
	
	public void update(Action lastExecutedAction) throws IOException, ExecutorStateException
	{
		logger.debug("Updating state after action '{}' from matrix '{}'", lastExecutedAction.getIdInMatrix(), lastExecutedAction.getMatrix().getName());
		
		if (stateInfo == null)
			throw noStateInfoError();
		
		Step step = lastExecutedAction.getStep();
		StepState stepState = stepStateCreator.apply(step);
		updateStateInfo(step, stepState);
		
		ActionState actionState = actionStateCreator.apply(lastExecutedAction);
		operator.update(stateInfo, context, lastExecutedAction, actionState);
	}
	
	public void update(Step lastFinishedStep) throws IOException, ExecutorStateException
	{
		logger.debug("Updating state after global step '{}'", lastFinishedStep.getName());
		
		if (stateInfo == null)
			throw noStateInfoError();
		
		StepState stepState = stepStateCreator.apply(lastFinishedStep);
		updateStateInfo(lastFinishedStep, stepState);
		
		operator.update(stateInfo, context, lastFinishedStep, stepState);
	}
	
	public void updateActionReportsPath(String actionReportsPath) throws IOException
	{
		stateInfo.getReportsInfo().setActionReportsPath(actionReportsPath);
		operator.updateStateInfo(stateInfo, context);
	}
	
	
	protected void updateStateInfo(Step step, StepState updatedState) throws ExecutorStateException
	{
		StepState oldState = stateInfo.getStep(step.getName());
		stateInfo.updateStep(oldState, updatedState);
	}
	
	
	private ExecutorStateException noStateInfoError()
	{
		return new ExecutorStateException("No state info to update");
	}
}
