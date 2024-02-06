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

package com.exactprosystems.clearth.data;

import java.util.Collection;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.StepMetadata;
import com.exactprosystems.clearth.automation.report.Result;

public class DefaultTestExecutionHandler implements TestExecutionHandler
{
	@Override
	public void close() throws Exception
	{
	}
	
	
	@Override
	public HandledTestExecutionIdStorage onTestStart(Collection<String> matrices, GlobalContext globalContext) throws TestExecutionHandlingException
	{
		return null;
	}
	
	@Override
	public void onTestEnd() throws TestExecutionHandlingException
	{
	}
	
	
	@Override
	public void onGlobalStepStart(StepMetadata stepData) throws TestExecutionHandlingException
	{
	}
	
	@Override
	public void onGlobalStepEnd() throws TestExecutionHandlingException
	{
	}
	
	
	@Override
	public HandledTestExecutionId onAction(Action action) throws TestExecutionHandlingException
	{
		return new UuidTestExecutionId();
	}
	
	@Override
	public void onActionResult(Result result, Action action) throws TestExecutionHandlingException
	{
	}
	
	
	@Override
	public boolean isActive()
	{
		return false;
	}
	
	@Override
	public String getName()
	{
		return null;
	}
}
