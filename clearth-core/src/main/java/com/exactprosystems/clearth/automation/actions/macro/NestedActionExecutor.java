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

package com.exactprosystems.clearth.automation.actions.macro;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.ActionExecutor;
import com.exactprosystems.clearth.automation.ActionParamsCalculator;
import com.exactprosystems.clearth.automation.FailoverStatus;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.StepContext;

public class NestedActionExecutor extends ActionExecutor
{
	private static final Logger logger = LoggerFactory.getLogger(NestedActionExecutor.class);
	
	public NestedActionExecutor(GlobalContext globalContext, ActionParamsCalculator calculator, String actionsReportsPath)
	{
		super(globalContext, calculator, new NestedActionReportWriter(actionsReportsPath), new FailoverStatus());
	}
	
	public void executeAction(Action action, StepContext stepContext, boolean writeReport)
	{
		this.prepareToAction(action);
		((NestedActionReportWriter)getReportWriter()).setWriteReport(writeReport);
		this.executeAction(action, stepContext, new AtomicBoolean(false));
	}
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}
}
