/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

import com.exactprosystems.clearth.automation.persistence.DefaultExecutorState;
import com.exactprosystems.clearth.automation.persistence.ExecutorState;
import com.exactprosystems.clearth.automation.persistence.ExecutorStateInfo;
import com.exactprosystems.clearth.utils.XmlUtils;

public class DefaultScheduler extends Scheduler
{
	public DefaultScheduler(String name, String configsRoot, String schedulerDirName, ExecutorFactory executorFactory, StepFactory stepFactory) throws Exception
	{
		super(name, configsRoot, schedulerDirName, executorFactory, stepFactory);
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
		return new DefaultActionGenerator(stepsMap, matricesContainer, preparableActions);
	}
	
	@Override
	public SequentialExecutor createSequentialExecutor(Scheduler scheduler, String userName, Map<String, Preparable> preparableActions)
	{
		return new DefaultSequentialExecutor(executorFactory, scheduler, userName, preparableActions);
	}
	
	
	@Override
	protected ExecutorStateInfo loadStateInfo(File sourceDir) throws IOException
	{
		return (ExecutorStateInfo)XmlUtils.xmlFileToObject(new File(sourceDir, ExecutorState.STATEINFO_FILENAME),
				DefaultExecutorState.STATEINFO_ANNOTATIONS, DefaultExecutorState.ALLOWED_CLASSES);
	}

	@Override
	protected void saveStateInfo(File destDir, ExecutorStateInfo stateInfo) throws IOException
	{
		XmlUtils.objectToXmlFile(stateInfo, new File(destDir, ExecutorState.STATEINFO_FILENAME),
				DefaultExecutorState.STATEINFO_ANNOTATIONS, DefaultExecutorState.ALLOWED_CLASSES);
	}

	@Override
	protected ExecutorState createExecutorState(File sourceDir) throws IOException
	{
		return new DefaultExecutorState(sourceDir);
	}

	@Override
	protected ExecutorState createExecutorState(Executor executor, StepFactory stepFactory, ReportsInfo reportsInfo)
	{
		return new DefaultExecutorState(executor, stepFactory, reportsInfo);
	}

	
	@Override
	protected void initExecutor(Executor executor)
	{
		//Nothing to do here
	}

	@Override
	protected void initSequentialExecutor(SequentialExecutor executor)
	{
		//Nothing to do here
	}

	@Override
	protected void initSchedulerOnRestore(Executor executor)
	{
		//Nothing to do here
	}
}
