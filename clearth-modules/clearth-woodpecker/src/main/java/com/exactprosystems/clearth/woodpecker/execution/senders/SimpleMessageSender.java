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

package com.exactprosystems.clearth.woodpecker.execution.senders;

import java.util.Collections;

import com.exactprosystems.clearth.automation.actions.MessageAction;
import com.exactprosystems.clearth.connectivity.connections.ClearThMessageConnection;
import com.exactprosystems.clearth.woodpecker.configuration.start.MessageSenderDesc;
import com.exactprosystems.clearth.woodpecker.daemons.message.MessageLoadingStatistics;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerException;
import com.exactprosystems.clearth.woodpecker.message.data.Message;
import com.exactprosystems.clearth.woodpecker.notifications.WoodpeckerNotifications;

public class SimpleMessageSender extends MessageSender
{
	private final ClearThMessageConnection con;
	
	public SimpleMessageSender(String id, String daemonName, WoodpeckerNotifications notifications,
			MessageLoadingStatistics loadingStatistics, MessageSenderDesc description, ClearThMessageConnection con)
	{
		super(id, daemonName, notifications, loadingStatistics, description, 
				Collections.singletonMap(MessageAction.CONNECTIONNAME, con.getName()));
		this.con = con;
	}
	
	@Override
	protected void sendMessage(Message message, SendingCallback sendingCallback) throws WoodpeckerException
	{
		doSendMessage(message, con);
		invokeCallback(sendingCallback, message);
	}
	
	
	protected void doSendMessage(Message message, ClearThMessageConnection con) throws WoodpeckerDaemonException
	{
		try
		{
			con.sendMessage(message.getMessageText());
		}
		catch (Exception e)
		{
			throw new WoodpeckerDaemonException(e, "[%s] Could not send message", getId());
		}
	}
	
	protected void invokeCallback(SendingCallback callback, Message message)
	{
		callback.onMessageSent();
		callback.onTaskCompleted();
	}
}
