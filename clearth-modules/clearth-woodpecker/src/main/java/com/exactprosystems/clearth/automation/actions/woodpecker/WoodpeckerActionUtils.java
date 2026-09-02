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

import com.exactprosystems.clearth.automation.exceptions.ResultException;
import com.exactprosystems.clearth.woodpecker.daemons.DaemonsPool;
import com.exactprosystems.clearth.woodpecker.daemons.WoodpeckerDaemon;
import com.exactprosystems.clearth.woodpecker.notifications.Notification;

import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;

public abstract class WoodpeckerActionUtils
{
	public static final String DAEMON_TYPE = "DaemonType";
	public static final String DAEMON_NAME = "DaemonName";
	public static final String RUN_ID = "RunID";

	public static final String DEFAULT_DAEMON_NAME = "Default";

	public static final String DAEMON_TYPE_NOT_FOUND = "Daemon type \"%s\" is not found";
	public static final String DAEMON_NOT_FOUND = "Daemon named \"%s\" is not found";

	
	public static void addWoodpeckerNotification(Notification.Severity severity, String daemonFullName, String details)
	{
		woodpecker().getNotifications().addNotification(new Notification(severity, daemonFullName, details));
	}

	public static WoodpeckerDaemon findDaemon(String daemonType, String daemonName) throws ResultException
	{
		DaemonsPool daemonsPool = woodpecker().findDaemonPool(daemonType);

		if (daemonsPool == null)
			throw ResultException.failed(String.format(DAEMON_TYPE_NOT_FOUND, daemonType));

		WoodpeckerDaemon daemon = daemonsPool.getDaemon(daemonName);

		if (daemon == null)
			throw ResultException.failed(String.format(DAEMON_NOT_FOUND, daemonName));

		return daemon;
	}
}
