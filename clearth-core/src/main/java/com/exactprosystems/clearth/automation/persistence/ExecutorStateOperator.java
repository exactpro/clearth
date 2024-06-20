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

package com.exactprosystems.clearth.automation.persistence;

import java.io.IOException;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.Step;
import com.exactprosystems.clearth.utils.Pair;

public interface ExecutorStateOperator<C extends ExecutorStateContext> extends AutoCloseable
{
	C save(ExecutorStateInfo stateInfo, ExecutorStateObjects stateObjects) throws IOException;
	Pair<ExecutorStateInfo, C> loadStateInfo() throws IOException;
	ExecutorStateObjects loadStateObjects(C context) throws IOException;
	void update(ExecutorStateInfo stateInfo, C context, Action lastExecutedAction, ActionState actionState) throws IOException;
	void update(ExecutorStateInfo stateInfo, C context, Step lastFinishedStep, StepState stepState) throws IOException;
	void updateSteps(ExecutorStateInfo stateInfo, C context) throws IOException;
	void updateStateInfo(ExecutorStateInfo stateInfo, C context) throws IOException;
}
