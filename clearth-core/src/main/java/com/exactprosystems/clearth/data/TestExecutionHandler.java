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

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.StepMetadata;
import com.exactprosystems.clearth.automation.report.Result;

/**
 * Interface for objects that handle various events of a test execution, e.g. store information about them in a database
 */
public interface TestExecutionHandler extends AutoCloseable
{
	void onTestStart(Collection<String> matrices, GlobalContext globalContext) throws TestExecutionHandlingException;
	void onTestEnd() throws TestExecutionHandlingException;
	void onGlobalStepStart(StepMetadata stepData) throws TestExecutionHandlingException;
	void onGlobalStepEnd() throws TestExecutionHandlingException;
	void onAction(Action action) throws TestExecutionHandlingException;
	void storeIntermediateResult(Result result, Action action) throws TestExecutionHandlingException;
	
	boolean isActive();
}
