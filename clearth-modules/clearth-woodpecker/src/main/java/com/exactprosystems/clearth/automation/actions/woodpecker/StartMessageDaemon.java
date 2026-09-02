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

import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.daemons.CommonDaemon;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageDaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.settings.ExecutionMode;
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.LoadingSchedule;
import com.exactprosystems.clearth.woodpecker.configuration.settings.loading.PriorityMode;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerSettingsException;

import static com.exactprosystems.clearth.automation.actions.woodpecker.WoodpeckerActionUtils.*;

public class StartMessageDaemon extends StartWoodpeckerDaemon
{
	private static final String
			EXECUTION_MODE = "ExecutionMode",
			RATE = "Rate",
			SCHEDULE = "Schedule",
			PRIORITY_MODE = "PriorityMode",
			MAX_BATCH_SIZE = "MaxBatchSize",
			PRECISE_DELAY = "PreciseDelay";

	@Override
	protected void setDaemonSettings(WoodpeckerDaemon daemon) throws WoodpeckerSettingsException
	{
		InputParamsHandler paramsHandler = new InputParamsHandler(inputParams);

		String daemonType 			= paramsHandler.getRequiredString(DAEMON_TYPE);
		String daemonName 			= paramsHandler.getString(DAEMON_NAME, DEFAULT_DAEMON_NAME);
		ExecutionMode executionMode = paramsHandler.getEnum(EXECUTION_MODE, ExecutionMode.class, ExecutionMode.Simple);
		Integer maxBatchSize 		= paramsHandler.getInteger(MAX_BATCH_SIZE, 1);
		boolean preciseDelay    = paramsHandler.getBoolean(PRECISE_DELAY, false);

		paramsHandler.check();

		MessageDaemonSettings daemonSettings = new MessageDaemonSettings(daemonType, daemonName);
		daemonSettings.setExecutionMode(executionMode);
		daemonSettings.setMaxBatchSize(maxBatchSize);
		daemonSettings.setPreciseDelay(preciseDelay);

		if (executionMode == ExecutionMode.Simple)
		{
			Long rate = paramsHandler.getRequiredLong(RATE);
			paramsHandler.check();

			daemonSettings.setRate(rate);
		}
		else
		{
			String schedule = paramsHandler.getRequiredString(SCHEDULE);
			paramsHandler.check();

			daemonSettings.setSchedule(LoadingSchedule.parse(schedule));
			daemonSettings.setPriorityMode(paramsHandler.getEnum(PRIORITY_MODE, PriorityMode.class, PriorityMode.Time));
		}

		paramsHandler.check();

		daemonSettings.save();

		((CommonDaemon) daemon).setSettings(daemonSettings);
	}
}
