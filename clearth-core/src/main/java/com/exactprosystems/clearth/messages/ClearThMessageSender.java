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

package com.exactprosystems.clearth.messages;

import java.io.IOException;

import com.exactprosystems.clearth.connectivity.ConnectivityException;
import com.exactprosystems.clearth.connectivity.EncodeException;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;
import com.exactprosystems.clearth.connectivity.iface.EncodedClearThMessage;
import com.exactprosystems.clearth.connectivity.iface.ICodec;

/**
 * Message sender that encodes {@link ClearThMessage} and writes it to given {@link PlainMessageSender}
 * @author vladimir.panarin
 */
public class ClearThMessageSender<M extends ClearThMessage<M>> implements MessageSender<M>
{
	protected final ICodec codec;
	protected final PlainMessageSender sender;
	
	public ClearThMessageSender(ICodec codec, PlainMessageSender sender)
	{
		this.codec = codec;
		this.sender = sender;
	}
	
	@Override
	public EncodedClearThMessage sendMessage(M message) throws IOException, ConnectivityException, EncodeException
	{
		Object encoded = codec.encode(message);
		ClearThMessageMetadata metadata = message.getMetadata();
		return metadata != null ? sender.sendMessage(new EncodedClearThMessage(encoded, metadata)) : sender.sendMessage(encoded);
	}
}
