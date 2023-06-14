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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactprosystems.clearth.connectivity.remotehand.RhClient;
import com.exactprosystems.clearth.connectivity.remotehand.RhResponse;
import com.exactprosystems.clearth.utils.Stopwatch;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class TcpRhConnectionHandler extends ChannelInboundHandlerAdapter
{
	private static final Logger logger = LoggerFactory.getLogger(TcpRhConnectionHandler.class);
	
	private volatile ChannelHandlerContext channelContext;
	private volatile RhResponse lastResponse;
	private final long waitTimeout;
	
	public TcpRhConnectionHandler(long waitTimeout)
	{
		this.waitTimeout = waitTimeout;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		if (!(msg instanceof RhResponse))
		{
			logger.error("Unexpected object received as RemoteHand response: {}", msg);
			return;
		}
		
		RhResponse rsp = (RhResponse)msg;
		if (rsp.getCode() == 0)
			logger.info("Greetings message received: {}", rsp.getDataString());
		else
			lastResponse = rsp;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		channelContext = ctx;
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		logger.error("Closing connection due to error occurred on wire", cause);
		ctx.close();
	}

	
	public ChannelHandlerContext getChannelContext()
	{
		return channelContext;
	}
	
	public boolean isActive()
	{
		return channelContext != null && channelContext.channel().isOpen();
	}

	public RhResponse getLastResponse()
	{
		return lastResponse;
	}
	
	
	public RhResponse waitForResponse()
	{
		Stopwatch sw = Stopwatch.createAndStart(getWaitTimeout());
		try
		{
			while (lastResponse == null && !sw.isExpired())
			{
				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
					logger.error("Wait for response interrupted", e);
					break;
				}
			}
			
			if (lastResponse == null)
				return new RhResponse(RhClient.CODE_ERROR, "No response received from RemoteHand");
			
			RhResponse result = lastResponse;
			return result;
		}
		finally
		{
			lastResponse = null;
		}
	}
	
	
	protected long getWaitTimeout()
	{
		return waitTimeout;
	}
}
