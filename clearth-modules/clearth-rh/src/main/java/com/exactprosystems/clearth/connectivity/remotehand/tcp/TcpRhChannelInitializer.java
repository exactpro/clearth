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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class TcpRhChannelInitializer extends ChannelInitializer<SocketChannel>
{
	private final Object connectionMonitor = new Object();
	private final long waitTimeout;
	private Channel clientChannel = null;
	private TcpRhConnectionHandler connectionHandler = null;
	
	public TcpRhChannelInitializer(long waitTimeout)
	{
		this.waitTimeout = waitTimeout;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception
	{
		synchronized (connectionMonitor)
		{
			if (clientChannel != null && clientChannel.isActive())
			{
				ch.close().sync();  //Refuse to accept connections from more than one RemoteHand
				return;
			}
			
			connectionHandler = new TcpRhConnectionHandler(waitTimeout);
			ch.pipeline().addLast(new ResponseDecoder(),
					new RequestEncoder(),
					connectionHandler);
			clientChannel = ch;
		}
	}
	
	
	public Channel getClientChannel()
	{
		return clientChannel;
	}
	
	public TcpRhConnectionHandler getConnectionHandler()
	{
		return connectionHandler;
	}
}
