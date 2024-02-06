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

package com.exactprosystems.clearth.data.th2.events;

import java.time.Instant;

import com.exactpro.cradle.BookId;
import com.exactpro.cradle.testevents.StoredTestEventId;
import com.exactpro.th2.common.event.Event.Status;
import com.exactpro.th2.common.grpc.Event;
import com.exactpro.th2.common.grpc.EventBatch;
import com.exactpro.th2.common.grpc.EventID;
import com.exactprosystems.clearth.automation.Action;
import com.google.protobuf.Timestamp;

public class EventUtils
{
	private EventUtils()
	{
	}
	
	public static EventBatch wrap(Event event)
	{
		return EventBatch.newBuilder().addEvents(event).build();
	}
	
	public static Status getStatus(boolean status)
	{
		return status ? Status.PASSED : Status.FAILED;
	}
	
	public static Instant getTimestamp(Timestamp timestamp)
	{
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}
	
	public static Instant getActionStartTimestamp(Action action)
	{
		return action.getStarted() != null ? Instant.ofEpochMilli(action.getStarted().getTime()) : Instant.now();
	}
	
	public static Instant getActionEndTimestamp(Action action)
	{
		return action.getFinished() != null ? Instant.ofEpochMilli(action.getFinished().getTime()) : null;
	}
	
	public static String idToString(EventID id)
	{
		StoredTestEventId storedId = new StoredTestEventId(new BookId(id.getBookName()), 
				id.getScope(), 
				getTimestamp(id.getStartTimestamp()),
				id.getId());
		return storedId.toString();
	}
}
