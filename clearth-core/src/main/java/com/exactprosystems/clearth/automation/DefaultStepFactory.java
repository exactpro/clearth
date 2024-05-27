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

import com.exactprosystems.clearth.automation.persistence.StepState;

import java.io.IOException;
import java.util.Map;

public class DefaultStepFactory extends StepFactory
{
	@Override
	public Step createStep()
	{
		return new DefaultStep();
	}

	@Override
	public Step createStep(String name, String kind, String startAt, StartAtType startAtType, boolean waitNextDay, String parameter, boolean askForContinue, boolean askIfFailed, boolean execute, String comment)
	{
		return new DefaultStep(name, kind, startAt, startAtType, waitNextDay, parameter, askForContinue, askIfFailed, execute, comment);
	}

	@Override
	public Step createStep(Map<String, String> record) throws IOException
	{
		return new DefaultStep(record);
	}

	@Override
	public StepState createStepState()
	{
		return new StepState();
	}

	@Override
	public StepState createStepState(Step step)
	{
		return new StepState(step);
	}

	@Override
	public StepState createStepState(StepState stepState)
	{
		return new StepState(stepState);
	}
	
	@Override
	protected boolean validStepKindEx(String stepKind)
	{
		return false;
	}
}
