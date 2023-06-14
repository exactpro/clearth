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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class TcpRhAcceptor implements AutoCloseable
{
	private static final int BACKLOG_SIZE = 128;
	
	private final EventLoopGroup bossGroup,
			workerGroup;
	private final Channel serverChannel;
	private final TcpRhChannelInitializer channelInitializer;
	
	public TcpRhAcceptor(int port, TcpRhChannelInitializer channelInitializer) throws Exception
	{
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		this.channelInitializer = channelInitializer;
		
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(channelInitializer).option(ChannelOption.SO_BACKLOG, BACKLOG_SIZE)
				.childOption(ChannelOption.SO_KEEPALIVE, true);
		
		try
		{
			serverChannel = b.bind(port).sync().channel();
		}
		catch (Exception e)
		{
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
			
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			throw e;
		}
	}
	
	@Override
	public void close() throws Exception
	{
		try
		{
			serverChannel.close().sync();
		}
		finally
		{
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	
	
	public Channel getClientChannel()
	{
		return channelInitializer.getClientChannel();
	}
	
	public TcpRhConnectionHandler getConnectionHandler()
	{
		return channelInitializer.getConnectionHandler();
	}
}
