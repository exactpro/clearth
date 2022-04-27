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

package com.exactprosystems.clearth.connectivity.validation;

import com.exactprosystems.clearth.connectivity.DecodeException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.connectivity.listeners.ClearThMessageCollector;

public class PlainCodec implements ICodec
{
	@Override
	public String encode(ClearThMessage<?> message) throws EncodeException
	{
		return message.toString();
	}
	
	@Override
	public ClearThMessage<?> decode(String message) throws DecodeException
	{
		ClearThMessage<?> result = new SimpleClearThMessage();
		result.addField(ClearThMessageCollector.MESSAGE, message);
		return result;
	}
	
	@Override
	public ClearThMessage<?> decode(String message, String type) throws DecodeException
	{
		ClearThMessage<?> result = decode(message);
		result.addField(ClearThMessage.MSGTYPE, type);
		return result;
	}

}
