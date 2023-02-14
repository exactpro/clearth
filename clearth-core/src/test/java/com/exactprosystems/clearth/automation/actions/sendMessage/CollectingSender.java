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

package com.exactprosystems.clearth.automation.actions.sendMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.messages.PlainMessageSender;

public class CollectingSender implements PlainMessageSender
{
	private final List<Object> sentMessages = new ArrayList<>();
	
	@Override
	public EncodedClearThMessage sendMessage(Object message) throws IOException, ConnectivityException
	{
		sentMessages.add(message);
		return EncodedClearThMessage.newSentMessage(message);
	}
	
	@Override
	public EncodedClearThMessage sendMessage(EncodedClearThMessage message) throws IOException, ConnectivityException
	{
		sentMessages.add(message);
		return EncodedClearThMessage.newSentMessage(message.getPayload());
	}
	
	
	public List<Object> getSentMessages()
	{
		return sentMessages;
	}
}
