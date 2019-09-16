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

package com.exactprosystems.clearth.automation.actions.xml;

import java.util.Set;

import com.exactprosystems.clearth.automation.actions.SendMessageAction;
import com.exactprosystems.clearth.connectivity.xml.ClearThXmlMessage;
import com.exactprosystems.clearth.connectivity.xml.ClearThXmlMessageBuilder;
import com.exactprosystems.clearth.connectivity.xml.XmlCodec;
import com.exactprosystems.clearth.messages.MessageBuilder;

public class SendXmlMessage extends SendMessageAction<ClearThXmlMessage>
{
	@Override
	public MessageBuilder<ClearThXmlMessage> getMessageBuilder(Set<String> serviceParameters)
	{
		return new ClearThXmlMessageBuilder(serviceParameters);
	}
	
	@Override
	protected String getDefaultCodecName()
	{
		return XmlCodec.DEFAULT_CODEC_NAME;
	}
}
