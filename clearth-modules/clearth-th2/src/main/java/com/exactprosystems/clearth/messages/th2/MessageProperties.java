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

import java.time.Instant;
import java.util.Map;

import com.exactpro.th2.common.grpc.ConnectionID;
import com.exactpro.th2.common.grpc.Direction;
import com.exactpro.th2.common.grpc.MessageID;
import com.exactprosystems.clearth.automation.actions.th2.Th2ActionUtils;
import com.exactprosystems.clearth.connectivity.iface.ClearThMessage;
import com.exactprosystems.clearth.utils.inputparams.InputParamsHandler;
import com.exactprosystems.clearth.utils.inputparams.ParametersHandler;
import com.google.protobuf.Timestamp;

public class MessageProperties
{
	private String book,
			sessionAlias,
			sessionGroup,
			msgType;
	private Direction direction;
	private Instant timestamp;
	
	public MessageProperties()
	{
	}
	
	public static MessageProperties fromInputParams(Map<String, String> inputParams, String defaultBook)
	{
		InputParamsHandler handler = new InputParamsHandler(inputParams);
		MessageProperties result = fromParametersHandler(handler, defaultBook);
		handler.check();
		return result;
	}
	
	public static MessageProperties fromParametersHandler(ParametersHandler handler, String defaultBook)
	{
		MessageProperties result = new MessageProperties();
		result.book = handler.getString(Th2ActionUtils.PARAM_BOOK, defaultBook);
		result.sessionAlias = handler.getRequiredString(Th2ActionUtils.PARAM_SESSION_ALIAS);
		result.sessionGroup = handler.getString(Th2ActionUtils.PARAM_SESSION_GROUP, result.sessionAlias);
		result.msgType = handler.getRequiredString(ClearThMessage.MSGTYPE);
		result.direction = handler.getEnum(Th2ActionUtils.PARAM_DIRECTION, Direction.class, Direction.FIRST);
		return result;
	}
	
	
	@Override
	public String toString()
	{
		return "[book=" + book 
				+ ", sessionAlias=" + sessionAlias 
				+ ", sessionGroup=" + sessionGroup
				+ ", msgType=" + msgType 
				+ ", direction=" + direction 
				+ ", timestamp=" + timestamp + "]";
	}
	
	
	public MessageID toMessageId()
	{
		ConnectionID conId = ConnectionID.newBuilder()
				.setSessionAlias(getSessionAlias())
				.setSessionGroup(getSessionGroup())
				.build();
		
		Instant ts = timestamp != null ? timestamp : Instant.now();
		return MessageID.newBuilder()
				.setBookName(getBook())
				.setConnectionId(conId)
				.setTimestamp(Timestamp.newBuilder()
						.setSeconds(ts.getEpochSecond())
						.setNanos(ts.getNano())
						.build())
				.setDirection(getDirection())
				.setSequence(0)  //Must be zero for Message being sent for encoding and forwarding to th2-conn
				.build();
	}
	
	
	public String getBook()
	{
		return book;
	}
	
	public void setBook(String book)
	{
		this.book = book;
	}
	
	
	public String getSessionAlias()
	{
		return sessionAlias;
	}
	
	public void setSessionAlias(String sessionAlias)
	{
		this.sessionAlias = sessionAlias;
	}
	
	
	public String getSessionGroup()
	{
		return sessionGroup;
	}
	
	public void setSessionGroup(String sessionGroup)
	{
		this.sessionGroup = sessionGroup;
	}
	
	
	public String getMsgType()
	{
		return msgType;
	}
	
	public void setMsgType(String msgType)
	{
		this.msgType = msgType;
	}
	
	
	public Direction getDirection()
	{
		return direction;
	}
	
	public void setDirection(Direction direction)
	{
		this.direction = direction;
	}
	
	
	public Instant getTimestamp()
	{
		return timestamp;
	}
	
	public void setTimestamp(Instant timestamp)
	{
		this.timestamp = timestamp;
	}
}
