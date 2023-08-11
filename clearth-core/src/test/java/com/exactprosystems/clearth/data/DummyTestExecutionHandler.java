/******************************************************************************
 * Copyright 2009-2023 Exactpro Systems Limited
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

package com.exactprosystems.clearth.data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.StepMetadata;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;

public class DummyTestExecutionHandler implements TestExecutionHandler
{
	private boolean active;
	
	private boolean testStarted = false,
			testEnded = false;
	private Collection<String> matrices = null;
	private String lastStep;
	private Map<String, StepExecutionInfo> steps = null;
	private Map<String, Result> actionResults = null;
	
	public DummyTestExecutionHandler(boolean active)
	{
		this.active = active;
	}
	
	@Override
	public void close() throws Exception
	{
	}
	
	@Override
	public void onTestStart(Collection<String> matrices, GlobalContext globalContext)
			throws TestExecutionHandlingException
	{
		testStarted = true;
		this.matrices = matrices;
	}
	
	@Override
	public void onTestEnd() throws TestExecutionHandlingException
	{
		testEnded = true;
	}
	
	@Override
	public void onGlobalStepStart(StepMetadata stepData) throws TestExecutionHandlingException
	{
		if (steps == null)
			steps = new LinkedHashMap<>();
		
		lastStep = stepData.getName();
		StepExecutionInfo info = new StepExecutionInfo(lastStep, true, false);
		steps.put(lastStep, info);
	}
	
	@Override
	public void onGlobalStepEnd() throws TestExecutionHandlingException
	{
		steps.get(lastStep).setEnded(true);
	}
	
	@Override
	public HandledTestExecutionId onAction(Action action) throws TestExecutionHandlingException
	{
		if (actionResults == null)
			actionResults = new LinkedHashMap<>();
		actionResults.put(createActionKey(action), null);
		return new UuidTestExecutionId();
	}
	
	@Override
	public void onActionResult(Result result, Action action) throws TestExecutionHandlingException
	{
		actionResults.put(createActionKey(action), copyResultData(result));
	}
	
	@Override
	public boolean isActive()
	{
		return active;
	}
	
	
	public void setActive(boolean active)
	{
		this.active = active;
	}
	
	
	public boolean isTestStarted()
	{
		return testStarted;
	}
	
	public boolean isTestEnded()
	{
		return testEnded;
	}
	
	public Collection<String> getMatrices()
	{
		return matrices;
	}
	
	public StepExecutionInfo getStepInfo(String name)
	{
		return steps != null ? steps.get(name) : null;
	}
	
	public Result getActionResult(String matrixName, String stepName, String actionId)
	{
		return actionResults != null
				? actionResults.get(createActionKey(matrixName, stepName, actionId))
				: null;
	}
	
	
	private String createActionKey(Action action)
	{
		return createActionKey(action.getMatrix().getName(), action.getStepName(), action.getIdInMatrix());
	}
	
	private String createActionKey(String matrixName, String stepName, String actionId)
	{
		return matrixName+"_"+stepName+"_"+actionId;
	}
	
	private Result copyResultData(Result result)
	{
		Result copy = new DefaultResult();
		if (result == null)
			return copy;
		
		copy.setSuccess(result.isSuccess());
		for (EncodedClearThMessage m : result.getLinkedMessages())
			copy.addLinkedMessage(m);
		
		return copy;
	}
	
	
	public static class StepExecutionInfo
	{
		private final String name;
		private boolean started,
				ended;
		
		public StepExecutionInfo(String name, boolean started, boolean ended)
		{
			this.name = name;
			this.started = started;
			this.ended = ended;
		}
		
		
		public String getName()
		{
			return name;
		}
		
		
		public boolean isStarted()
		{
			return started;
		}
		
		public void setStarted(boolean started)
		{
			this.started = started;
		}
		
		
		public boolean isEnded()
		{
			return ended;
		}
		
		public void setEnded(boolean ended)
		{
			this.ended = ended;
		}
	}
}
