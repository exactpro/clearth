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

import com.exactprosystems.clearth.automation.report.results.DefaultTableResultDetail;
import com.exactprosystems.clearth.automation.report.results.TableResultDetail;
import com.exactprosystems.clearth.connectivity.connections.ClearThRunnableConnection;

import java.util.Arrays;

public class StopConnections extends RunnableConnectionsAction
{
	@Override
	protected TableResultDetail doWithConnection(ClearThRunnableConnection msgCon)
	{
		String connName = msgCon.getName();

		if (!msgCon.isRunning())
			return new DefaultTableResultDetail(true, Arrays.asList(connName, "Already stopped"));

		try
		{
			msgCon.stop();
			return new DefaultTableResultDetail(true, Arrays.asList(connName, "Stopped"));
		}
		catch (Exception e)
		{
			logger.error("Could not stop connection '" + connName + "'", e);
			return new DefaultTableResultDetail(false, Arrays.asList(connName,
					"Could not stop. Error: " + e.getMessage()));
		}
	}
}
