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
import java.util.List;
import java.util.Map;

import com.exactprosystems.clearth.automation.persistence.ExecutorStateManager;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateOperator;
import com.exactprosystems.clearth.automation.persistence.FileStateOperator;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateInfo;
import com.exactprosystems.clearth.utils.XmlUtils;

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
	protected ExecutorStateInfo loadStateInfo(File sourceDir) throws IOException
	{
		File stateInfoFile = new File(sourceDir, FileStateOperator.STATEINFO_FILENAME);
		if (!stateInfoFile.isFile())
			return null;
		
		return (ExecutorStateInfo)XmlUtils.xmlFileToObject(stateInfoFile,
				FileStateOperator.STATEINFO_ANNOTATIONS, FileStateOperator.ALLOWED_CLASSES);
	}

	@Override
	protected void saveStateInfo(File destDir, ExecutorStateInfo stateInfo) throws IOException
	{
		XmlUtils.objectToXmlFile(stateInfo, new File(destDir, FileStateOperator.STATEINFO_FILENAME),
				FileStateOperator.STATEINFO_ANNOTATIONS, FileStateOperator.ALLOWED_CLASSES);
	}
	
	@Override
	protected ExecutorStateOperator createExecutorStateOperator(File storageDir) throws IOException
	{
		return new FileStateOperator(storageDir.toPath());
	}
	
	@Override
	protected ExecutorStateManager createExecutorStateManager(ExecutorStateOperator operator) throws IOException
	{
		return new ExecutorStateManager(operator);
	}
	
	@Override
	protected ExecutorStateManager createExecutorStateManager(ExecutorStateOperator operator, SimpleExecutor executor, StepFactory stepFactory, ReportsInfo reportsInfo) throws IOException
	{
		return new ExecutorStateManager(operator, executor, stepFactory, reportsInfo);
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
