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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.automation.persistence.ExecutorStateManager;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateOperatorFactory;
import com.exactprosystems.clearth.automation.persistence.db.DbStateOperator;
import com.exactprosystems.clearth.automation.persistence.db.DbStateOperatorFactory;

public class DefaultScheduler extends Scheduler
{
	public DefaultScheduler(String name, String configsRoot, String schedulerDirName, 
			ExecutorFactory executorFactory, StepFactory stepFactory, ActionGeneratorResources generatorResources) throws Exception
	{
		super(name, configsRoot, schedulerDirName, executorFactory, stepFactory, generatorResources);
	}

	@Override
	public SchedulerData createSchedulerData(String name, String configsRoot, String schedulerDirName,
	                                         String lastExecutedDataDir, String matricesDir) throws Exception
	{
		return new DefaultSchedulerData(name, configsRoot, schedulerDirName, matricesDir, lastExecutedDataDir, stepFactory);
	}
	
	@Override
	public void initEx() throws Exception
	{
		//Nothing to do here
	}
	
	@Override
	public ActionGenerator createActionGenerator(Map<String, Step> stepsMap, List<Matrix> matricesContainer, Map<String, Preparable> preparableActions)
	{
		return new DefaultActionGenerator(stepsMap, matricesContainer, preparableActions, generatorResources);
	}
	
	@Override
	public SequentialExecutor createSequentialExecutor(Scheduler scheduler, String userName, Map<String, Preparable> preparableActions)
	{
		return new DefaultSequentialExecutor(executorFactory, scheduler, userName, preparableActions);
	}
	
	
	@Override
	protected ExecutorStateManager<?> loadStateInfo(File sourceDir) throws IOException
	{
		File stateInfoFile = new File(sourceDir, DbStateOperator.STATEINFO_FILENAME);
		if (!stateInfoFile.isFile())
			return null;
		
		ExecutorStateOperatorFactory<?> operatorFactory = createExecutorStateOperatorFactory(sourceDir);
		ExecutorStateManager<?> es = createExecutorStateManager(operatorFactory);
		es.load();
		return es;
	}
	
	@Override
	protected ExecutorStateOperatorFactory<?> createExecutorStateOperatorFactory(File storageDir)
	{
		Path stateFile = storageDir.toPath().resolve(DbStateOperator.STATEINFO_FILENAME);
		return new DbStateOperatorFactory(stateFile);
	}
	
	@Override
	protected ExecutorStateManager<?> createExecutorStateManager(ExecutorStateOperatorFactory<?> operatorFactory)
	{
		return new ExecutorStateManager<>(operatorFactory);
	}
	
	
	@Override
	protected void initExecutor(SimpleExecutor executor)
	{
		//Nothing to do here
	}
	
	@Override
	protected void initSequentialExecutor(SequentialExecutor executor)
	{
		//Nothing to do here
	}
	
	@Override
	protected void initSchedulerOnRestore(SimpleExecutor executor)
	{
		//Nothing to do here
	}
}
