/******************************************************************************
 * Copyright 2009-2024 Exactpro Systems Limited
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

package com.exactprosystems.clearth.messages.th2;

import com.exactpro.th2.common.grpc.EventID;
import com.exactpro.th2.common.grpc.Message;
import com.exactprosystems.clearth.connectivity.iface.SimpleClearThMessage;
import com.exactprosystems.clearth.messages.converters.ConversionException;

public abstract class GrpcRequestFactory<RQ>
{
	private final GrpcMessageFactory messageFactory;
	
	public GrpcRequestFactory()
	{
		messageFactory = createMessageFactory();
	}
	
	public GrpcRequestFactory(String flatDelimiter)
	{
		messageFactory = createMessageFactory(flatDelimiter);
	}
	
	
	protected abstract RQ messageToRequest(Message msg, EventID parentEvent) throws ConversionException;
	
	
	public RQ createRequest(SimpleClearThMessage message, MessageProperties properties, EventID parentEvent) throws ConversionException
	{
		try
		{
			Message msg = messageFactory.createMessage(message, properties);
			return messageToRequest(msg, parentEvent);
		}
		catch (ConversionException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new ConversionException("Error while creating request", e);
		}
	}
	
	
	protected GrpcMessageFactory createMessageFactory()
	{
		return new GrpcMessageFactory();
	}
	
	protected GrpcMessageFactory createMessageFactory(String flatDelimiter)
	{
		return new GrpcMessageFactory(flatDelimiter);
	}
}
