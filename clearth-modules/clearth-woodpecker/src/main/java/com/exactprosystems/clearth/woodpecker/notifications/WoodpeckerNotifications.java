/******************************************************************************
 * Copyright 2009-2025 Exactpro Systems Limited
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

package com.exactprosystems.clearth.woodpecker.notifications;

import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.exactprosystems.clearth.utils.ExceptionUtils.getDetailedMessage;
import static com.exactprosystems.clearth.woodpecker.notifications.Notification.Severity.WARN;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class WoodpeckerNotifications
{
	private final List<NotificationsSubscriber> subscribers = new CopyOnWriteArrayList<>();
	
	
	public void subscribe(NotificationsSubscriber subscriber)
	{
		subscribers.add(subscriber);
	}
	
	public void unSubscribe(NotificationsSubscriber subscriber)
	{
		subscribers.remove(subscriber);
	}
	
	
	public void addNotification(Notification notification)
	{
		notify(notification);
	}
	
	public void addWarning(String daemonName, Logger logger, String text)
	{
		logger.warn("{}: {}", daemonName, text);
		createNotification(daemonName, WARN, text);
	}
	
	public void addWarning(String daemonName, Logger logger, Exception e, String comment)
	{
		logger.warn("{}: {}", new Object[]{daemonName, (comment != null) ? comment : "error occurred", e});		
		String exceptionDetails = getDetailedMessage(e);
		String message = isNotEmpty(comment) ? comment + ": " + exceptionDetails : exceptionDetails;
		createNotification(daemonName, WARN, message);
	}
	
	public void addWarning(String daemonName, Logger logger, Exception e)
	{
		addWarning(daemonName, logger, e, null);
	}
	
	
	private void createNotification(String daemonName,
	                                Notification.Severity severity,
	                                String text)
	{
		Notification notification = new Notification(severity, daemonName, text);
		notify(notification);
	}
	
	private void notify(Notification notification)
	{
		for (NotificationsSubscriber subscriber : subscribers)
		{
			subscriber.notify(notification);
		}
	}
}
