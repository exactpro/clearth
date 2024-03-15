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

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.exceptions.AutomationException;
import com.exactprosystems.clearth.automation.report.ReportsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultSimpleExecutor extends SimpleExecutor
{
	private static final Logger logger = LoggerFactory.getLogger(DefaultSimpleExecutor.class);
	
	public DefaultSimpleExecutor(Scheduler scheduler, List<Step> steps, List<Matrix> matrices, GlobalContext globalContext,
			FailoverStatus failoverStatus, Map<String, Preparable> preparableActions)
	{
		super(scheduler, steps, matrices, globalContext, failoverStatus, preparableActions);
	}
	
	@Override
	protected Logger getLogger()
	{
		return logger;
	}
	
	
	@Override
	protected boolean createConnections() throws Exception
	{
		return true;
	}
	
	@Override
	protected boolean loadMappingsAndSettings() throws IOException, AutomationException
	{
		return true;
	}
	
	@Override
	protected StepImpl createStepImpl(String stepKind, String stepParameter)
	{
		return null;
	}
	
	@Override
	protected void closeConnections()
	{
	}


	@Override
	protected void prepareToTryAgainMain()
	{
	}
	
	@Override
	protected void prepareToTryAgainAlt()
	{
	}

	@Override
	protected ReportsWriter initReportsWriter(String pathToStoreReports, String pathToActionsReports)
	{
		return new ReportsWriter(this, pathToStoreReports, pathToActionsReports);
	}

	@Override
    public List<String> getMatrixSteps(String matrixName)
	{
		File actionsReports = new File(ClearThCore.appRootRelative(actionsReportsDir));
		return getStepsByMatricesMap(actionsReports).getOrDefault(matrixName, Collections.emptyList());
	}
}
