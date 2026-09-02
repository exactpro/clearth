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

package com.exactprosystems.clearth.woodpecker.message.data;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * 15 August 2018
 */
public class LinkedMessages implements Iterable<Message>
{
	private final List<Message> messages = new ArrayList<>();


	public List<Message> getMessages()
	{
		return messages;
	}
	
	public int size()
	{
		return messages.size();
	}
	
	public Message getMessage(int index)
	{
		return messages.get(index);
	}

	public void addMessage(Message message)
	{
		messages.add(message);
	}

	@Override
	public Iterator<Message> iterator()
	{
		return messages.iterator();
	}
}
