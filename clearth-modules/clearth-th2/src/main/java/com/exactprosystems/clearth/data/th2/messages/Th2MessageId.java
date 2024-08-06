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

package com.exactprosystems.clearth.data.th2.messages;

import com.exactpro.th2.common.schema.message.impl.rabbitmq.transport.MessageId;
import com.exactprosystems.clearth.data.HandledMessageId;
import com.exactprosystems.clearth.data.th2.serialization.MessageIDSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class Th2MessageId implements HandledMessageId
{
	@JsonSerialize(using = MessageIDSerializer.class)  //This is used when saving message and metadata (including id) with MessageFileWriter 
	private final MessageId id;
	
	public Th2MessageId(MessageId id)
	{
		this.id = id;
	}
	
	@Override
	public String toString()
	{
		return id.toString();
	}
	
	public MessageId getId()
	{
		return id;
	}
}
