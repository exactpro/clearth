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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.data.DataHandlingException;
import com.exactprosystems.clearth.data.TestExecutionHandler;
import com.exactprosystems.clearth.utils.Pair;

public class ExecutorStateManager<C extends ExecutorStateContext>
{
	private static final Logger logger = LoggerFactory.getLogger(ExecutorStateManager.class);
	
	private final ExecutorStateOperatorFactory<C> operatorFactory;
	private ExecutorStateInfo stateInfo;
	private C context;
	
	public ExecutorStateManager(ExecutorStateOperatorFactory<C> operatorFactory)
	{
		this.operatorFactory = operatorFactory;
	}
	
	
	public ExecutorStateInfo getStateInfo()
	{
		return stateInfo;
	}
	
	public SimpleExecutor executorFromState(Scheduler scheduler, ExecutorFactory executorFactory, Date businessDay, Date baseTime, String startedByUser)
			throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException, AutomationException, DataHandlingException, ExecutorStateException, IOException
	{
		if (stateInfo == null)
			throw new ExecutorStateException("No state info available");
		
		Map<String, Preparable> preparableActions = new HashMap<>();
		
		ExecutorStateObjects stateObjects;
		try (ExecutorStateOperator<C> operator = operatorFactory.createOperator())
		{
			stateObjects = operator.loadStateObjects(context);
		}
		catch (Exception e)
		{
			throw new IOException("Error while loading state details", e);
		}
		
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
	
	public void save(SimpleExecutor executor, ReportsInfo reportsInfo) throws IOException
	{
		try (ExecutorStateOperator<C> operator = operatorFactory.createOperator())
		{
			save(executor, reportsInfo, operator);
		}
		catch (Exception e)
		{
			throw new IOException("Error while saving state", e);
		}
	}
	
	public ExecutorStateUpdater<C> saveBeforeUpdates(SimpleExecutor executor, ReportsInfo reportsInfo) throws IOException
	{
		ExecutorStateOperator<C> operator = operatorFactory.createOperator();
		try
		{
			save(executor, reportsInfo, operator);
		}
		catch (Exception e)
		{
			try
			{
				operator.close();
			}
			catch (Exception e1)
			{
				e.addSuppressed(e1);
			}
			throw e;
		}
		
		return createExecutorStateUpdater(stateInfo, context, operator);
	}
	
	public ExecutorStateUpdater<C> createStateUpdater() throws IOException
	{
		return createExecutorStateUpdater(stateInfo, context, operatorFactory.createOperator());
	}
	
	public void load() throws IOException
	{
		logger.info("Loading state info");
		
		Pair<ExecutorStateInfo, C> loaded;
		try (ExecutorStateOperator<C> operator = operatorFactory.createOperator())
		{
			loaded = operator.loadStateInfo();
		}
		catch (Exception e)
		{
			throw new IOException("Error while loading state", e);
		}
		
		stateInfo = loaded.getFirst();
		context = loaded.getSecond();
	}
	
	public void updateSteps() throws IOException, ExecutorStateException
	{
		logger.info("Updating state of steps");
		
		if (stateInfo == null)
			throw new ExecutorStateException("No state info to update");
		
		try (ExecutorStateOperator<C> operator = operatorFactory.createOperator())
		{
			operator.updateSteps(stateInfo, context);
		}
		catch (Exception e)
		{
			throw new IOException("Error while updating steps", e);
		}
	}
	
	
	protected void initExecutor(SimpleExecutor executor)
	{
	}
	
	
	protected ExecutorStateInfo createStateInfo()
	{
		return new ExecutorStateInfo();
	}
	
	protected ExecutorStateObjects createStateObjects()
	{
		return new ExecutorStateObjects();
	}
	
	protected MatrixState createMatrixState(Matrix matrix)
	{
		return new MatrixState(matrix, a -> createActionState(a));
	}
	
	public StepState createStepState()
	{
		return new StepState();
	}
	
	public StepState createStepState(Step step)
	{
		return new StepState(step);
	}
	
	public StepState createStepState(StepState stepState)
	{
		return new StepState(stepState);
	}
	
	protected ActionState createActionState(Action action)
	{
		return new ActionState(action);
	}
	
	protected ExecutorStateUpdater<C> createExecutorStateUpdater(ExecutorStateInfo stateInfo, C context, ExecutorStateOperator<C> operator)
	{
		return new ExecutorStateUpdater<>(stateInfo, context, operator,
				step -> createStepState(step),
				action -> createActionState(action));
	}
	
	protected void init(ExecutorStateInfo stateInfo, SimpleExecutor executor, ReportsInfo reportsInfo)
	{
		List<StepState> steps = executor.getSteps().stream()
				.map(s -> createStepState(s))
				.collect(Collectors.toList());
		
		List<String> matricesNames = executor.getMatrices().stream()
				.map(m -> new File(m.getFileName()).getName())
				.collect(Collectors.toList());
		
		stateInfo.setSteps(steps);
		stateInfo.setMatrices(matricesNames);
		stateInfo.setWeekendHoliday(executor.isWeekendHoliday());
		stateInfo.setHolidays(executor.getHolidays());
		stateInfo.setBusinessDay(executor.getBusinessDay());
		stateInfo.setStartedByUser(executor.getStartedByUser());
		stateInfo.setStarted(executor.getStarted());
		stateInfo.setEnded(executor.getEnded());
		stateInfo.setReportsInfo(reportsInfo);
	}
	
	protected void init(ExecutorStateObjects stateObjects, SimpleExecutor executor)
	{
		List<MatrixState> matrices = executor.getMatrices().stream()
				.map(m -> createMatrixState(m))
				.collect(Collectors.toList());
		
		stateObjects.setMatrices(matrices);
		stateObjects.setFixedIDs(executor.getFixedIds());
	}
	
	private void save(SimpleExecutor executor, ReportsInfo reportsInfo, ExecutorStateOperator<C> operator) throws IOException
	{
		logger.info("Saving state of '{}'", executor.getName());
		
		stateInfo = createStateInfo();
		ExecutorStateObjects stateObjects = createStateObjects();
		init(stateInfo, executor, reportsInfo);
		init(stateObjects, executor);
		
		context = operator.save(stateInfo, stateObjects);
	}
}
