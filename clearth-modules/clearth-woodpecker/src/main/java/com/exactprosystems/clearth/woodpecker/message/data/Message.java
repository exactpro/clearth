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

package com.exactprosystems.clearth.woodpecker.message.data;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Map;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;

public class Message
{
	private final String messageText;
	private final String messageType;
	private final ClearThMessage clearThMessage;
	private Map<String, MutableInt> rgsCountByType;
	
	public Message(String messageText, String messageType, ClearThMessage clearThMessage)
	{
		this.messageText = messageText;
		this.messageType = messageType;
		this.clearThMessage = clearThMessage;
	}

	public String getMessageText()
	{
		return messageText;
	}

	public String getMessageType()
	{
		return messageType;
	}

	public ClearThMessage getClearThMessage()
	{
		return clearThMessage;
	}

	public int getRgsCount(String type)
	{
		MutableInt counter = (rgsCountByType != null) ? rgsCountByType.get(type) : null;
		if (counter != null)
			return counter.intValue();
		return 0;
	}

	public void setRgsCountByType(Map<String, MutableInt> rgsCountByType)
	{
		this.rgsCountByType = rgsCountByType;
	}
}
