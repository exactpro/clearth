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

import com.exactprosystems.clearth.automation.persistence.StepState;

import java.io.IOException;
import java.util.Map;

public abstract class StepFactory
{
	public abstract Step createStep();
	public abstract Step createStep(String name, String kind, String startAt, StartAtType startAtType, boolean waitNextDay, String parameter, boolean askForContinue, boolean askIfFailed, boolean execute, String comment);
	public abstract Step createStep(Map<String, String> record) throws IOException;
	
	public abstract StepState createStepState();
	public abstract StepState createStepState(Step step);
	public abstract StepState createStepState(StepState stepState);
	
	protected abstract boolean validStepKindEx(String stepKind);
	
	public boolean validStepKind(String stepKind)
	{
		if (CoreStepKind.stepKindByLabel(stepKind) != CoreStepKind.InvalidStep)
			return true;
		
		return validStepKindEx(stepKind);
	}
}
