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
import java.util.List;
import java.util.stream.Collectors;

import com.exactprosystems.clearth.automation.Matrix;
import com.exactprosystems.clearth.automation.ReportsInfo;
import com.exactprosystems.clearth.automation.SimpleExecutor;
import com.exactprosystems.clearth.automation.StepFactory;

public class ExecutorState
{
	private final ExecutorStateInfo stateInfo;
	private final ExecutorStateObjects stateObjects;
	
	public ExecutorState(ExecutorStateInfo stateInfo, ExecutorStateObjects stateObjects)
	{
		this.stateInfo = stateInfo;
		this.stateObjects = stateObjects;
	}
	
	public ExecutorState()
	{
		stateInfo = createStateInfo();
		stateObjects = createStateObjects();
	}
	
	public ExecutorState(SimpleExecutor executor, StepFactory stepFactory, ReportsInfo reportsInfo)
	{
		this();
		init(stateInfo, executor, stepFactory, reportsInfo);
		init(stateObjects, executor, stepFactory);
	}
	
	
	public ExecutorStateInfo getStateInfo()
	{
		return stateInfo;
	}
	
	public ExecutorStateObjects getStateObjects()
	{
		return stateObjects;
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
		return new MatrixState(matrix);
	}
	
	
	protected void init(ExecutorStateInfo stateInfo, SimpleExecutor executor, StepFactory stepFactory, ReportsInfo reportsInfo)
	{
		List<StepState> steps = executor.getSteps().stream()
				.map(s -> stepFactory.createStepState(s))
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
	
	protected void init(ExecutorStateObjects stateObjects, SimpleExecutor executor, StepFactory stepFactory)
	{
		List<MatrixState> matrices = executor.getMatrices().stream()
				.map(m -> createMatrixState(m))
				.collect(Collectors.toList());
		
		stateObjects.setMatrices(matrices);
		stateObjects.setFixedIDs(executor.getFixedIds());
	}
}
