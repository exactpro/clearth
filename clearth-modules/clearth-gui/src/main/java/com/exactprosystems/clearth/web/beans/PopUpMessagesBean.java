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

package com.exactprosystems.clearth.web.beans;

import com.exactprosystems.clearth.web.misc.PopUpMessageDesc;

import javax.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;

public class PopUpMessagesBean extends ClearThBean
{
	private static final int POP_UP_MESSAGES_LIMIT = 100;
	
	private PopUpMessageDesc selectedMessage = null;
	private LinkedList<PopUpMessageDesc> messages = null;

	@PostConstruct
	public void init()
	{
		messages = new LinkedList<PopUpMessageDesc>();
	}

	public List<PopUpMessageDesc> getMessages()
	{
		return messages;
	}
	
	public void addMessage(PopUpMessageDesc message)
	{
		synchronized (messages)
		{
			messages.addFirst(message);
			checkLimit();
		}
	}

	private void checkLimit()
	{
		while (messages.size() > POP_UP_MESSAGES_LIMIT)
			messages.removeLast();
	}

	public void removeSelectedMessage()
	{
		if (selectedMessage != null)
		{
			synchronized (messages)
			{
				messages.remove(selectedMessage);
			}
		}
	}
	
	public void clear()
	{
		synchronized (messages)
		{
			messages.clear();
		}
	}

	public PopUpMessageDesc getSelectedMessage()
	{
		return selectedMessage;
	}

	public void setSelectedMessage(PopUpMessageDesc selectedMessage)
	{
		this.selectedMessage = selectedMessage;
	}
}
