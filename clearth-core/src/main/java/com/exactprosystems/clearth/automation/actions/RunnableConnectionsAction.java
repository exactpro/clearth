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

package com.exactprosystems.clearth.automation.actions;

import com.exactprosystems.clearth.ClearThCore;
import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultTableResultDetail;
import com.exactprosystems.clearth.automation.report.results.TableResult;
import com.exactprosystems.clearth.automation.report.results.TableResultDetail;
import com.exactprosystems.clearth.connectivity.connections.ClearThConnection;
import com.exactprosystems.clearth.connectivity.connections.ClearThRunnableConnection;
import com.exactprosystems.clearth.connectivity.connections.storage.ClearThConnectionStorage;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

public abstract class RunnableConnectionsAction extends Action
{
	public final String CONN_NAMES = "ConnectionNames";

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext)
			throws ResultException, FailoverException
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		List<String> connList = handler.getRequiredList(CONN_NAMES, ",");
		handler.check();

		return checkConnections(connList);
	}

	protected Result checkConnections(List<String> expectedConnections)
	{
		TableResult result = new TableResult("", Arrays.asList("Connection", "Details"), true);
		ClearThConnectionStorage cthConnectionStorage = ClearThCore.connectionStorage();

		for(String connName: expectedConnections)
		{
			if(StringUtils.isBlank(connName))
				continue;

			ClearThConnection clearThConnection = cthConnectionStorage.getConnection(connName);

			if (clearThConnection == null)
			{
				result.addDetail(new DefaultTableResultDetail(false, Arrays.asList(connName, "Does not exist")));
				continue;
			}
			
			if (!(clearThConnection instanceof ClearThRunnableConnection))
			{
				result.addDetail(new DefaultTableResultDetail(false, Arrays.asList(connName, "Not runnable")));
				continue;
			}
			result.addDetail(doWithConnection((ClearThRunnableConnection) clearThConnection));
		}
		return result;
	}

	protected abstract TableResultDetail doWithConnection(ClearThRunnableConnection msgCon);
}
