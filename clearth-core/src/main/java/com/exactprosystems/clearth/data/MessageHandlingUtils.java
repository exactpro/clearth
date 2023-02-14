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

package com.exactprosystems.clearth.data;

import com.exactprosystems.clearth.connectivity.iface.ClearThMessageMetadata;

public class MessageHandlingUtils
{
	public static final String HANDLED_MESSAGE_ID = "HandledMessageId";
	
	public static HandledMessageId getMessageId(ClearThMessageMetadata metadata)
	{
		return metadata == null ? null : (HandledMessageId) metadata.getField(HANDLED_MESSAGE_ID);
	}
	
	public static void setMessageId(ClearThMessageMetadata metadata, HandledMessageId id)
	{
		metadata.addField(HANDLED_MESSAGE_ID, id);
	}
}
