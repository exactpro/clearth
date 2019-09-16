/******************************************************************************
 * Copyright 2009-2019 Exactpro Systems Limited
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

package com.exactprosystems.clearth.test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.ConfigFiles;
import com.exactprosystems.clearth.ValueGenerator;
import com.exactprosystems.clearth.automation.*;
import com.exactprosystems.clearth.connectivity.MQConnectionFactory;
import com.exactprosystems.clearth.utils.ClearThException;

public abstract class ApplicationTest
{
	private final ConfigFiles configFiles;
	private Map<String, Scheduler> schedulers = new HashMap<String, Scheduler>();
	
	public ApplicationTest(ConfigFiles configFiles)
	{
		this.configFiles = configFiles;
	}
	
	public void executeTest(String matrixFileName, String schedulerName, String userName) throws Exception
	{
		if (ClearThCore.getInstance() == null)
			startTestApplication();
		
		Scheduler scheduler = getSchedulerByName(schedulerName);
		
		if (scheduler == null)
			throw new ClearThException("Scheduler is not found!");
		
		loadMatrix(matrixFileName, scheduler);
		scheduler.start(userName);
		
		try
		{
			while (scheduler.isRunning())
			{
				Thread.sleep(100);
			}
		}
		catch (InterruptedException e)
		{
			throw new ClearThException(e);
		}
		
		schedulers.put(schedulerName, scheduler);
	}
	
	protected void startTestApplication() throws Exception
	{
		getApplicationClass().getDeclaredConstructor(ConfigFiles.class).newInstance(configFiles);
	}
	
	protected void loadMatrix(String matrixName, Scheduler scheduler)
	{
		File file = new File(matrixName);
		scheduler.addMatrix(file);
	}
	
	protected Scheduler getSchedulerByName(String name)
	{
		return ClearThCore.getInstance().getSchedulersManager().getCommonSchedulerByName(name);
	}
	
	public Scheduler getScheduler(String name)
	{
		return schedulers.get(name);
	}
	
	protected abstract MQConnectionFactory getMQConnectionFactory() throws ClearThException;
	
	protected abstract StepFactory getStepFactory();
	
	protected abstract SchedulerFactory getSchedulerFactory(ExecutorFactory ef, StepFactory stf);
	
	protected abstract ExecutorFactory getExecutorFactory(ValueGenerator vg);
	
	protected abstract Class<? extends ClearThCore> getApplicationClass();
}
