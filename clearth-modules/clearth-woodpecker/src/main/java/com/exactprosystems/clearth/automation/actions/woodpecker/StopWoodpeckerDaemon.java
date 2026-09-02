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

import static com.exactprosystems.clearth.automation.actions.woodpecker.WoodpeckerActionUtils.*;
import static com.exactprosystems.clearth.utils.ExceptionUtils.getDetailedMessage;
import static com.exactprosystems.clearth.woodpecker.notifications.Notification.Severity.ERROR;

public class StopWoodpeckerDaemon extends Action
{
	@Override
	protected Result run(StepContext stepContext, MatrixContext matrixContext, GlobalContext globalContext) throws ResultException
	{
		InputParamsHandler paramsHandler = new InputParamsHandler(inputParams);

		String daemonType = paramsHandler.getRequiredString(DAEMON_TYPE);
		String daemonName = paramsHandler.getString(DAEMON_NAME, DEFAULT_DAEMON_NAME);

		paramsHandler.check();

		WoodpeckerDaemon daemon = findDaemon(daemonType, daemonName);
		if (!daemon.isRunning())
			return DefaultResult.passed(null, "Daemon is already stopped: " + daemonName);

		try
		{
			daemon.stop();
		}
		catch (WoodpeckerException e)
		{
			addWoodpeckerNotification(ERROR, daemon.getSettings().getFullName(), getDetailedMessage(e));

			return DefaultResult.failed("Error during stop daemon from matrix", e);
		}

		return DefaultResult.passed(null, "Daemon stopped from matrix successfully: " + daemonName);

	}
}
