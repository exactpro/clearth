/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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
import com.exactprosystems.clearth.automation.GlobalContext;
import com.exactprosystems.clearth.automation.actions.SendMessageAction;
import com.exactprosystems.clearth.automation.exceptions.FailoverException;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessageBuilder;
import com.exactprosystems.clearth.connectivity.validation.PlainCodec;
import com.exactprosystems.clearth.messages.MessageBuilder;
import com.exactprosystems.clearth.messages.PlainMessageSender;

import java.util.Set;

public class SimpleSendMessageActionTest extends SendMessageAction<SimpleClearThMessage>
{
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
	protected PlainMessageSender getPlainSender(GlobalContext globalContext) throws FailoverException
	{
		return new CollectingSender();
	}
	
	@Override
	protected ICodec getCodec(GlobalContext globalContext)
	{
		return new PlainCodec();
	}
}
