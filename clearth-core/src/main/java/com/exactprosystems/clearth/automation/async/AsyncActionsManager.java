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

package com.exactprosystems.clearth.automation.async;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class AsyncActionsManager implements ActionMonitor, Closeable
{
	private static final Logger logger = LoggerFactory.getLogger(AsyncActionsManager.class);

	public static final String DEFAULT_ASYNC_THREAD_NAME = "Default";
	
	protected final GlobalContext globalContext;
	protected final Consumer<Action> actionToMvel;
	
	protected AsyncActionsThread defaultThread;
	protected Map<String, AsyncActionsThread> threads;
	protected final Set<AsyncActionData> startedActions, finishedActions;
	protected final BlockingQueue<AsyncActionData> actionsToProcess;
	protected final Map<String, Set<AsyncActionData>> actionsByStep,  //Arranges actions by step name to quickly get know if step needs to wait for actions end
		actionsBeforeStep;  //Arranges actions by step name to quickly get know which actions should finish before the step starts
	protected final Set<AsyncActionData> actionsForScheduler;
	
	public AsyncActionsManager(GlobalContext globalContext, Consumer<Action> actionToMvel)
	{
		this.globalContext = globalContext;
		this.actionToMvel = actionToMvel;
		
		startedActions = createActionDataSet();
		finishedActions = createActionDataSet();
		actionsToProcess = createActionDataQueue();
		
		actionsByStep = createActionsByStepStorage();
		actionsBeforeStep = createActionsByStepStorage();
		actionsForScheduler = createActionDataSet();
	}
	
	@Override
	public void actionFinished(AsyncActionData actionData)
	{
		if (!startedActions.remove(actionData))
			return;

		finishedActions.add(actionData);
		
		Action action = actionData.getAction();
		// Need to apply action parameters to MVEL right here because they could be used in further actions.
		// Setting this params in ActionExecutor on async action status checking could cause calculation exceptions
		// because next action may be already started but previous params weren't applied to MVEL
		actionToMvel.accept(action);
		
		refreshState(action);
		
		try
		{
			actionsToProcess.put(actionData);
		}
		catch (InterruptedException e)
		{
			getLogger().error("Could not update finished actions storage", e);
		}
	}

	private void refreshState(Action action)
	{
		action.setPayloadFinished(true);
		action.getStep().refreshAsyncFlag(action);
	}
	
	@Override
	public void close() throws IOException
	{
		interruptExecution();
		
		startedActions.clear();
		finishedActions.clear();
		actionsToProcess.clear();
		actionsByStep.clear();
		actionsBeforeStep.clear();
		actionsForScheduler.clear();
	}
	
	public void interruptExecution()
	{
		while (!startedActions.isEmpty())
		{
			AsyncActionData action = startedActions.iterator().next();
			action.setFinished(new Date());
			action.setResult(DefaultResult.failed("Execution has been interrupted."));
			actionFinished(action);
		}
		
		if (defaultThread != null)
			defaultThread.interruptExecution();
		
		if (threads != null)
		{
			for (AsyncActionsThread t : threads.values())
				t.interruptExecution();
		}
	}
	
	/**
	 * Triggers asynchronous action execution. Action is inserted into queue of corresponding actions group. 
	 * Its state can be monitored with isActionFinished method
	 * @param actionData to execute asynchronously
	 * @throws InterruptedException
	 */
	public void addAsyncAction(AsyncActionData actionData) throws InterruptedException
	{
		addActionToHystory(actionData);
		
		AsyncActionsThread thread = getThreadForAction(actionData.getAction());
		try
		{
			thread.addAction(actionData);
		}
		catch (InterruptedException e)
		{
			removeActionFromHistory(actionData);
			throw e;
		}
	}
	
	
	/**
	 * Returns finished action from managers's history. Action is removed from history. Its result must be processed by the caller.
	 * @return next finished action to process its result, null if no actions are finished yet
	 */
	public AsyncActionData getNextFinishedAction()
	{
		return actionsToProcess.poll();
	}
	
	public boolean isActionFinished(AsyncActionData actionData)
	{
		return finishedActions.contains(actionData);
	}
	
	public Set<AsyncActionData> getStepActions(String stepName)
	{
		return actionsByStep.get(stepName);
	}

	public Set<AsyncActionData> getBeforeStepActions(String stepName)
	{
		return actionsBeforeStep.get(stepName);
	}
	
	public Set<AsyncActionData> getSchedulerActions()
	{
		return actionsForScheduler;
	}
	
	
	protected Logger getLogger()
	{
		return logger;
	}
	
	protected Set<AsyncActionData> createActionDataSet()
	{
		return Collections.newSetFromMap(new ConcurrentHashMap<AsyncActionData, Boolean>());
	}
	
	protected BlockingQueue<AsyncActionData> createActionDataQueue()
	{
		return new LinkedBlockingQueue<AsyncActionData>();
	}
	
	protected Map<String, Set<AsyncActionData>> createActionsByStepStorage()
	{
		return new ConcurrentHashMap<String, Set<AsyncActionData>>();
	}
	
	
	//*** History management ***
	
	protected void addActionToHystory(AsyncActionData actionData)
	{
		startedActions.add(actionData);
		
		switch (actionData.getAction().getWaitAsyncEnd())
		{
		case STEP :	addActionToStep(actionData); break;
		case SCHEDULER : addActionForScheduler(actionData); break;
		default : break;
		}
	}
	
	protected void removeActionFromHistory(AsyncActionData actionData)
	{
		startedActions.remove(actionData);
		
		switch (actionData.getAction().getWaitAsyncEnd())
		{
		case STEP :	removeActionFromStep(actionData); break;
		case SCHEDULER : removeActionForScheduler(actionData); break;
		default : break;
		}
	}

	protected void addActionToStep(AsyncActionData actionData)
	{
		String beforeStepName = actionData.getAction().getWaitAsyncEndStep();
		Set<AsyncActionData> set = (isNotBlank(beforeStepName))
			? actionsBeforeStep.computeIfAbsent(beforeStepName, key -> createActionDataSet())
			: actionsByStep.computeIfAbsent(actionData.getAction().getStepName(), key -> createActionDataSet());
		set.add(actionData);
	}
	
	protected void removeActionFromStep(AsyncActionData actionData)
	{
		String beforeStepName = actionData.getAction().getWaitAsyncEndStep();
		Set<AsyncActionData> set = (isNotBlank(beforeStepName))
			? actionsBeforeStep.get(beforeStepName)
			: actionsByStep.get(actionData.getAction().getStepName());
		if (set != null)
			set.remove(actionData);
	}
	
	protected void addActionForScheduler(AsyncActionData actionData)
	{
		actionsForScheduler.add(actionData);
	}
	
	protected void removeActionForScheduler(AsyncActionData actionData)
	{
		actionsForScheduler.remove(actionData);
	}
	
	
	//*** Action execution ***
	
	protected AsyncActionsThread createThread(String name)
	{
		AsyncActionsThread result = new AsyncActionsThread(name, globalContext, this);
		result.start();
		return result;
	}
	
	protected AsyncActionsThread getDefaultThread()
	{
		if (defaultThread == null)
			defaultThread = createThread(Thread.currentThread().getName()+":"+DEFAULT_ASYNC_THREAD_NAME);
		return defaultThread;
	}
	
	protected AsyncActionsThread getNamedThread(String groupName)
	{
		if (threads == null)
			threads = new HashMap<String, AsyncActionsThread>();
		AsyncActionsThread result = threads.get(groupName);
		if (result == null)
		{
			result = createThread(groupName);
			threads.put(groupName, result);
		}
		return result;
	}
	
	protected AsyncActionsThread getThreadForAction(Action action)
	{
		String group = action.getAsyncGroup();
		if ((group == null) || (group.isEmpty()))
			return getDefaultThread();
		else
			return getNamedThread(Thread.currentThread().getName()+":"+group);
	}
}
