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

package com.exactprosystems.clearth.automation.actions.woodpecker;

import com.exactprosystems.clearth.automation.Action;
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.MatrixContext;
import com.exactprosystems.clearth.automation.StepContext;
import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.automation.report.Result;
import com.exactprosystems.clearth.automation.report.results.DefaultResult;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerSettingsException;

import static com.exactprosystems.clearth.automation.actions.woodpecker.WoodpeckerActionUtils.*;
import static com.exactprosystems.clearth.utils.ExceptionUtils.getDetailedMessage;
import static com.exactprosystems.clearth.woodpecker.notifications.Notification.Severity.ERROR;
import static com.exactprosystems.clearth.woodpecker.notifications.Notification.Severity.INFO;

public abstract class StartWoodpeckerDaemon extends Action
{
	protected static final String START_ERROR = "Error during start daemon from matrix";
	protected static final String START_SUCCESSFULLY = "Daemon started from matrix successfully";

	protected abstract void setDaemonSettings(WoodpeckerDaemon settings) throws WoodpeckerSettingsException;

	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		InputParamsHandler paramsHandler = new InputParamsHandler(inputParams);

		String daemonType = paramsHandler.getRequiredString(DAEMON_TYPE);
		String daemonName = paramsHandler.getString(DAEMON_NAME, DEFAULT_DAEMON_NAME);

		paramsHandler.check();

		WoodpeckerDaemon daemon = findDaemon(daemonType, daemonName);
		if (daemon.isRunning())
			return DefaultResult.passed("Daemon is already running: " + daemonName);
		
		try
		{
			setDaemonSettings(daemon);
		}
		catch (WoodpeckerSettingsException e)
		{
			return DefaultResult.failed("Error during setting daemon settings", e);
		}		
		
		try
		{
			String runId = daemon.start();
			addOutputParam(RUN_ID, runId);
			addWoodpeckerNotification(INFO, daemon.getSettings().getFullName(), START_SUCCESSFULLY);
		}
		catch (WoodpeckerException e)
		{
			addWoodpeckerNotification(ERROR, daemon.getSettings().getFullName(), getDetailedMessage(e));

			return DefaultResult.failed(START_ERROR, e);
		}

		return DefaultResult.passed(START_SUCCESSFULLY + ": " + daemonName);
	}
}
