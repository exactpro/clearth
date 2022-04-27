/******************************************************************************
 * Copyright 2009-2022 Exactpro Systems Limited
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

import java.util.Set;

import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.actions.SendMessageAction;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.connectivity.validation.PlainCodec;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.messages.PlainMessageSender;

public class CustomSendMessageAction extends SendMessageAction<SimpleClearThMessage>
{
	private static PlainMessageSender sender;
	
	public static void setSender(PlainMessageSender sender)
	{
		CustomSendMessageAction.sender = sender;
	}
	
	
	@Override
	public MessageBuilder<SimpleClearThMessage> getMessageBuilder(Set<String> serviceParameters, Set<String> metaFields)
	{
		return new SimpleClearThMessageBuilder(serviceParameters, metaFields);
	}
	
	@Override
	protected String getDefaultCodecName()
	{
		return null;
	}
	
	@Override
	protected PlainMessageSender getCustomMessageSender(GlobalContext globalContext)
	{
		return sender;
	}
	
	@Override
	protected ICodec getCodec(GlobalContext globalContext)
	{
		return new PlainCodec();
	}
}
