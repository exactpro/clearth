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

package com.exactprosystems.clearth.woodpecker.misc.encoder;

import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;
import com.exactprosystems.clearth.woodpecker.exceptions.WoodpeckerDaemonException;

import java.util.Map;

import static java.lang.String.format;

public class MessageEncoder
{
	private final Map<String, ICodec> codecs;
	
	public MessageEncoder(Map<String, ICodec> codecs)
	{
		this.codecs = codecs;
	}
	
	public String encode(ClearThMessage message, String codecName) throws WoodpeckerDaemonException
	{
		ICodec codec = codecs.get(codecName);
		if (codec == null)
			throw new WoodpeckerDaemonException(format("Codec '%s' isn't supported or invalid.", codecName));

		String encodedMessage;
		try
		{
			encodedMessage = codec.encode(message);
		}
		catch (EncodeException e)
		{
			throw new WoodpeckerDaemonException(format("Cannot encode message: %s", message.toString()), e);
		}
		
		return encodedMessage;
	}
}
