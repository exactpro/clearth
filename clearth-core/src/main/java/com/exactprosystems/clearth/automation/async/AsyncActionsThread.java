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

package com.exactprosystems.clearth.automation.async;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.exactprosystems.clearth.automation.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;

public class AsyncActionsThread extends Thread
{
	private static final Logger logger = LoggerFactory.getLogger(AsyncActionsThread.class);
	
	protected final AtomicBoolean interrupted; 
	
	protected final BlockingQueue<AsyncActionData> actions;
	protected final GlobalContext globalContext;
	protected final ActionMonitor monitor;

	public AsyncActionsThread(String name, GlobalContext globalContext, ActionMonitor monitor)
	{
		super(name);
		this.interrupted = new AtomicBoolean(false);
		this.actions = createActionsQueue();
		this.globalContext = globalContext;
		this.monitor = monitor;
	}
	
	@Override
	public void run()
	{
		while (!isExecutionInterrupted())
		{
			AsyncActionData a;
			try
			{
				a = getNextAction();
				if (a == null)
					continue;
			}
			catch (InterruptedException e)
			{
				getLogger().warn("Execution interrupted");
				break;
			}
			
			try
			{
				Action action = a.getAction();
				String actionDesc = null;
				if (getLogger().isDebugEnabled())
				{
					actionDesc = action.getDescForLog("");
					getLogger().debug("Starting{}", actionDesc);
				}
				
				executeAction(a);
				
				if (getLogger().isDebugEnabled())
					getLogger().debug("Finished{}", actionDesc != null ? actionDesc : action.getDescForLog(""));
			}
			catch (Exception e)
			{
				handleActionCrash(a, e);
			}
			finally
			{
				notifyMonitor(a);
			}
		}
	}
	
	public void addAction(AsyncActionData actionData) throws InterruptedException
	{
		actions.put(actionData);
	}
	
	public void interruptExecution()
	{
		interrupted.set(true);
		interrupt();
	}
	
	public boolean isExecutionInterrupted()
	{
		return interrupted.get();
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	protected BlockingQueue<AsyncActionData> createActionsQueue()
	{
		return new LinkedBlockingQueue<AsyncActionData>();
	}
	
	protected AsyncActionData getNextAction() throws InterruptedException
	{
		return actions.poll(5000, TimeUnit.MILLISECONDS);
	}
	
	
	protected void executeAction(AsyncActionData actionData) throws Exception
	{
		//Action attributes must not be set directly not to harm main scheduler thread. It will get new values from AsyncActionData and apply them.
		Action a = actionData.getAction();
		actionData.setStarted(new Date());
		Result result = a.executeForResult(actionData.getStepContext(), actionData.getMatrixContext(), globalContext);
		actionData.setFinished(new Date());
		actionData.setResult(result);
	}
	
	protected void handleActionCrash(AsyncActionData actionData, Exception e)
	{
		actionData.getAction().setResult(DefaultResult.failed(e));
	}
	
	protected void notifyMonitor(AsyncActionData actionData)
	{
		monitor.actionFinished(actionData);
	}
}
