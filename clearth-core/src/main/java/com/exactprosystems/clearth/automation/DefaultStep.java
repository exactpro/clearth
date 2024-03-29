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

package com.exactprosystems.clearth.automation;

import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class DefaultStep extends Step
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultStep.class);
	
	public DefaultStep()
	{
		super();
	}
	
	public DefaultStep(String name, String kind, String startAt, StartAtType startAtType, boolean waitNextDay, String parameter, boolean askForContinue, boolean askIfFailed, boolean execute, String comment)
	{
		super(name, kind, startAt, startAtType, waitNextDay, parameter, askForContinue, askIfFailed, execute, comment);
	}

	public DefaultStep(Map<String, String> record) throws IOException
	{
		super(record);
	}

	protected Logger getLogger()
	{
		return logger;
	}
	
	
	@Override
	public void init()
	{
		stepData.setStarted(null);
		stepData.setFinished(null);
		stepData.setExecutionProgress(new ActionsExecutionProgress());
		interrupted = false;
		paused = false;
	}

	@Override
	public void initBeforeReplay()
	{
		stepData.setExecutionProgress(new ActionsExecutionProgress());
		statusComment = null;
		stepData.setFinished(null);
	}
	
	
	@Override
	protected void beforeActions(GlobalContext globalContext)
	{
	}

	@Override
	protected void beforeAction(Action action, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
	}

	@Override
	protected void actionFailover(Action action, FailoverException e, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
	}

	@Override
	protected void afterAction(Action action, StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
	{
	}

	@Override
	protected void afterActions(GlobalContext globalContext, Map<Matrix, StepContext> stepContexts)
	{
	}
}
