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

package com.exactprosystems.clearth.utils;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;

import static com.exactprosystems.clearth.connectivity.iface.ClearThMessage.MSGTYPE;

import java.util.Map;

public class SimpleKeyValueCodec implements ICodec
{
	private static final String PAIR_DELIMITER = "\n";
	
	@Override
	public String encode(ClearThMessage<?> message)
	{
		return message.getEncodedMessage();
	}
	
	@Override
	public ClearThMessage<?> decode(String message) throws DecodeException
	{
		return decode(message, null);
	}
	
	@Override
	public ClearThMessage<?> decode(String encodedMessage, String msgType) throws DecodeException
	{
		Map<String, String> fields = getMessageFields(encodedMessage);
		
		if (msgType == null)
		{
			msgType = fields.get(MSGTYPE);
			if (msgType == null)
				throw new DecodeException("Message doesn't contain type");
			else
				fields.remove(MSGTYPE);
		}
		
		SimpleClearThMessage cthMessage = new SimpleClearThMessage();
		cthMessage.addField(MSGTYPE, msgType);
		fields.forEach(cthMessage::addField);
		cthMessage.setEncodedMessage(encodedMessage);
		
		return cthMessage;
	}
	
	private Map<String, String> getMessageFields(String message)
	{
		return KeyValueUtils.parseKeyValueString(message, PAIR_DELIMITER, false);
	}
}
