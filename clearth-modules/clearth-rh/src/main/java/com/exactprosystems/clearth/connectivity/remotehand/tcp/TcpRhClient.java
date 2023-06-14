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

import java.io.File;
import java.io.IOException;

import com.exactprosystems.clearth.connectivity.remotehand.RhScriptProcessor;
import com.exactprosystems.clearth.connectivity.remotehand.RhClient;
import com.exactprosystems.clearth.connectivity.remotehand.RhResponse;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

/**
 * Implementation of RhClient to send requests via TCP/IP
 * Sent request have the following structure:
 *   type - 4 bytes
 *   totalPayloadSize - 4 bytes
 *   one or more times:
 *     size of payload string in bytes - 4 bytes
 *     bytes of payload string
 * Depending on request type, payload strings can be absent. In this case totalPayloadSize is not sent as well.
 * All requests, except for Logon and Download, must contain sessionID as first payload string.
 * 
 * Responses from RemoteHand are always of the following structure:
 *   code - 4 bytes
 *   size of string in bytes - 4 bytes
 *   bytes of string
 */
public class TcpRhClient extends RhClient
{
	private static final Logger logger = LoggerFactory.getLogger(TcpRhClient.class);
	
	private final Channel clientChannel;
	private final TcpRhConnectionHandler connectionHandler;
	
	public TcpRhClient(RhScriptProcessor processor, Channel clientChannel, TcpRhConnectionHandler connectionHandler)
	{
		super(processor);
		this.clientChannel = clientChannel;
		this.connectionHandler = connectionHandler;
	}
	
	public TcpRhClient(RhScriptProcessor processor, TcpRhAcceptor acceptor)
	{
		this(processor, acceptor.getClientChannel(), acceptor.getConnectionHandler());
	}
	
	public TcpRhClient(Channel clientChannel, TcpRhConnectionHandler connectionHandler)
	{
		this.clientChannel = clientChannel;
		this.connectionHandler = connectionHandler;
	}
	
	public TcpRhClient(TcpRhAcceptor acceptor)
	{
		this(acceptor.getClientChannel(), acceptor.getConnectionHandler());
	}
	
	
	@Override
	protected RhResponse sendLogon() throws IOException
	{
		return sendRequestAndWaitResponse(new TcpRhRequest(RequestType.LOGON));
	}
	
	@Override
	protected RhResponse sendScript(String script) throws IOException
	{
		return sendRequestAndWaitResponse(new TcpRhRequest(RequestType.SCRIPT, getSessionId(), script));
	}
	
	@Override
	protected RhResponse queryStatus() throws IOException
	{
		return sendRequestAndWaitResponse(new TcpRhRequest(RequestType.STATUS, getSessionId()));
	}
	
	@Override
	protected RhResponse downloadFile(String type, String id) throws IOException
	{
		return sendRequestAndWaitResponse(new TcpRhRequest(RequestType.DOWNLOAD, type, id));
	}
	
	@Override
	protected RhResponse sendLogout() throws IOException
	{
		return sendRequestAndWaitResponse(new TcpRhRequest(RequestType.LOGOUT, getSessionId()));
	}
	
	@Override
	protected void disposeResources() throws Exception
	{
		//clientChannel must be kept open else RemoteHand will be disconnected from ClearTH and new TcpRhClient could not be created
		//This is not a leak, because connection is managed by TcpRhAcceptor, not by TcpRhClient
	}
	
	@Override
	public RhResponse sendFile(File f, String path) throws IOException
	{
		return sendRequestAndWaitResponse(new TcpRhRequest(RequestType.FILE, 
				getSessionId(), path, FileUtils.readFileToByteArray(f)));
	}
	
	protected RhResponse sendRequestAndWaitResponse(TcpRhRequest request)
	{
		try
		{
			clientChannel.writeAndFlush(request).sync();
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			logger.error("Wait for data sending interrupted", e);
		}
		
		return connectionHandler.waitForResponse();
	}
}
