/******************************************************************************
 * Copyright 2009-2020 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.daemons.action;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.woodpecker.configuration.settings.DaemonSettings;
import com.exactprosystems.clearth.woodpecker.configuration.start.DaemonActionDesc;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.execution.DaemonContext;
import com.exactprosystems.clearth.woodpecker.expressions.WoodpeckerFunctions;
import com.exactprosystems.clearth.woodpecker.notifications.Notification;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;

import static com.exactprosystems.clearth.utils.ExceptionUtils.getDetailedMessage;
import static com.exactprosystems.clearth.woodpecker.Woodpecker.woodpecker;
import static java.lang.String.format;

public abstract class DaemonAction
{
	private static final Logger log = LoggerFactory.getLogger(DaemonAction.class);

	protected final DaemonActionDesc description;
	protected final String actionName;
	protected final String daemonName;
	protected final DaemonSettings daemonSettings;
	protected final WoodpeckerNotifications notifications;
	protected final ActionLoadingStatistics loadingStatistics;
	protected final DaemonContext daemonContext;

	private final AtomicInteger seqErrorsCounter = new AtomicInteger();
	private volatile boolean invalidated;

	
	protected abstract void run(WoodpeckerFunctions functions) throws WoodpeckerException;

	public void dispose() { /* Nothing to dispose by default */ }


	public DaemonAction(DaemonActionDesc description, DaemonSettings daemonSettings, 
	                    ActionLoadingStatistics loadingStatistics, DaemonContext daemonContext)
	{
		this.description = description;
		actionName = description.getActionName();
		daemonName = daemonSettings.getFullName();
		this.daemonSettings = daemonSettings;
		notifications = woodpecker().getNotifications();
		this.loadingStatistics = loadingStatistics;
		this.daemonContext = daemonContext;
	}

	
	public boolean isInvalidated()
	{
		return invalidated;
	}
	

	public void execute(WoodpeckerFunctions functions)
	{
		try
		{
			run(functions);
			
			loadingStatistics.incActions(actionName);
		}
		catch (WoodpeckerException e)
		{
			handleError(e);
		}
	}


	@SuppressWarnings("unused")
	protected void handleError(Exception error)
	{
		log.warn("[{}][{}]", daemonName, actionName, error);
		
		notifications.addNotification(createWarning(getDetailedMessage(error)));
		
		updateErrorsCounter();
	}
	
	@SuppressWarnings("unused")
	protected void handleError(Exception error, String messageTemplate, Object... templateArgs)
	{
		String errorMessage = format(messageTemplate, templateArgs);
		String exceptionDetails = getDetailedMessage(error);

		log.warn("[{}][{}]: {}", daemonName, actionName, errorMessage, error);
		
		notifications.addNotification(createWarning(format("%s %s", errorMessage, exceptionDetails)));
		
		updateErrorsCounter();
	}
	
	@SuppressWarnings("unused")
	protected void handleError(String messageTemplate, Object... templateArgs)
	{
		String errorMessage = format(messageTemplate, templateArgs);
		
		log.warn("[{}][{}]: {}", daemonName, actionName, errorMessage);
		
		notifications.addNotification(createWarning(errorMessage));
		
		updateErrorsCounter();
	}
	
	private Notification createWarning(String errorMessage)
	{
		return new Notification(Notification.Severity.WARN, 
				format("Error in Daemon '%s' Action '%s'.", daemonName, actionName), errorMessage);
	}
	
	private void updateErrorsCounter()
	{
		if (seqErrorsCounter.incrementAndGet() > description.getMaxSeqErrorsCount())
			invalidate();
	}
	
	private void invalidate()
	{
		invalidated = true;
		
		notifications.addWarning(daemonName, log, 
				format("Too many errors in the Action '%s'. It has been stopped.", actionName));
		
		dispose();
	}
}
