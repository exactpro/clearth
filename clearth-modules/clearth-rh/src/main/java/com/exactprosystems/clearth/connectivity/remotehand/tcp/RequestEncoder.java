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

package com.exactprosystems.clearth.connectivity.remotehand.tcp;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;

public class RequestEncoder extends MessageToByteEncoder<TcpRhRequest>
{
	private static final int PAYLOAD_SIZE_LENGTH = 4;
	
	@Override
	protected void encode(ChannelHandlerContext ctx, TcpRhRequest msg, ByteBuf out) throws Exception
	{
		int payloadSize = 0;
		List<byte[]> dataBytes;
		
		Object[] data = msg.getData();
		if (data != null && data.length > 0)
		{
			dataBytes = new ArrayList<>(data.length);
			for (Object d : data)
			{
				byte[] b = (d instanceof byte[]) ? (byte[])d : d.toString().getBytes(CharsetUtil.UTF_8);
				dataBytes.add(b);
				payloadSize += PAYLOAD_SIZE_LENGTH + b.length;
			}
		}
		else
			dataBytes = null;
		
		out.writeInt(msg.getCode());
		if (dataBytes != null)
		{
			out.writeInt(payloadSize);
			for (byte[] b : dataBytes)
			{
				out.writeInt(b.length);
				out.writeBytes(b);
			}
		}
	}
}
