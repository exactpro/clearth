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
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.data.DataHandlingException;
import com.exactprosystems.clearth.data.TestExecutionHandler;

public class ExecutorStateManager
{
	private final ExecutorStateOperator operator;
	private final ExecutorState state;
	
	public ExecutorStateManager(ExecutorStateOperator operator, SimpleExecutor executor, StepFactory stepFactory, ReportsInfo reportsInfo)
	{
		this.operator = operator;
		this.state = createExecutorState(executor, stepFactory, reportsInfo);
	}
	
	public ExecutorStateManager(ExecutorStateOperator operator) throws IOException
	{
		this.operator = operator;
		this.state = operator.load();
	}
	
	
	protected ExecutorState createExecutorState(SimpleExecutor executor, StepFactory stepFactory, ReportsInfo reportsInfo)
	{
		return new ExecutorState(executor, stepFactory, reportsInfo);
	}
	
	protected void initExecutor(SimpleExecutor executor)
	{
	}
	
	public SimpleExecutor executorFromState(Scheduler scheduler, ExecutorFactory executorFactory, Date businessDay, Date baseTime, String startedByUser)
			throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, AutomationException, DataHandlingException
	{
		Map<String, Preparable> preparableActions = new HashMap<>();
		
		ExecutorStateInfo stateInfo = state.getStateInfo();
		ExecutorStateObjects stateObjects = state.getStateObjects();
		
		List<Step> steps = null;
		Map<Step, Map<String, StepContext>> allStepContexts = null;
		List<StepState> stepStates = stateInfo.getSteps();
		if (stepStates != null)
		{
			steps = new ArrayList<>();
			allStepContexts = new HashMap<>();
			for (StepState stepState : stepStates)
			{
				Step restored = stepState.stepFromState(scheduler.getStepFactory());
				steps.add(restored);
				allStepContexts.put(restored, stepState.getStepContexts());
			}
		}
		
		List<Matrix> matrices = null;
		List<MatrixState> matrixStates = stateObjects.getMatrices();
		if (matrixStates != null)
		{
			matrices = new ArrayList<>();
			for (MatrixState matrixState : matrixStates)
			{
				Matrix m = matrixState.matrixFromState(steps);
				matrices.add(m);
				if (steps != null)
				{
					for (Action a : m.getActions())
						for (Step step : steps)
							if (step.getName().equals(a.getStepName()))
							{
								step.addAction(a);
								if (!preparableActions.containsKey(a.getName()) && a.isExecutable() &&
										a instanceof Preparable)
									preparableActions.put(a.getName(), (Preparable) a);
								break;
							}
					
					for (Step step : steps)
					{
						Map<String, StepContext> stepContexts = allStepContexts.get(step);
						if (stepContexts == null)
							continue;
						
						if (stepContexts.containsKey(m.getName()))
						{
							Map<Matrix, StepContext> sc = step.getStepContexts();
							if (sc == null)
							{
								sc = new LinkedHashMap<Matrix, StepContext>();
								step.setStepContexts(sc);
							}
							sc.put(m, stepContexts.get(m.getName()));
						}
					}
				}
			}
		}
		
		TestExecutionHandler executionHandler = ClearThCore.getInstance().getDataHandlersFactory().createTestExecutionHandler(scheduler.getName());
		GlobalContext globalContext = executorFactory.createGlobalContext(businessDay, baseTime, stateInfo.isWeekendHoliday(), stateInfo.getHolidays(),
						startedByUser, executionHandler);
		
		SimpleExecutor result = executorFactory.createExecutor(scheduler, steps, matrices, globalContext, preparableActions, stateInfo.getReportsInfo().getReportsConfig());
		result.setFixedIds(stateObjects.getFixedIDs());
		result.setStarted(stateInfo.getStarted());
		result.setEnded(stateInfo.getEnded());
		initExecutor(result);
		return result;
	}
	
	public void save() throws IOException
	{
		operator.save(state);
	}
	
	public void update(Action lastExecutedAction) throws IOException
	{
		operator.update(state, lastExecutedAction);
	}
}
